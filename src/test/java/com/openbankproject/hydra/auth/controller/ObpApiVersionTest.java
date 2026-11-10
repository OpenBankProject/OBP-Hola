package com.openbankproject.hydra.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every OBP-API URL Hola calls uses API version v5.1.0 or newer, so a deployment can
 * disable the older versions without breaking Hola.
 */
class ObpApiVersionTest {

    private static final Pattern OBP_VERSION = Pattern.compile("/obp/v(\\d+)\\.(\\d+)\\.(\\d+)/");
    private static final int[] MINIMUM = {5, 1, 0};

    private static List<String> declaredObpUrls(Class<?> controller) {
        List<String> urls = new ArrayList<>();
        for (Field field : controller.getDeclaredFields()) {
            Value value = field.getAnnotation(Value.class);
            if (value != null && value.value().contains("/obp/v")) {
                urls.add(field.getName() + " = " + value.value());
            }
        }
        return urls;
    }

    private static boolean olderThanMinimum(String url) {
        Matcher m = OBP_VERSION.matcher(url);
        while (m.find()) {
            for (int i = 0; i < 3; i++) {
                int part = Integer.parseInt(m.group(i + 1));
                if (part != MINIMUM[i]) {
                    if (part < MINIMUM[i]) return true;
                    break;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("controllers declare OBP-API URLs on v5.1.0 or newer")
    void controllersUseV510OrNewer() {
        List<String> urls = new ArrayList<>();
        urls.addAll(declaredObpUrls(IndexController.class));
        urls.addAll(declaredObpUrls(OtherController.class));

        assertThat(urls).as("OBP-API URLs found").isNotEmpty();
        assertThat(urls).filteredOn(ObpApiVersionTest::olderThanMinimum).isEmpty();
    }

    /** obp/vX.Y.Z anywhere in a source line, also built by concatenation or ending at the version. */
    private static final Pattern OBP_VERSION_IN_SOURCE = Pattern.compile("obp/v(\\d+)\\.(\\d+)\\.(\\d+)(?![\\d.])");

    @Test
    @DisplayName("no main source file builds an OBP-API path below v5.1.0")
    void sourcesUseV510OrNewer() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Paths.get("src/main"))) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(f -> f.toString().matches(".*\\.(java|properties|yml|yaml|html|js|docker)$"))::iterator) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher m = OBP_VERSION_IN_SOURCE.matcher(lines.get(i));
                    while (m.find()) {
                        if (olderThanMinimum("/obp/v" + m.group(1) + "." + m.group(2) + "." + m.group(3) + "/")) {
                            found.add(file + ":" + (i + 1) + " " + m.group());
                        }
                    }
                }
            }
        }
        assertThat(found).isEmpty();
    }
}
