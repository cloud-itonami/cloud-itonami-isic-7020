# ADR-0001: Consultant-LLM ⊣ Consulting Engagement Governor architecture

## Status

Accepted. `cloud-itonami-isic-7020` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7020` publishes an OSS business blueprint for
management consultancy: advising client organizations on strategy,
operations, organization and management practice, run by a qualified,
licensed operator so a community or independent professional never
surrenders customer data and ledgers to a closed SaaS. Like every
prior actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph-clj
StateGraph + independent Governor + Phase 0→3 rollout pattern
established by `cloud-itonami-isic-6511` (life insurance) and applied
across forty-four prior siblings, most recently `cloud-itonami-isic-
3830` (materials recovery).

## Decision

### Decision 1: this fleet's FIRST professional-services vertical

Every prior actor in this fleet has been a human-services, leisure,
financial-services, infrastructure/utility, manufacturing, or
circular-economy domain. `cloud-itonami-isic-7020` is the FIRST to
model a PROFESSIONAL-SERVICES/advisory-work process. Unlike prior
verticals, this blueprint's own README frames the robotics premise as
thin (a document-courier robot handling physical deliverable/binder
handoff "where used") -- the actual work is advisory, not physical.
This is also this fleet's first SINGLE-actuation-shape build since
`leasing`/6491 (the blueprint's own Trust Controls name only ONE
real-world act, "issuing a recommendation/deliverable to a client", as
requiring governor gating), a useful structural confirmation that the
governed-actor pattern scales down cleanly to the leanest blueprint
tier, not just the richest.

### Decision 2: entity and op shape

The primary entity is an `engagement` (a client consulting engagement,
analogous to `leasing.store`'s `lease`). Four ops: `:engagement/intake`
(directory upsert, no capital risk), `:finding/research` (per-
jurisdiction professional-standards evidence checklist, never auto --
analogous to `leasing.operation`'s jurisdiction-assessment concept),
`:conflict/screen` (conflict-of-interest screening, unconditional-
evaluation discipline, never auto), and `:actuation/issue-deliverable`
(SINGLE, high-stakes -- issuing the real recommendation/deliverable to
the client). This matches `leasing`'s single-actuation-on-one-entity
shape exactly (one history collection, one sequence counter, one
dedicated double-actuation-guard boolean).

### Decision 3: `engagement-scope-exceeded?` -- the 4th set-containment/subset check, opposite polarity

Following `registrar.registry/prerequisites-satisfied?` (1st),
`casework.registry/eligibility-criteria-unsatisfied?` (2nd) and
`secondary.registry/graduation-requirements-unsatisfied?` (3rd),
`consulting.registry/engagement-scope-exceeded?` applies the SAME
`clojure.set/subset?` mechanism to an engagement's own recorded
`:recommendation-topics` against its own recorded `:contracted-scope-
items`. Unlike its three predecessors (all check whether a REQUIRED
set is satisfied by an ACTUAL/COMPLETED set -- a "sufficiency"
framing), this check verifies the OPPOSITE polarity: whether a
PROPOSED set stays within a CONTRACTED/PERMITTED set -- a "boundary/
permission" framing. This generalizes the set-containment family
beyond pure sufficiency checks into permission-boundary checks, a
genuine structural contribution documented here rather than silently
assumed identical to the prior three.

### Decision 4: `conflict-of-interest-unresolved-violations` -- 29th discipline grounding, 4th concept grounding, first outside financial services

Before writing this check's docstring, every prior sibling's
`governor.cljc` was grepped for `conflict-of-interest` -- THREE hits
were found (`adjustment.governor` 6621, `intermediation.governor`
6622, `brokerage.governor` 6612), confirming the specific conflict-of-
interest CONCEPT is a REUSE, not new (avoiding the false-precedent-
claim risk `leasing`'s ADR-0001 documents). This is the FOURTH
grounding of that specific concept, and the FIRST outside a financial-
services context (insurance adjustment, insurance intermediation and
securities brokerage all being finance-adjacent; management
consultancy is a general professional-services domain). The broader
"unconditional-evaluation" discipline itself (`casualty.governor/
sanctions-violations`'s original fix) reaches its 29th distinct
application overall, continuing the count established across this
window's builds (water=25th, telecom=26th, aerospace=27th,
recovery=28th, consulting=29th). Exercised in tests/demo via
`:conflict/screen` DIRECTLY against an already-flagged engagement, not
via the actuation op against an unscreened engagement -- the "screen
the screening op directly, not the actuation op" lesson `parksafety`'s
ADR-2607071922 Decision 5 established, now applied for a NINETEENTH
consecutive sibling (`facility`=8th, `school`=9th, `association`=10th,
`leasing`=11th, `behavioral`=12th, `secondary`=13th, `card`=14th,
`water`=15th, `telecom`=16th, `aerospace`=17th, `recovery`=18th,
`consulting`=19th).

### Decision 5: dedicated double-actuation-guard boolean

`:deliverable-issued?` is a dedicated boolean on the `engagement`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`consulting.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/consulting/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
`:contracted-scope-items`/`:recommendation-topics` (Clojure sets) are
EDN-string-encoded on the Datomic-backed entity itself, the same
convention every sibling actor's compound-value fields use.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:engagement/intake` (no
capital risk). `:finding/research` and `:conflict/screen` are never
auto-eligible at any phase (matching every sibling's screening-op
posture), and `:actuation/issue-deliverable` is permanently excluded
from every phase's `:auto` set -- a structural fact, not a rollout
milestone, enforced by BOTH `consulting.phase` and `consulting.
governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib

This vertical's engagement records are practice-specific rather than a
shared cross-operator data contract, so `consulting.*` runs on the
generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only --
the same posture `9412`/`8720`/`8521`/`3030`/`3830` and others without
a bespoke capability lib already establish.

### Decision 9: mock + LLM advisor pair

`consulting.consultantadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-issuing a
deliverable).

## Alternatives considered

- **A dual-actuation shape** (matching the fleet's more common
  pattern). Rejected: the blueprint's own published Trust Controls and
  Core Contract name exactly ONE real-world act requiring governor
  gating ("issuing a recommendation/deliverable to a client") --
  inventing a second actuation concept not grounded in the blueprint's
  own text would be fabricating scope, the exact discipline `consulting.
  facts` itself is built to avoid.
- **Claiming `conflict-of-interest-unresolved-violations` as a NEW
  check-family concept.** Rejected after the grep-verified precedent
  check in Decision 4 -- it is a reuse of an existing concept
  (established by `adjustment`/`intermediation`/`brokerage`), applied
  here to a new, non-financial domain. Honestly framed as the 4th
  grounding of an existing concept, not a novel contribution, following
  the verification discipline `leasing`'s ADR-0001 established.
- **Treating `engagement-scope-exceeded?` as unrelated to the existing
  set-containment family** (since its polarity is inverted). Rejected:
  the underlying mechanism (`clojure.set/subset?` comparison between
  two sets on the same entity) is identical; documenting the polarity
  difference as a deliberate generalization (Decision 3) is more
  useful for future actors than either falsely claiming identical
  framing or inventing an unrelated family name for what is
  mechanically the same check shape.

## Consequences

- Forty-fifth actor in this fleet (44 implemented before this build),
  and the FIRST professional-services vertical.
- Confirms the governed-actor pattern applies cleanly to the leanest
  blueprint tier (single actuation, generic boilerplate Core Contract)
  as well as the richest.
- Confirms the set-containment/subset check family generalizes beyond
  a "sufficiency" framing into a "permission-boundary" framing (opposite
  polarity), a genuine structural contribution.
- Correctly identifies conflict-of-interest as a REUSED concept (not
  novel), the 4th grounding overall and the first outside financial
  services -- avoiding a false-precedent-claim error.
