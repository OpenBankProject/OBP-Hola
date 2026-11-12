package com.openbankproject.hydra.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The contract between Hola and OBP-API.
 *
 * obp-api-contract/contract.json lists every OBP-API call Hola makes, with the response fields the code
 * reads (in Java, or in the page scripts a body is passed to). obp-api-contract/snapshot.json is what an
 * OBP-API documents for those calls, written by obp-api-contract/snapshot.py from its resource docs.
 * These tests need no server: they check that the list matches the code, and that every call reaches an
 * endpoint OBP-API documents, with every field the code reads. Retake the snapshot from a new OBP-API
 * build to learn whether that build breaks Hola.
 *
 * The UK Open Banking and Berlin Group calls (standard_calls) have no OBP resource docs, so for them only
 * their place in the source is checked; the @Value URLs that nothing uses are listed in
 * declared_but_unused so that they are not taken for calls.
 */
class ObpApiContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CONTRACT_DIR = Paths.get("obp-api-contract");
    private static final Path MAIN_JAVA = Paths.get("src/main/java");

    /** Text that marks a source line as building the URL of an OBP-API call. */
    private static final List<String> OBP_PATH_MARKERS = Arrays.asList(
            "/obp/v", "/obp/${", "/berlin-group/", "/open-banking/", "${endpoint.path.prefix");

    private static JsonNode load(String name) throws IOException {
        return JSON.readTree(CONTRACT_DIR.resolve(name).toFile());
    }

    private static List<JsonNode> list(JsonNode node) {
        List<JsonNode> out = new ArrayList<>();
        if (node != null) node.forEach(out::add);
        return out;
    }

    private static List<JsonNode> calls() throws IOException {
        return list(load("contract.json").get("calls"));
    }

    /** Every entry with a source and needles: calls, standard_calls and declared_but_unused. */
    private static List<JsonNode> entriesInSource() throws IOException {
        JsonNode contract = load("contract.json");
        List<JsonNode> out = new ArrayList<>();
        out.addAll(list(contract.get("calls")));
        out.addAll(list(contract.get("standard_calls")));
        out.addAll(list(contract.get("declared_but_unused")));
        return out;
    }

    private static List<String> strings(JsonNode node) {
        return list(node).stream().map(JsonNode::asText).collect(Collectors.toList());
    }

    private static String key(JsonNode call) {
        return call.get("verb").asText() + " " + call.get("path").asText();
    }

    private static String name(JsonNode entry) {
        return entry.has("field") ? entry.get("field").asText() : key(entry);
    }

    private static String read(String file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
    }

    /** The snapshot entry for a call, or an empty node. */
    private static JsonNode documented(JsonNode call, JsonNode snapshot) {
        for (JsonNode endpoint : snapshot.get("endpoints")) {
            if (key(endpoint).equals(key(call))) return endpoint;
        }
        return JSON.createObjectNode();
    }

    /** The reads of call that fields does not document. A read written a|b is satisfied by either field. */
    static List<String> undocumentedReads(JsonNode call, Set<String> fields) {
        return strings(call.get("reads")).stream()
                .filter(read -> Arrays.stream(read.split("\\|")).noneMatch(fields::contains))
                .collect(Collectors.toList());
    }

    /** Whether a source line builds an OBP-API URL: code, not a comment, with a string holding a marker. */
    static boolean buildsObpPath(String line) {
        String code = line.trim();
        if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return false;
        return code.contains("\"") && OBP_PATH_MARKERS.stream().anyMatch(code::contains);
    }

    /** Lines of main Java source that build an OBP-API URL: {file relative to the repo, line number, line}. */
    private static List<String[]> obpPathLines() throws IOException {
        List<String[]> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java")).sorted()::iterator) {
                String rel = file.toString().replace('\\', '/');
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (buildsObpPath(lines.get(i))) {
                        out.add(new String[]{rel, String.valueOf(i + 1), lines.get(i).trim()});
                    }
                }
            }
        }
        return out;
    }

    /** Tests that each listed call still exists: every needle of an entry occurs in the entry's source file. */
    @Test
    @DisplayName("every call in the contract is in its source file")
    void everyCallInTheContractIsInItsSourceFile() throws IOException {
        List<String> gone = new ArrayList<>();
        for (JsonNode entry : entriesInSource()) {
            String text = read(entry.get("source").asText());
            for (String needle : strings(entry.get("needles"))) {
                if (!text.contains(needle)) gone.add(name(entry) + " - " + needle);
            }
        }
        assertThat(gone).isEmpty();
    }

    /**
     * Tests coverage: a line that builds an OBP-API URL must contain a needle of an entry listed for that
     * file, so a new call cannot reach OBP-API without its fields being recorded.
     */
    @Test
    @DisplayName("every OBP-API call in the source is in the contract")
    void everyObpApiCallInTheSourceIsInTheContract() throws IOException {
        List<JsonNode> entries = entriesInSource();
        List<String[]> lines = obpPathLines();
        // The scan does find the URLs, so the check below is not vacuous.
        assertThat(lines).as("lines that build an OBP-API URL").isNotEmpty();
        List<String> unlisted = lines.stream()
                .filter(l -> entries.stream().noneMatch(e -> e.get("source").asText().equals(l[0])
                        && strings(e.get("needles")).stream().anyMatch(n -> l[2].contains(n.trim()))))
                .map(l -> l[0] + ":" + l[1] + " " + l[2])
                .collect(Collectors.toList());
        assertThat(unlisted).isEmpty();
    }

    /**
     * Tests that the source line detection used above takes code that builds an OBP-API URL and leaves
     * comments and the force_jws path prefixes (which are matched against URLs, not called) alone.
     */
    @Test
    @DisplayName("the coverage scan finds OBP-API URLs and skips comments")
    void theCoverageScanFindsObpApiUrls() {
        assertThat(buildsObpPath("    @Value(\"${obp.base_url}/obp/v5.1.0/banks\")")).isTrue();
        assertThat(buildsObpPath("String url = obpBaseUrl + \"/obp/v7.0.0/root\";")).isTrue();
        assertThat(buildsObpPath("@Value(\"${endpoint.path.prefix.v401}/accounts\")")).isTrue();
        assertThat(buildsObpPath("@Value(\"${obp.base_url}/berlin-group/v1.3/consents\")")).isTrue();
        assertThat(buildsObpPath("// see \"/obp/v5.1.0/banks\"")).isFalse();
        assertThat(buildsObpPath("pathOfStandard.put(\"OBPv5.1.0\", \"obp/v5.1.0\");")).isFalse();
    }

    /**
     * Tests that each URL listed as declared_but_unused really is unused: its field name occurs once in its
     * file, at the declaration. A field that comes into use must move to calls, with what it reads.
     */
    @Test
    @DisplayName("URLs listed as declared but unused are not used")
    void declaredButUnusedUrlsAreNotUsed() throws IOException {
        List<String> used = new ArrayList<>();
        for (JsonNode entry : list(load("contract.json").get("declared_but_unused"))) {
            String field = entry.get("field").asText();
            Matcher m = Pattern.compile("\\b" + Pattern.quote(field) + "\\b").matcher(read(entry.get("source").asText()));
            int count = 0;
            while (m.find()) count++;
            if (count != 1) used.add(field + " occurs " + count + " times in " + entry.get("source").asText());
        }
        assertThat(used).isEmpty();
    }

    /**
     * Tests the split between the lists: calls go to OBP-API's own /obp paths, which have resource docs;
     * standard_calls are UK Open Banking or Berlin Group paths and record why they are not checked against
     * the snapshot.
     */
    @Test
    @DisplayName("calls are OBP paths, standard calls are UK or Berlin Group paths")
    void callsAndStandardCallsAreSplitByPath() throws IOException {
        List<String> misplaced = new ArrayList<>();
        for (JsonNode call : calls()) {
            if (!call.get("path").asText().startsWith("/obp/v")) misplaced.add("calls: " + key(call));
        }
        for (JsonNode call : list(load("contract.json").get("standard_calls"))) {
            String path = call.get("path").asText();
            boolean standard = path.startsWith("/open-banking/") || path.startsWith("/berlin-group/");
            if (!standard || !call.hasNonNull("undocumented_endpoint")) misplaced.add("standard_calls: " + key(call));
        }
        assertThat(misplaced).isEmpty();
    }

    /** Tests that snapshot.json holds exactly the calls of contract.json; otherwise rerun snapshot.py. */
    @Test
    @DisplayName("the snapshot was taken for this contract")
    void theSnapshotWasTakenForThisContract() throws IOException {
        Set<String> listed = calls().stream().map(ObpApiContractTest::key).collect(Collectors.toCollection(TreeSet::new));
        Set<String> snapped = list(load("snapshot.json").get("endpoints")).stream()
                .map(ObpApiContractTest::key).collect(Collectors.toCollection(TreeSet::new));
        assertThat(snapped).isEqualTo(listed);
    }

    /**
     * Tests that each call's version and path name a documented endpoint, unless the entry records why not
     * (undocumented_endpoint). A recorded gap that OBP-API has since documented fails too, so the note goes.
     */
    @Test
    @DisplayName("every call reaches an endpoint OBP-API documents")
    void everyCallReachesAnEndpointObpApiDocuments() throws IOException {
        JsonNode snapshot = load("snapshot.json");
        List<String> problems = new ArrayList<>();
        for (JsonNode call : calls()) {
            JsonNode entry = documented(call, snapshot);
            boolean missing = entry.path("missing").asBoolean(false);
            boolean knownGap = call.hasNonNull("undocumented_endpoint");
            if (missing && !knownGap) problems.add(key(call) + ": no documented endpoint");
            if (!missing && knownGap) {
                problems.add(key(call) + ": now documented as " + entry.path("operation_id").asText()
                        + "; drop undocumented_endpoint");
            }
            if (missing && knownGap) System.err.println("known gap - " + key(call) + ": " + call.get("undocumented_endpoint").asText());
            if (entry.has("ambiguous")) problems.add(key(call) + ": matches several endpoints " + entry.get("ambiguous"));
        }
        assertThat(problems).isEmpty();
    }

    /**
     * Tests that each field the code reads is in the documented response of its endpoint, unless the entry
     * lists it in undocumented_fields; a listed field that OBP-API now documents fails, so the list shrinks.
     */
    @Test
    @DisplayName("every field the code reads is documented")
    void everyFieldTheCodeReadsIsDocumented() throws IOException {
        JsonNode snapshot = load("snapshot.json");
        List<String> problems = new ArrayList<>();
        for (JsonNode call : calls()) {
            JsonNode entry = documented(call, snapshot);
            if (!entry.has("fields")) continue;
            Set<String> fields = new TreeSet<>(strings(entry.get("fields")));
            Set<String> known = new TreeSet<>(strings(call.get("undocumented_fields")));
            for (String read : undocumentedReads(call, fields)) {
                if (known.contains(read)) {
                    System.err.println("known gap - " + key(call) + ": " + read + " is not documented");
                } else {
                    problems.add(key(call) + " (" + entry.path("operation_id").asText() + "): reads " + read + ", not documented");
                }
            }
            for (String field : known) {
                if (fields.contains(field)) problems.add(key(call) + ": " + field + " is now documented; drop it from undocumented_fields");
            }
        }
        assertThat(problems).isEmpty();
    }

    /**
     * Tests the check itself: with consent_request_id gone from the documented fields, the read is reported,
     * and a read with an alternative (a|b) passes on either field.
     */
    @Test
    @DisplayName("a renamed or removed field is reported")
    void aRenamedOrRemovedFieldIsReported() throws IOException {
        JsonNode consentRequest = JSON.readTree("{\"reads\": [\"consent_request_id\"]}");
        Set<String> renamed = new TreeSet<>(Arrays.asList("consent_id", "payload"));
        assertThat(undocumentedReads(consentRequest, renamed)).containsExactly("consent_request_id");

        JsonNode either = JSON.readTree("{\"reads\": [\"consent_request_id|id\"]}");
        assertThat(undocumentedReads(either, new TreeSet<>(Collections.singletonList("id")))).isEmpty();
        assertThat(undocumentedReads(either, new TreeSet<>())).containsExactly("consent_request_id|id");
    }
}
