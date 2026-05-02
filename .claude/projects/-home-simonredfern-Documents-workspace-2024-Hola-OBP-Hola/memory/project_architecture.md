---
name: OBP component role separation
description: Architectural decision on how Hola, Portal, OBP-API, and OBP-OIDC divide responsibilities for consent flows
type: project
---

Authentication should happen at OBP Portal. OBP-OIDC should remain as standard OIDC as possible — no OBP-specific consent logic.

**Component roles:**
- **Hola** — represents the TPP (Fintech application)
- **OBP Portal** (or certain pages in it) — represents the bank's interface
- **OBP-API** — carries all consent management backend functions (persistence, business logic)
- **OBP-OIDC** — standard OIDC provider only

**Why:** Clean separation of concerns. The bank's consent UI belongs in Portal, not in the OIDC provider. Consent persistence and business logic belong in OBP-API, not scattered across components.

**How to apply:** When designing consent flows, Portal handles GUI portions of consent and calls OBP-API for any persistence. OBP-OIDC should not take on OBP-specific consent logic. This favors Option A from WIP.md — user authenticates at Portal, Portal creates the consent via OBP-API.
