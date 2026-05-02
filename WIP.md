# WIP: OBP Consent Flow — Architectural Decision Needed

## Context

Wiring up OBP-OIDC + OBP-Portal + Hola for OBP consent flows. OBP-OIDC should stay focused on standards-based OIDC — consent challenge management belongs in OBP-API, not OBP-OIDC.

## What OBP-API already provides

- **`GET /obp/v5.0.0/consumer/consent-requests/{ID}`** — Fetch consent request details. **No user auth required.**
- **`POST /obp/v5.0.0/consumer/consent-requests/{ID}/IMPLICIT/consents`** — Create consent from a consent request. **Requires user authentication.**
- **`POST /obp/v3.1.0/banks/{BANK_ID}/consents/{CONSENT_ID}/challenge`** — Answer consent challenge (SCA). **Requires user auth.**

## The core problem

After login at OBP-OIDC, someone needs to call the IMPLICIT consent creation endpoint on OBP-API, and that endpoint **requires user authentication**. But the user just logged in at OBP-OIDC, not at Portal — so Portal doesn't have a user access token.

## Three options for who creates the consent

**Option A: Portal creates it** — But Portal has no user session for this user. It would need a token. OBP-OIDC could issue a short-lived token and pass it to Portal alongside the redirect. This adds token-issuing logic to OBP-OIDC.

**Option B: OBP-OIDC creates it** — After Portal redirects back with "approved", OBP-OIDC calls OBP-API to create the consent. But this puts OBP-specific consent logic in OBP-OIDC.

**Option C: Hola creates it** — OBP-OIDC returns with `consent_request_id` but no `consent_id`. Hola uses the access_token from the token exchange to call OBP-API's IMPLICIT consent endpoint itself. This keeps OBP-OIDC and Portal thin, but requires changes to Hola (which the original plan said "no changes needed").

## OBP-OIDC state between redirects

Instead of a `ConsentChallengeService` (in-memory store), OBP-OIDC could use its existing `JwtService` to create a short-lived signed JWT containing the OAuth params — no in-memory store, no new service, just a JWT round-trip. Standards-based and keeps OBP-OIDC stateless.

## Already done

- Added `ConsentChallenge` case class to `OBP-OIDC/src/main/scala/com/tesobe/oidc/models/OidcModels.scala` (may need to revert or adjust depending on direction chosen)

## Decision: Option A — Portal authenticates the user and creates the consent

**Rationale:** Each component should stick to its role:
- **Hola** = TPP (Fintech app) — initiates the flow, receives the consent
- **OBP Portal** = the bank's interface — handles consent GUI, user authentication
- **OBP-API** = consent management backend — all persistence and business logic
- **OBP-OIDC** = standard OIDC provider — should not contain OBP-specific consent logic

Portal handles the GUI portions of consent and calls OBP-API for any persistence. This keeps OBP-OIDC as standard as possible.

## Next steps

- Implement the Portal-based consent flow (Option A)
- Ensure OBP-OIDC redirects to Portal for consent approval
- Portal authenticates the user, shows consent UI, and calls OBP-API to create the consent
- OBP-OIDC receives confirmation and completes the OIDC flow back to Hola
