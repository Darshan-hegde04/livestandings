# Agent notes — livestandings

Live standings board. Project rules live in `.cursor/rules/`.

Human docs: [ARCHITECTURE.md](./ARCHITECTURE.md) (HLD/LLD), [DECISIONS.md](./DECISIONS.md), [README.md](./README.md).

| Rule | When |
|------|------|
| `project.mdc` | Always — floor, stack, out of scope |
| `architecture.mdc` | Always — Kafka, corrections, recompute, SSE, metrics |
| `java-spring.mdc` | Java / YAML |
| `frontend.mdc` | `web/**` React |
| `compose-ops.mdc` | Compose, README / DECISIONS |

Prefer a working floor over extra features. Do not write standings from the submit endpoint — path is REST → Kafka → consumer → MySQL.
