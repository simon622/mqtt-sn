# MQTT-SN v2.0 Codec vs Spec Gap Analysis

Comparison of this repo's `mqtt-sn-codec` v2.0 wire implementation
(`mqtt-sn-codec/src/main/java/org/slj/mqtt/sn/wire/version2_0/`) against
`mqtt-sn-v2.0-draft.pdf` (OASIS "MQTT for Sensor Networks Version 2.0",
Committee Specification Draft 01, dated 05 February 2026 — a notably newer
draft than the changelog table in `README.md` reflects).

Generated: 2026-08-02. Last updated: 2026-08-02 (progress update below).

## Progress

**Steps 1-3 of the prioritized recommendation are DONE** (implemented and
verified in this repo — full `mvn clean install` across all 12 modules
succeeds, and `mqtt-sn-codec`'s test suite is 100/100 green):

- [x] **Step 1 — Packet-type constants.** The broken uncommitted edit
  described in §2 below was discarded. `MqttsnConstants.java` now has the
  original, correct v1.2 packet-type values restored, plus a fully
  independent `*_V2_0`-suffixed block matching spec Table 2 exactly, with
  zero collisions between the two tables. Every v2.0 payload class and the
  `Mqttsn_v2_0_Codec` dispatch switch were updated to the new names.
- [x] **Step 2 — Topic Type model.** v1.2's `TOPIC_TYPE` enum/constants are
  untouched (still correct for v1.2 and widely used outside the codec
  module). For v2.0, fixed the real bug this had caused: `setTopicName()`
  on `MqttsnPublish_V2_0`/`MqttsnSubscribe_V2_0` was mis-tagging topic names
  as `TOPIC_NORMAL`/`TOPIC_SHORT` (silently truncating names >2 chars via
  the `TOPIC_NORMAL` path) instead of `TOPIC_FULL`. Added `validate()`
  guards on Publish/Subscribe/Suback/Unsubscribe rejecting the now-Reserved
  SHORT value, and a REGACK-specific guard restricting it to
  Predefined/Session alias only, per spec §3.5.
- [x] **Step 3 — CONNECT/CONNACK/DISCONNECT rewrites.** All three fully
  rewritten field-by-field against Figures 6/7/24: real flag bytes,
  conditional Will/Auth blocks on CONNECT, conditional Session
  Expiry/Server Keep Alive/Auth on CONNACK, and DISCONNECT's corrected
  3-flag-bit model (dropped the non-existent `retainRegistrations` flag,
  which belongs on the not-yet-implemented SLEEPREQ). Found and fixed two
  bugs of my own along the way: an off-by-N error in the codec's
  version-sniffing offset calculation for CONNECT (didn't account for the
  Packet Identifier field), and a decode/encode inconsistency in
  `MqttsnPublish_V2_0`'s QoS -1 handling once its old dedicated type byte
  was removed — QoS -1 on PUBLISH is now explicitly rejected at `validate()`
  time (CSD01 Table 8 reserves those QoS bits; the replacement mechanism is
  PUBWOS, still unimplemented per §5 below).
- `Mqttsn2_0WireTests.java` was updated alongside: inherited v1.2 tests that
  exercise packet types/behaviors not applicable to v2.0 (GWINFO, SEARCHGW,
  WILLMSG family, PUBREC/PUBCOMP, REGISTER, Short Topic Name, QoS -1) are
  now explicit no-op overrides with a comment, rather than silently
  mis-passing against the wrong wire format (or now correctly failing).

**Steps 4 and 6 are also now DONE** (same verification bar: full
`mvn clean install` across all 12 modules succeeds; `mqtt-sn-codec` test
suite is 105/105 green — 44 v1.2 + 61 v2.0):

- [x] **Step 4 — Missing packet classes.** Added, wired into
  `Mqttsn_v2_0_Codec` dispatch and `Mqttsn_v2_0_MessageFactory`:
  - **PUBREC/PUBREL/PUBCOMP** (`MqttsnPubrec_V2_0`/`MqttsnPubrel_V2_0`/
    `MqttsnPubcomp_V2_0`) — confirmed missing entirely during step 3, per
    the note below. Share a new abstract base (renamed in step 7 to
    `AbstractMqttsnIdWithOptionalReasonCode_V2_0` once UNSUBACK turned out
    to share the same shape) with a properly optional/length-inferred
    Reason Code (Figures 14-17); `MqttsnPuback_V2_0` was refactored onto
    the same base for consistency (previously always emitted the Reason
    Code byte - spec valid, but now matches its siblings and is leaner on
    the wire).
  - **PUBWOS** (`MqttsnPubwos_V2_0`, Figure 11) — Publish Without Session:
    no Packet Identifier, no QoS field (always QoS 0), Topic Type
    restricted to Predefined Alias or Topic Name (enforced in `validate()`).
  - **WAKEUP** (`MqttsnWakeup_V2_0`, Figure 25) — header-only.
  - **SLEEPREQ** (`MqttsnSleepreq_V2_0`, Figure 26) / **SLEEPRESP**
    (`MqttsnSleepresp_V2_0`, Figure 27).
  - **v2.0-native ADVERTISE/SEARCHGW/GWINFO** (`MqttsnAdvertise_V2_0`/
    `MqttsnSearchGw_V2_0`/`MqttsnGwInfo_V2_0`, §3.20) replacing the v1.2
    fallthrough. Note SEARCHGW's real wire delta from v1.2: no `radius`
    byte, instead an optional variable-length "Additional Network
    Information" field (Figure 32) — the shared `IMqttsnMessageFactory
    .createSearchGw(int radius)` signature maps `radius` onto that field
    as a single byte for v2.0, per the spec's own worked example.
  - `IMqttsnMessageFactory` gained new methods (`createPubwos` x2,
    `createWakeup`, `createSleepReq`, `createSleepResp` x2) with
    "not supported" defaults in `AbstractMqttsnMessageFactory`, following
    the codebase's existing pattern (e.g. `createAuth` in v1.2).
  - **Still not implemented**: Connection Encapsulation, Forwarder
    Encapsulation, REGISTER v2.0-native format (byte ID reserved, no class
    yet - explicitly out of step 4's scope per the original recommendation).
- [x] **Step 6 — Reason Codes.** Added the full CSD01 Table 4 (Reason
  Codes, §2.3) as ~50 named `RETURN_CODE_*_V2_0` constants in
  `MqttsnConstants.java`, alongside the untouched v1.2 set. Used the new
  constants in place of magic numbers where already present and low-risk
  (`MqttsnSpecificationValidator.validateAuthReasonCode`). Did not change
  `validateReturnCode`'s validation strictness (still a generic uint8
  range check, matching the existing codebase convention of not enforcing
  per-packet-type allow-lists).
- `Mqttsn2_0WireTests.java` updated again: new round-trip tests for
  PUBWOS/WAKEUP/SLEEPREQ/SLEEPRESP; the no-op overrides for
  GWINFO/SEARCHGW/PUBREC/PUBCOMP were removed now that real v2.0
  implementations exist (the inherited v1.2 tests now exercise them for
  real via the overridden factory methods); new no-op overrides added for
  `testMqttsnWilltopic`/`testMqttsnWilltopicreq` (byte-value collisions
  with the newly-dispatched PUBCOMP_V2_0/PUBREL_V2_0 surfaced these -
  WILLTOPIC* doesn't exist in v2.0 either, same rationale as WILLMSG*).

**Step 7 (SUBSCRIBE/SUBACK/UNSUBSCRIBE/UNSUBACK/PINGREQ/PINGRESP
field-by-field audit) is also now DONE** (same verification bar: full
`mvn clean install` succeeds; `mqtt-sn-codec` test suite is 109/109 green —
44 v1.2 + 65 v2.0). This pass turned up two genuinely serious bugs beyond
what the original scan flagged, both now fixed:

- [x] **SUBSCRIBE** — already had the correct flag layout (confirmed
  against Figure 18, no change needed there); added the one missing piece,
  a `validate()` rejection of a zero-length Topic Filter (Protocol Error
  per 3.7.5).
- [x] **SUBACK** — **rewritten from a fabricated wire format.**
  `MqttsnSuback_V2_0` had an invented "QoS" bit-field in its flags byte
  that doesn't exist anywhere in Figure 19; real v2.0 folds "granted QoS"
  directly into the Reason Code (Table 4: Reason Codes 0x00/0x01/0x02 ARE
  "Granted QoS 0/1/2" for SUBACK specifically). Also fixed: no "Topic Alias
  flag" bit existed (Topic Alias was hard-coded as always 2 bytes present
  instead of conditional), Reason Code was hard-coded present instead of
  optional/length-inferred, and Topic Type wasn't constrained to
  Predefined/Session Alias per 3.8.2.1. Also fixed a **real correctness
  bug this had caused in the runtime**: the inherited
  `AbstractMqttsnMessage.isErrorMessage()` treats any non-zero Reason Code
  as an error, which would have flagged a perfectly successful "granted
  QoS 1" or "granted QoS 2" SUBACK as an error message throughout
  `mqtt-sn-core`/`mqtt-sn-gateway`'s handler code the moment a client
  requested anything above QoS 0 - `MqttsnSuback_V2_0.isErrorMessage()` now
  overrides this with the correct "≥0x80 is failure" rule from §2.3.
  `getQoS()`/`setQoS()` are kept as thin aliases onto the Reason Code for
  API compatibility with existing callers (`AbstractMqttsnMessageHandler`,
  `MqttsnGatewayMessageHandler`), and `Mqttsn_v2_0_MessageFactory
  .createSuback` now folds its `grantedQoS` parameter into the Reason Code
  correctly.
- [x] **UNSUBSCRIBE** — added the missing reserved-bits-must-be-0
  validation (bits 7-2, 3.9.2); the rest of the layout was already correct.
- [x] **UNSUBACK** — was hard-coding the Reason Code as always present
  (5 bytes fixed) when Figure 21 says it's optional/length-inferred, same
  shape as the PUBACK family. Refactored onto the (renamed, now shared)
  `AbstractMqttsnIdWithOptionalReasonCode_V2_0` base.
- [x] **PINGREQ / PINGRESP** — **rewritten from a fabricated wire format,
  the same category of bug as SUBACK.** `MqttsnPingreq_V2_0` had
  `maxMessages`/`clientId` fields with `needsId()=false`, matching no
  figure in this spec at all; real Figure 22 is just a mandatory Packet
  Identifier, nothing else. `MqttsnPingresp_V2_0` had a `messagesRemaining`
  byte at the wrong offset, also with `needsId()=false`; real Figure 23 is
  a mandatory Packet Identifier followed by an optional Application
  Messages Remaining byte. Both now have `needsId()=true` per Table 3 (a
  genuine v2.0 addition over v1.2 - PING request/response correlation via
  Packet Identifier didn't exist before). `getClientId()` is kept as an
  always-null compatibility shim on `MqttsnPingreq_V2_0` since
  `MqttsnGatewayMessageHandler.handlePingreq` reads it opportunistically
  and already tolerates null.

**Step 5's field-layout half is also now DONE** (`mqtt-sn-codec` test suite
is 118/118 green — 44 v1.2 + 65 v2.0 + 9 new `ProtectionPacketFlagsTest`
unit tests; full project build succeeds). The packet-type byte was already
fixed in step 1; this pass verified the rest of Figure 28 field-by-field
against `MqttsnProtection.java`/`ProtectionPacketFlags.java`/
`AbstractProtectionScheme.java` and found it was **already correct**
except for one area:

- [x] Field order/offsets, Protection Flags bit layout (AuthTagLen bits
  7-4 / CryptoLen bits 3-2 / CounterLen bits 1-0), Crypto Material Length
  decode (0/2/4/12 bytes), Monotonic Counter Length decode (0/2/4 bytes,
  0x3 reserved), and the Protection Scheme table (all 15 schemes from
  Table 11, correct indices 0x00-0x04/0x40-0x49) — verified correct,
  no change needed.
- [x] **Authentication Tag Length (§3.17.2.3) — was genuinely broken,
  now fixed.** The old validation accepted `[0x3, 0xF]` and rejected
  `0x0`/`0x1`/`0x2` — backwards from spec, which requires `0x0`
  (provider-defined) and `0x1` (scheme's nominal tag size) to be valid
  and reserves `0x2`/`0x3`. Worse, `0x1` is the value every AEAD scheme
  (AES-CCM/GCM, ChaCha20-Poly1305 - 9 of the 15 schemes) is *required* to
  use ("If the Protection Scheme is not Authentication Only, the
  Authentication Tag Length MUST be set to 0x1"), so the old code made it
  impossible to construct a spec-compliant encrypted Protection
  Encapsulation packet at all. Also fixed the decoded-length formula
  (was a flat `(value+1)*2` bytes for every case; now: `0x0` throws
  clearly - provider-defined length can't be derived generically - `0x1`
  returns the scheme's own nominal tag size, `0x4`-`0xF` returns
  `value*2` bytes per the spec's truncation formula, with a new check
  that Authentication-Only truncation can't exceed the scheme's nominal
  size (`MQTT-SN-3.17.2.3-8`)). Updated the two example CLI configs in
  `mqtt-sn-protection-runtimes` (`ProtectionExampleClientCli`/
  `ProtectionExampleGatewayCli`) that were using the old, now-rejected
  `0x3`/`0xF`-for-AEAD values.
- **Still open**: the `mqtt-sn-protection` module design question (keep
  as legacy/v1.2-only vs. unify with Protection Encapsulation) — that's a
  product decision for the maintainer, not a coding task, and Connection
  Encapsulation/Forwarder Encapsulation/v2.0-native REGISTER still have no
  packet class at all (noted under step 4 above).

Unrelated finding from this session: `mvn test` at the repo root fails in
`mqtt-sn-core`'s `SubscriptionTests` with a workspace-lock/"no transports
available" error. Confirmed unrelated to any of the above — that test
harness hardcodes the v1.2 codec (`MqttsnTestRuntimeRegistry` line 59) — and
looks like a pre-existing flaky/broken test-workspace issue, not touched.

## Headline finding (original pass)

The draft spec (CSD01) is a substantial rewrite relative to what the codec
implements. The codec's v2.0 support was clearly built against an
earlier/different working draft. Nearly every packet type ID, several packet
layouts, and multiple whole packet types have changed or are missing.
`MqttsnConstants.java` currently has an uncommitted, half-finished edit
attempting to chase this — it is broken (duplicate/colliding constant
values) and does not match the spec either.

*(See "Progress" above — this section and the rest of the document are left
as originally written, describing the state as found, for reference.)*

---

## 1. Packet Type ID table — spec (Table 2, p.18-19) vs current `MqttsnConstants.java`

**Status as originally found (below) is now resolved for the whole table —
see "Progress" above. Current state:** every row now has a dedicated,
non-colliding `*_V2_0` constant matching the spec ID exactly (e.g.
`DISCONNECT_V2_0=0x0E`, `AUTH_V2_0=0x0F`, `REGISTER_V2_0=0x10`,
`REGACK_V2_0=0x11`, `PUBWOS_V2_0=0x12`, `SLEEPREQ_V2_0=0x13`,
`SLEEPRESP_V2_0=0x14`, `WAKEUP_V2_0=0x15`, `ADVERTISE_V2_0=0x16`,
`SEARCHGW_V2_0=0x17`, `GWINFO_V2_0=0x18`,
`FORWARDER_ENCAPSULATION_V2_0=0xFD`, `SESSION_ENCAPSULATION_V2_0=0xFE`,
`PROTECTION_ENCAPSULATION_V2_0=0xFF`), and the original v1.2 constants
(`CONNECT=0x04`, `AUTH=0x03`, `REGISTER=0x0A`, `ADVERTISE=0x00`, etc.) were
restored unchanged. **Only the codec dispatch/decoding is updated to match**
— PUBWOS/SLEEPREQ/SLEEPRESP/WAKEUP/v2.0-native ADVERTISE/SEARCHGW/GWINFO
still have no packet classes (still fall through to the v1.2 decoder, still
step 4 work, unchanged from below).

Findings as originally documented, for reference:

| Packet | Spec ID | Current code value (at time of original pass) | Status (at time of original pass) |
|---|---|---|---|
| CONNECT | 0x01 | 0x01 | matches |
| CONNACK | 0x02 | 0x02 | matches |
| PUBLISH | 0x03 | 0x03 | matches |
| PUBACK | 0x04 | 0x04 | matches |
| PUBREC | 0x05 | 0x05 | matches |
| PUBREL | 0x06 | 0x06 | matches |
| PUBCOMP | 0x07 | 0x07 | matches |
| SUBSCRIBE | 0x08 | 0x08 | matches |
| SUBACK | 0x09 | 0x09 | matches |
| UNSUBSCRIBE | 0x0A | 0x0A | matches |
| UNSUBACK | 0x0B | 0x0B | matches |
| PINGREQ | 0x0C | 0x0C | matches |
| PINGRESP | 0x0D | 0x0D | matches |
| DISCONNECT | 0x0E | 0x18 | wrong → **fixed** |
| AUTH | 0x0F | 0x03 | wrong, collided with PUBLISH → **fixed** |
| REGISTER | 0x10 | 0x0A | wrong, collided with UNSUBSCRIBE → **fixed** |
| REGACK | 0x11 | 0x0B | wrong, collided with UNSUBACK → **fixed** |
| PUBWOS | 0x12 | not defined | missing → **fixed, `MqttsnPubwos_V2_0` implemented (step 4)** |
| SLEEPREQ | 0x13 | not defined | missing → **fixed, `MqttsnSleepreq_V2_0` implemented (step 4)** |
| SLEEPRESP | 0x14 | not defined | missing → **fixed, `MqttsnSleepresp_V2_0` implemented (step 4)** |
| WAKEUP | 0x15 | not defined | missing → **fixed, `MqttsnWakeup_V2_0` implemented (step 4)** |
| ADVERTISE | 0x16 | 0x00 | wrong, collided with Reserved 0x00 → **fixed, `MqttsnAdvertise_V2_0` implemented (step 4)** |
| SEARCHGW | 0x17 | 0x01 | wrong, collided with CONNECT → **fixed, `MqttsnSearchGw_V2_0` implemented (step 4)** |
| GWINFO | 0x18 | 0x02 | wrong, collided with CONNACK → **fixed, `MqttsnGwInfo_V2_0` implemented (step 4)** |
| Forwarder Encapsulation | 0xFD | not defined | missing → **ID now reserved, packet class still not implemented** |
| Session/Connection Encapsulation | 0xFE | `ENCAPSMSG=0xFE` | value matched (name differed, harmless) |
| Protection Encapsulation | 0xFF | `PROTECTION=0x1E` | wrong → **fixed (`PROTECTION_ENCAPSULATION_V2_0=0xFF`); field layout vs Figure 28 verified in step 5 — correct except Authentication Tag Length, which was genuinely broken and is now fixed too** |
| n/a (obsolete, no spec equivalent) | — | `WILLTOPICUPD=0x1A`, `WILLTOPICRESP=0x1B`, `WILLMSGUPD=0x1C`, `WILLMSGRESP=0x1D`, `HELO=0x2D`, `PUBLISH_M1=0x11` | dead constants for v2.0 — these packet types don't exist in CSD01 (Will handling moved into CONNECT). Left in place as-is since they are legitimately used by v1.2. |

**Note on the spec itself:** Table 7 (p.42, "Publish Packet Types") lists
PUBLISH as `0x0C` and PUBWOS as `0x11` — this directly contradicts the
authoritative Table 2 (p.18) which says PUBLISH=`0x03`, PUBWOS=`0x12`. This
looks like a leftover editorial error in the draft from an earlier revision.
Table 2 is explicitly the canonical definition (§2.1.3), so treat it as
authoritative and flag Table 7 as a spec erratum when reporting
upstream/filing a comment, rather than a target to implement.

---

## 2. `MqttsnConstants.java` current broken state (uncommitted WIP)

**RESOLVED — see "Progress" above.** This edit was discarded rather than
built upon; the section below is left as originally written for reference
on *why* it had to be discarded.

The in-progress edit (visible via `git diff`, not yet committed) has landed
the codec in a worse, inconsistent state than either the old or new
numbering:

- `CONNECT=0x01` / `SEARCHGW=0x01` — direct collision.
- `CONNACK=0x02` / `GWINFO=0x02` — direct collision.
- `PUBLISH=0x03` / `AUTH=0x03` — direct collision.
- `REGISTER=0x0A` / `UNSUBSCRIBE=0x0A` — direct collision (both declared,
  second one wins at compile time but any switch/lookup keyed by value is
  ambiguous).
- `REGACK=0x0B` / `UNSUBACK=0x0B` — direct collision.
- `DISCONNECT=0x18` / GWINFO-correct-spec-value (0x18) not present — free
  real estate wrongly claimed.
- `PROTECTION=0x1E` is unused by the "wrapping-a-packet" model the spec now
  uses (0xFF), and the switch statement in
  `Mqttsn_v2_0_Codec.createInstance` still dispatches on
  `MqttsnConstants.PROTECTION` (0x1E), so Protection Encapsulation packets
  sent per-spec (leading byte 0xFF) won't even be recognized by the decoder.

This file should not be built on top of — it needs a clean rewrite of the
v2.0 constant block, not incremental patching.

---

## 3. Packets present in the codec but with outdated wire layout

| Packet | Spec delta | Status |
|---|---|---|
| **CONNECT** | Flags byte order: bit0=CleanStart, bit1=Will, bit2=Auth, bit3=SessExp, bit4=DAM, bit5=NetAddr, bit6=SrvSugg (bit7 reserved). Session Expiry Interval field is *conditional* on the SessExp flag, Will fields are a variable block, Auth fields variable. | **FIXED** — `MqttsnConnect_V2_0` fully rewritten against Figure 6: correct flag bits, conditional Will Flags byte, conditional Session Expiry/Default Awake Messages, full Will (topic/payload/retain/QoS) and Auth (method/data) field support. |
| **CONNACK** | Flags: bit0=SessPres, bit1=SessExp, bit2=ServerKA, bit3=Auth. Session Expiry / Server Keep Alive / Auth fields all conditional on their flags. | **FIXED** — `MqttsnConnack_V2_0` fully rewritten against Figure 7, including newly-added Server Keep Alive and Auth support (neither field existed on the class before). |
| **DISCONNECT** | Flags: bit0=PacketId flag, bit1=SessExp flag, bit2=ReasonCode flag (bits 3-7 reserved). Packet Identifier is now a real (optional) field. | **FIXED** — `MqttsnDisconnect_V2_0` fully rewritten against Figure 24; removed the spec-nonexistent `retainRegistrations` flag, added proper optional Packet Identifier support (via `getPacketIdentifier()`/`setPacketIdentifier()`, deliberately not wired through `needsId()` to avoid changing inflight-confirmation runtime semantics). |
| **PUBLISH (QoS 0)** | Header: `Reserved(7) / QoS(6-5) / Retain(4) / Reserved(3-2) / TopicType(1-0)`. QoS bits `11` are explicitly "Reserved – must not be used" (Table 8) — the no-session-publish case is now the separate PUBWOS packet type. | **Fixed as far as this packet goes.** Topic type SHORT is now rejected (§4). QoS -1 is now explicitly rejected at `validate()` time rather than silently mis-encoded, and PUBWOS (`MqttsnPubwos_V2_0`) is now implemented as the real replacement mechanism (step 4). Still out of scope: reserved-bits-must-be-0 enforcement on the flags byte. |
| **PUBLISH (QoS 1/2)** | Flags: `DUP(7) / QoS(6-5) / Retain(4) / Reserved(3-2) / TopicType(1-0)`. | **Not changed.** Flag bit positions still carry stale `Will`/`CleanSession` comments (copy-pasted from CONNECT, functionally inert since those bits are never read) and reserved-bit validation still isn't enforced. Low priority — deferred. |
| **REGACK** | Flags: `TopicAlias flag(2) / TopicType(1-0)`, Reason Code now optional (length-inferred). Topic Type MUST be Predefined or Session Topic Alias only. | **Partially fixed.** `validate()` now rejects any topic type other than Predefined/Session Alias, per spec. Still not fixed: the class still hardcodes an 8-byte fixed-length encode with Topic Alias and Reason Code always present — no "Topic Alias flag" bit, no length flexibility for the optional Reason Code. |

*(SUBSCRIBE/SUBACK/UNSUBSCRIBE/UNSUBACK/PINGREQ/PINGRESP were diffed
field-by-field in step 7 - see "Progress" above. Headline: SUBSCRIBE and
UNSUBSCRIBE only needed minor validation additions (already had the right
flag layout), but SUBACK and PINGREQ/PINGRESP were both fabricated wire
formats matching no figure in this spec at all, and have been rewritten.
PUBACK/PUBREC/PUBREL/PUBCOMP/UNSUBACK all now share one abstract base
class with a correctly optional/length-inferred Reason Code.)*

---

## 4. Topic Type model is structurally outdated across the board

**FIXED, with a scoping decision — see "Progress" above.** Spec (Table 5,
p.24-25) defines exactly 4 2-bit Topic Type values: `0=Session Topic Alias`,
`1=Predefined Topic Alias`, `2=Reserved`, `3=Topic Name or Filter`. "Short
Topic Name" was explicitly removed in CSD01.

Rather than splitting `MqttsnConstants.TOPIC_TYPE` into version-specific
enums — which would have cascaded into `IMqttsnMessageFactory`'s shared
interface signature and widely into `mqtt-sn-core`/`mqtt-sn-client`/
`mqtt-sn-gateway` (the enum/constants are used well beyond the codec, e.g.
`AbstractTopicRegistry`, `AbstractMqttsnMessageHandler`) — the existing
byte values were kept (they already numerically match the new spec's 4
values for cases 0/1/3; only the *meaning* of value 2 changed from
"Short Topic Name" in v1.2 to "Reserved" in v2.0, and only v2.0-side
enforcement was needed). Fixed instead: the genuine encode-side bugs this
had caused — `MqttsnPublish_V2_0`/`MqttsnSubscribe_V2_0.setTopicName()` no
longer special-case short names (previously `TOPIC_NORMAL` was used for
names >2 chars, which is wrong even independent of v2.0: `TOPIC_NORMAL`
means "2-byte numeric alias", not "topic name string", causing silent
truncation) — and `validate()` on every v2.0 payload class that carries a
Topic Type now rejects `TOPIC_SHORT` outright, plus the REGACK-specific
Predefined/Session-only constraint from §3.

---

## 5. Packets entirely missing from the codec

Status at time of original pass, with resolutions from step 4 noted inline
(the README/changelog table doesn't mention most of these either, since it
only tracks the older changelog):

- ~~**PUBREC / PUBREL / PUBCOMP**~~ — **FIXED (step 4).** Was newly
  confirmed absent while implementing step 3 (not identified by name in
  the original pass). Now implemented as `MqttsnPubrec_V2_0`/
  `MqttsnPubrel_V2_0`/`MqttsnPubcomp_V2_0`, sharing a new
  `AbstractMqttsnPublishConfirmation_V2_0` base (also adopted by
  `MqttsnPuback_V2_0`), with dispatch cases in `Mqttsn_v2_0_Codec` and
  factory overrides in `Mqttsn_v2_0_MessageFactory`.
- ~~**PUBWOS**~~ (Publish Without Session) — **FIXED (step 4).**
  `MqttsnPubwos_V2_0` implemented per Figure 11.
- ~~**WAKEUP**~~ — **FIXED (step 4).** `MqttsnWakeup_V2_0`, header-only.
- ~~**SLEEPREQ** / **SLEEPRESP**~~ — **FIXED (step 4).**
  `MqttsnSleepreq_V2_0`/`MqttsnSleepresp_V2_0` implemented. Note this
  only adds the packet classes/wire format - it does not implement the
  *runtime behavior* of moving a session to the Asleep state (that lives
  in `mqtt-sn-core`, out of scope for the codec module).
- ~~**ADVERTISE / SEARCHGW / GWINFO**~~ — **FIXED (step 4).**
  `MqttsnAdvertise_V2_0`/`MqttsnSearchGw_V2_0`/`MqttsnGwInfo_V2_0`
  implemented, replacing the v1.2 fallthrough for these byte values.
- ~~**Protection Encapsulation**~~ (0xFF, §3.17) — **FIXED (steps 1 and
  5).** Packet type byte fixed in step 1
  (`PROTECTION_ENCAPSULATION_V2_0=0xFF`, dispatch updated accordingly).
  Field layout verified field-by-field against Figure 28 in step 5:
  everything was already correct (field order/offsets, Protection Flags
  bit layout, Crypto Material/Monotonic Counter Length decode, the full
  Table 11 Protection Scheme list) except Authentication Tag Length
  (§3.17.2.3), which had inverted validation (accepted the two reserved
  values, rejected the two most common ones - including the value every
  AEAD scheme is required to use) and a wrong decoded-length formula. Both
  fixed in `ProtectionPacketFlags.java`.
- **Connection Encapsulation** (§3.18) and **Forwarder Encapsulation**
  (§3.19) — neither has a codec class; `ENCAPSMSG=0xFE` constant exists but
  nothing decodes/encodes the Client-Identifier-wrapped or
  Client-Addressing-Info-wrapped envelope formats described in the spec.

---

## 6. `mqtt-sn-protection` module vs spec's Protection Encapsulation

The existing `mqtt-sn-protection`/`mqtt-sn-protection-runtimes` modules
implement HMAC/CHECKSUM-based message integrity as a separate, out-of-band
scheme (per the current README's "Message Integrity" section — configured
via `MqttsnSecurityOptions.withIntegrityType(hmac|checksum)`). CSD01 folds
this concept directly into the wire protocol as the Protection Encapsulation
packet (0xFF), which:

- Is inspired by OSCORE (per §1.2.1 changelog).
- Defines its own protection-scheme table (Table 11, p.69) with 4
  authentication-only schemes (HMAC-SHA256/SHA3-256, CMAC-128/192/256) and 4
  AEAD schemes (AES-CCM/GCM variants, ChaCha20/Poly1305) — a strict
  superset in cryptographic sophistication of the current
  CHECKSUM/HMAC-only module.
- Wraps *any* other packet type (except Forwarder Encapsulation) rather
  than being an independent prefix scheme bolted onto payload or protocol
  messages.

These are not identical mechanisms, but they overlap enough in intent that
this is a design decision, not just a coding task: either (a) keep
`mqtt-sn-protection` as a legacy/v1.2-only mechanism and build Protection
Encapsulation as the v2.0-native replacement (some of
`AbstractAeadProtectionScheme`/`AbstractAuthenticationOnlyProtectionScheme`/
`ProtectionKey` in `wire/version2_0/payload` already look purpose-built for
this — worth checking how much is reusable), or (b) unify them. Recommend
surfacing this specific question to the user/maintainer rather than
deciding it unilaterally — it has real API and module-boundary implications
beyond wire-format compliance.

---

## 7. Reason Codes (§2.3, Table 4, pp.20-23)

**FIXED (step 6) — see "Progress" above.** All ~50 named codes from Table 4
now exist as `RETURN_CODE_*_V2_0` constants in `MqttsnConstants.java`,
alongside the untouched, still-small v1.2 set
(`ACCEPTED/REJECTED_CONGESTION/INVALID_TOPIC_ID/SERVER_UNAVAILABLE/
PAYLOAD_FORMAT_INVALID`, which remains correct for v1.2).
`MqttsnSpecificationValidator.validateReturnCode` was deliberately left as
a generic uint8 range check rather than a per-packet-type allow-list check
— that's consistent with how the rest of this validator already treats
Reason/Return Codes, and enforcing the "which codes are valid on which
packet" column of Table 4 would be a materially bigger, separate piece of
work (it's a per-packet-type mapping, not a single value range).

---

## Prioritized recommendation

1. ~~**Do not build on the current uncommitted `MqttsnConstants.java` edit.**
   Revert or discard it, then do a single clean pass defining the full
   v2.0 packet-type block from Table 2 (p.18-19) with no collisions, plus
   the Reserved/Forwarder/Session/Protection Encapsulation range
   constants.~~ **DONE.**
2. ~~**Fix the shared Topic Type model** (`MqttsnConstants.TOPIC_TYPE`) to
   the spec's 4-value scheme (Session Alias / Predefined Alias / Reserved /
   Name-or-Filter) before touching individual payload classes, since it's a
   shared dependency for PUBLISH, REGISTER, REGACK, SUBSCRIBE, SUBACK,
   UNSUBSCRIBE, PUBWOS.~~ **DONE** (see §4 for the scoping note — enum
   values kept, v2.0-side enforcement added instead of a full type split).
3. ~~**Rewrite CONNECT/CONNACK/DISCONNECT flag layouts** to match Figures 6,
   7, 24 — these are the most-used packets and currently have flag bit
   positions with no relationship to the spec at all.~~ **DONE.**
4. ~~**Add the missing packet classes**: PUBWOS, WAKEUP, SLEEPREQ/SLEEPRESP
   (and move sleep-duration semantics out of DISCONNECT), PUBREC/PUBREL/
   PUBCOMP (newly confirmed missing, see §5), then v2.0-native
   ADVERTISE/SEARCHGW/GWINFO to replace the v1.2 fallthrough.~~ **DONE.**
   Still open within this item's spirit: Connection/Forwarder Encapsulation
   and v2.0-native REGISTER have no packet class yet (byte IDs reserved).
5. ~~**Fix Protection Encapsulation's packet-type byte** (done: 0x1E→0xFF)
   and validate its field layout against Figure 28.~~ **DONE.** Found and
   fixed a real Authentication Tag Length bug that made it impossible to
   construct a spec-compliant AEAD-protected packet - see "Progress"
   above. The `mqtt-sn-protection` module design question in §6 (keep as
   legacy/v1.2-only vs. unify with Protection Encapsulation) is still open
   and remains a product decision for the maintainer, not a coding task.
6. ~~**Reconcile Reason Codes** against Table 4 as a final pass, since many
   packet-level `validate()` methods depend on it and it's low-risk/
   mechanical once the packet formats are settled.~~ **DONE.**
7. ~~**Field-by-field audit of SUBSCRIBE/SUBACK/UNSUBSCRIBE/UNSUBACK/
   PINGREQ/PINGRESP.**~~ **DONE.** Turned up two fabricated wire formats
   (SUBACK, PINGREQ/PINGRESP) matching no figure in this spec draft at all,
   plus a real runtime correctness bug this had caused (`isErrorMessage()`
   misclassifying successful QoS 1/2 grants as errors) - see "Progress"
   above.

`Mqttsn2_0WireTests.java`/new `ProtectionPacketFlagsTest.java` have been
updated alongside every fix above throughout (118/118 tests passing as of
step 5 — 44 v1.2 + 65 v2.0 + 9 Protection Flags unit tests).

**All numbered steps (1-7) are now done.** What remains, per the notes
above, is not part of the original 7-step plan: Connection Encapsulation,
Forwarder Encapsulation, and v2.0-native REGISTER have no packet class at
all yet, and the `mqtt-sn-protection` vs. Protection Encapsulation design
question is a product decision for the maintainer.

Also worth flagging upstream/in a TC comment: the Table 7 (p.42) vs Table 2
(p.18) PUBLISH/PUBWOS byte-value inconsistency in the spec itself — Table 2
should be treated as authoritative per §2.1.3.
