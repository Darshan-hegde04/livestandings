# Architecture decisions

This file records the **choices that keep standings correct under Sunday burst load** — not every implementation detail. Each decision has a context, what we chose, why, what it costs, and what we rejected.

Related: [ARCHITECTURE.md](./ARCHITECTURE.md) (HLD/LLD) · [README.md](./README.md) (how to run)

---

## At a glance

| ID | Decision | One-line summary |
|----|----------|------------------|
| **D1** | Corrections | Kafka offset order wins — not phone clocks |
| **D2** | Kafka ordering | Key by `leagueId`; ack only after DB commit |
| **D3** | Recompute | Full league standings from latest row per match |
| **D4** | Metrics | AdminClient ingest lag + apply latency in Grafana |
| — | Real-time | SSE in-process after apply (single instance) |
| — | Auth | Organiser token on writes; reads public |

**Invariant shared by all of them:** the submit/correct REST endpoint never writes standings. Path is always REST → Kafka → apply → MySQL → SSE (same JVM).

> **Default (`main`):** In-process SSE (no Kafka fan-out). Branch `variant/kafka-sse-fanout` keeps the multi-instance fan-out design.

---

## D1 — Corrections

### Context

Organisers fix typos (~10–15% of results). Phones can be offline, clocks can drift, and Kafka may redeliver after a crash. Captains must **never** briefly rank on an overwritten score.

### Choice

- Authority is **Kafka partition offset order** for that league — not `reported_at` from the phone.
- Store `source_partition` + `source_offset` on each `match_results` row.
- Upsert only when the incoming offset is **greater** on the same partition (same/older → reject as stale/duplicate).
- Bump `league_state.version` in the **same DB transaction** as the upsert + standings replace.
- The React board ignores SSE snapshots with an older `version`.

### Why

Offset order is broker truth for a keyed stream. Phone timestamps are not. Version gating stops a late fan-out or reconnect from flashing a superseded table.

### Cost

Produces must be keyed by `leagueId` (see D2) so one league’s corrections stay totally ordered on one partition.

### Rejected

| Alternative | Why not |
|-------------|---------|
| Trust `reported_at` | Clock skew / offline devices reorder corrections |
| Last-write-wins without offsets | Duplicate replay after crash can corrupt standings |
| Client-only “latest wins” | Server would still publish wrong rankings |

---

## D2 — Kafka ordering

### Context

Results for one league must apply in a stable order. Clumps of 20–30 can hit at once; crashes must not lose “applied” work or silently skip it.

### Choice

| Piece | Setting |
|-------|---------|
| Topic | `match-results` |
| Key | `leagueId` (never null) |
| Partitions | 3 (room to grow toward ~5) |
| Producer | `acks=all`, `enable.idempotence=true`, `max.in.flight ≤ 5` |
| Consumer | Manual ack **after** the DB transaction commits (at-least-once) |

### Why

Same key → same partition → total order per league. Idempotent producer + `acks=all` avoids silent loss on broker failover. Ack-after-commit means a crash mid-apply redelivers; D1 makes that safe.

### Cost

One hot league is bound to one partition’s throughput. That matches “one busy Sunday league,” not a multi-tenant firehose.

### Rejected

| Alternative | Why not |
|-------------|---------|
| Null keys / round-robin | Cross-partition reordering within a league |
| Auto-commit before apply | Crash after commit-to-Kafka-offset, before DB → lost updates |
| Exactly-once transactions end-to-end | Extra complexity; at-least-once + D1 is enough here |

---

## D3 — Recompute (not incremental)

### Context

After an accepted result (or a batch of them), standings must match “latest score per match.” Incremental row tweaks are easy to get wrong under replay or a rejected-then-retried message.

### Choice

- On apply: load the latest row per match for that league (~200), **recompute** the full table, replace `standings`.
- Batch listener: **one recompute per league per poll batch** (clump of 30 → one recompute, not thirty).

### Why

Full recompute cannot silently drift if a message is applied twice or an older offset is skipped. Cost is O(matches) — fine at Sunday scale.

### Cost

CPU/DB work scales with match count per accepted batch. At ~200 matches this is intentional headroom, not a bottleneck.

### Rejected

| Alternative | Why not |
|-------------|---------|
| Incremental snapshot deltas | Silent drift on double apply / out-of-order edge cases |
| Recompute once per message only | Wasteful under clumps; batch already groups the work |

---

## D4 — Metrics

### Context

Reviewers (and operators) need to see whether the board is **falling behind** the phone submits — not a frozen or misleading client lag number.

### Choice

| Metric | Source / meaning |
|--------|------------------|
| `standings_ingest_lag` | LagProbe via AdminClient: Σ max(0, log-end − committed) for `match-results` / applier group |
| `standings_apply_latency` | Timer from event `submittedAt` → apply DB commit |
| Apply rate | Accepted applies / time |
| Stale / duplicate rejections | D1 guards firing |

Prometheus scrapes Actuator; Grafana dashboard shows lag, latency, rate, rejections (anon viewer in compose).

### Why

Broker log-end vs committed offset is ground truth for backlog. Client `records-lag` can stick or lie after rebalances; we do not rely on it alone.

### Cost

LagProbe polls AdminClient on a short interval (~5s). Negligible at this scale.

### Rejected

| Alternative | Why not |
|-------------|---------|
| Client `records-lag` only | Can freeze / mislead after consumer changes |
| “No lag panel, just logs” | Cannot prove the board keeps up during a burst |

---

## Real-time delivery

### Context

Captains only **receive**. They never submit over the live channel. The UI must update without full reload and without hammering MySQL. The brief allows **one server process**.

### Choice

- **SSE** (`SseEmitter` + browser `EventSource`).
- Snapshot on connect, then live pushes.
- After apply commit → `StandingsSseHub.sendSnapshot(...)` **in-process** (same JVM).
- No `standings-updated` Kafka topic / fan-out consumer.

### Why

With one server, every captain SSE connection lives in that JVM. A direct push after apply is enough, with fewer Kafka parts. Version checks (D1) still protect the UI.

### Cost

**Depends on one process by design.** A second server without a notify bus would leave captains on the other instance stale. Scale-out would reintroduce fan-out (as on `variant/kafka-sse-fanout`).

### Rejected

| Alternative | Why not |
|-------------|---------|
| Kafka `standings-updated` fan-out | Right for N instances; heavier than needed for the one-server floor |
| WebSocket / STOMP | Captains never send; bidirectional is unused surface |
| HTTP polling | DB thrash + built-in staleness |

---

## Auth

### Context

Organisers write; captains only read. Full identity/OAuth is out of scope.

### Choice

- Writes: `X-Organiser-Token` interceptor vs `ORGANISER_TOKEN` env (default in compose: `dev-organiser-token`).
- Reads + SSE: public.
- No Spring Security stack.

### Why

Enough to stop captains writing. Simple to demo and load-test.

### Rejected

Signup, OAuth, per-user roles — out of scope for this board.

---

## What breaks at 10×

| Pressure | What happens |
|----------|----------------|
| One league, 10× match rate | Hot partition (D2) becomes the bottleneck — ordering is the feature and the limit |
| ~2k matches per league | Full recompute (D3) still plausible; far beyond that, revisit incremental **with** stronger invariants |
| Many app instances | Fan-out consumer count grows with instances (by design for SSE) |
| Multi-league noisy neighbour | Separate leagues hash across partitions; one huge league still pins one partition |
