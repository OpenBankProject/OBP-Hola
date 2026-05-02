# FAPI Compliance TODO

The current OBP-OIDC integration is a pragmatic OIDC integration with consent support. The following items are needed for FAPI 1.0 Advanced compliance:

1. **Request Objects** — Authorization parameters must be in a signed JWT (`request` parameter), not plain query parameters. Hola does support this for Hydra (via `buildRequestObject()`), but OBP-OIDC doesn't.

2. **PKCE with S256** — Hola does use PKCE, so this part is covered.

3. **MTLS or DPoP** — Sender-constrained access tokens, not just bearer tokens.

4. **JARM** — Signed authorization responses.

5. **PAR (Pushed Authorization Requests)** — The authorization request should be pushed server-to-server first.

Passing `consent_id` as a plain query parameter is the biggest gap — in FAPI, that would need to be inside the signed request object to prevent tampering.
