# cloud-itonami-7020

Open Business Blueprint for **ISIC Rev.5 7020**: Management consultancy activities.

This repository designs a forkable OSS business for management consultancy -- advising client organizations on strategy, operations, organization and management practice -- run by a qualified, licensed operator so a community or
independent professional never surrenders customer data and ledgers to a
closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-courier robot handles physical deliverable/binder handoff where used,
under an actor that proposes actions and an independent **Consulting Engagement Governor**
that gates them. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Core Contract

```text
intake + identity + engagement records
        |
        v
Consultant-LLM -> Consulting Engagement Governor -> hold, proceed, or human approval
        |
        v
engagement ledger + evidence record + audit
```

No automated proposal, by itself, can complete the following without governor
approval and audit evidence: issuing a recommendation/deliverable to a client.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7020`).

This vertical's engagement records are practice-specific rather than a shared
cross-operator data contract, so it runs on the generic identity/forms/dmn/
bpmn/audit-ledger stack only -- no bespoke domain capability lib.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Maturity

`:blueprint` -- this repository is the published business/operator design.
The governed actor implementation (`Consultant-LLM` + `Consulting Engagement Governor` as
running code) is a follow-up, same as any other `:blueprint`-tier
`cloud-itonami-*` entry in `kotoba-lang/industry`'s registry.

## License

Code and implementation templates are AGPL-3.0-or-later.
