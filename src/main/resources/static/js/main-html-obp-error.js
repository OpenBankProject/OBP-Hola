// Render an OBP-API error into the page instead of letting jQuery swallow it.
//
// Every consent-governed call can legitimately be refused -- a consent that does not declare a
// permission gets 403 OBP-20017, one that is not AUTHORISED gets 401 OBP-35005, and so on. Without
// a .fail() handler jQuery discards the response body and the user sees an empty box, which is
// indistinguishable from "the account genuinely has no data".
//
// Shared by the UK v3.1 and v4.0.1 flows; the controllers pass OBP's status and body through
// verbatim (see OtherController.passThroughObpError) so the OBP error code survives to here.
function renderObpError(target, xhr) {
    let detail = xhr.responseText || 'no response body';
    try {
        const parsed = JSON.parse(xhr.responseText);
        if (parsed && parsed.message) {
            detail = parsed.message;
        }
    } catch (e) {
        // Not JSON — fall back to the raw body.
    }
    target.append(
        `<div class="alert alert-danger" data-testid="obp-error">HTTP ${xhr.status}: ${detail}</div>`);
}
