# cloud-itonami-isic-7020

Open Business Blueprint for **ISIC Rev.5 7020**: Management consultancy
activities.

This repository publishes a management-consultancy actor -- engagement
intake, professional-standards finding/research, conflict-of-interest
screening and deliverable issuance -- as an OSS business that any
qualified, licensed consultant can fork, deploy, run, improve and
sell, so a community or independent professional never surrenders
customer data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830)) --
the FIRST professional-services vertical in this fleet (every prior
actor has been a human-services, leisure, financial-services,
infrastructure/utility, manufacturing or circular-economy domain).
Here it is **Consultant-LLM ⊣ Consulting Engagement Governor**.

> **Why an actor layer at all?** An LLM is great at drafting an
> engagement-intake summary, normalizing records, and checking whether
> a proposed recommendation actually stays within its own contracted
> engagement scope -- but it has **no notion of which jurisdiction's
> professional-standards requirements are official, no license to
> issue a real recommendation/deliverable to a client, and no way to
> know on its own whether a conflict of interest against the
> engagement has actually stayed unresolved**. Letting it issue a
> deliverable directly invites fabricated professional-standards
> citations, scope-creep recommendations the client never contracted
> for, and an unresolved conflict of interest being quietly
> overlooked -- and liability, and professional-ethics risk, for
> whoever runs it. This project seals the Consultant-LLM into a single
> node and wraps it with an independent **Consulting Engagement
> Governor**, a human **approval workflow**, and an immutable **audit
> ledger**.

## Scope: what this actor does and does not do

This actor covers engagement intake through professional-standards
finding/research, conflict-of-interest screening and deliverable
issuance. It does **not**, by itself, hold any professional license
required to practice management consultancy in a given jurisdiction,
and it does not claim to. It also does **not** model a real
engagement-management/CRM system, real strategy-analysis tooling, or
the actual analytical work a consultant performs -- no methodology
engine (see `consulting.facts`'s own docstring for the honest
simplification this makes: a starting catalog of professional-
standards bodies, not a survey of every jurisdiction's consulting-
regulation variant). Whoever deploys and operates a live instance (a
licensed management consultant) supplies any jurisdiction-specific
license, the real analytical/strategic expertise and the real
engagement-management integrations, and bears that jurisdiction's
liability -- the software supplies the governed, spec-cited, audited
execution scaffold so that consultant does not have to build the
compliance layer from scratch for every new engagement.

### Actuation

**Issuing a real recommendation/deliverable to a client is never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`consulting.governor`'s `:actuation/issue-deliverable`
high-stakes gate and `consulting.phase`'s phase table, which never
puts `:actuation/issue-deliverable` in any phase's `:auto` set) -- see
`consulting.phase`'s docstring and `test/consulting/phase_test.clj`'s
`issue-deliverable-never-auto-at-any-phase`. The actor may draft,
check and recommend; a human consultant is always the one who
actually issues a deliverable. Unlike the majority of siblings in this
fleet, this actor has a SINGLE actuation event -- matching `leasing`'s/
`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/`funeral`'s/
`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s single-
actuation shape, since this blueprint's own published Trust Controls
name only ONE real-world act ("issuing a recommendation/deliverable
to a client") as requiring governor gating.

## The core contract

```
engagement intake + jurisdiction facts (consulting.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ Consultant-  │ ─────────────▶ │ Consulting                    │  (independent system)
   │ LLM (sealed) │  + citations    │ Engagement Governor:         │
   └──────────────┘                 │ spec-basis · evidence-       │
                             commit ◀────┼──────────▶ hold │ incomplete ·
                                 │             │           │ engagement-scope-
                           record + ledger  escalate ─▶ human   exceeded (subset) ·
                                             (ALWAYS for         conflict-of-interest-
                                              :actuation/issue-           unresolved
                                              deliverable)                (unconditional) ·
                                                                           already-issued
```

**The Consultant-LLM never issues a deliverable the Consulting
Engagement Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated professional-standards
requirements; unsupported evidence; a deliverable that exceeds the
contracted scope; an unresolved conflict of interest; a double
issuance) force **hold** and *cannot* be approved past; a clean
issuance proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a document-courier robot
handles physical deliverable/binder handoff where used, under the
actor, gated by the independent **Consulting Engagement Governor**.
The governor never dispatches hardware itself; `:high`/`:safety-
critical` actions require human sign-off. Like every professional-
services vertical in this fleet, the physical-robotics component here
is thin (document handling) compared to the analytical/advisory work
itself -- the governed-actor pattern is applied to the DELIVERABLE
gating, not a physical dispatch action.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Consulting Engagement Governor, deliverable-issuance draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7020`). This vertical's engagement records are practice-specific
rather than a shared cross-operator data contract, so `consulting.*`
runs on the generic robotics/identity/forms/dmn/bpmn/audit-ledger
stack only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/consulting/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + deliverable-issuance history. No dynamically-filed sub-record -- the single actuation op acts directly on a pre-seeded engagement, and the double-actuation guard checks a dedicated `:deliverable-issued?` boolean rather than a `:status` value |
| `src/consulting/registry.cljc` | Deliverable-issuance draft records, plus `engagement-scope-exceeded?` -- the FOURTH instance of this fleet's set-containment/subset check family (`registrar`/`casework`/`secondary` established the first three), in the opposite polarity (a proposed set must stay a subset of a contracted set) |
| `src/consulting/facts.cljc` | Per-jurisdiction management-consultancy professional-standards catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/consulting/consultantadvisor.cljc` | **Consultant-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/finding-research/conflict-screening/deliverable-issuance proposals |
| `src/consulting/governor.cljc` | **Consulting Engagement Governor** -- 4 HARD checks (spec-basis · evidence-incomplete · engagement-scope-exceeded, pure ground-truth subset recompute · conflict-of-interest-unresolved, unconditional evaluation, the TWENTY-NINTH grounding of this discipline and FOURTH grounding of the specific conflict-of-interest concept, first outside financial services) + already-issued guard + 1 soft (confidence/actuation gate) |
| `src/consulting/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (deliverable issuance always human; engagement intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/consulting/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/consulting/sim.cljc` | demo driver |
| `test/consulting/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers engagement intake through professional-standards
finding/research, conflict-of-interest screening and deliverable
issuance -- the core governed lifecycle this blueprint's own `docs/
business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Engagement intake + per-jurisdiction professional-standards checklisting, HARD-gated on an official spec-basis citation (`:engagement/intake`/`:finding/research`) | Real engagement-management/CRM system integration, real strategy-analysis/methodology tooling (see `consulting.facts`'s docstring) |
| Conflict-of-interest screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:conflict/screen`) | The actual analytical/strategic work a consultant performs |
| Deliverable issuance, HARD-gated on full evidence and contracted-scope sufficiency, plus a double-issuance guard (`:actuation/issue-deliverable`) | |
| Immutable audit ledger for every intake/research/screening/issuance decision | |

Extending coverage is additive: add the next gate (e.g. a billing-
milestone check) as its own governed op with its own HARD checks and
tests, following the SAME "an independent governor re-verifies against
the actor's own records before any real-world act" pattern this
repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`consulting.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `consulting.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `consulting.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `Consultant-LLM` + `Consulting Engagement Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, modeled closely on
the forty-four prior actors' architecture. See `docs/adr/0001-
architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
