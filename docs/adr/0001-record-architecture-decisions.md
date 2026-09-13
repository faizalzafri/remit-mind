# 1. Record architecture decisions

## Status
Accepted

## Context
RemitMind is moving from a solo learning project to a portfolio-grade product with a full SDLC. Decisions like "compute the compliance decision in Java instead of the model," "self-host yente instead of using the hosted OpenSanctions API," or "keep the vector store in-memory" need a durable record of *why*, not just what — otherwise future work (or a future reader evaluating this repo) has to reverse-engineer intent from code and commit messages.

## Decision
We will use lightweight Architecture Decision Records (ADRs), one per significant decision, stored in `docs/adr/` and tracked in git (unlike `docs/learning/`, which stays a private, gitignored journal). Each ADR follows this template:

```
# N. Title

## Status
Proposed | Accepted | Superseded by ADR-M

## Context
What problem or force led to this decision.

## Decision
What we're doing.

## Consequences
What becomes easier or harder as a result.
```

New ADRs are numbered sequentially and never renumbered; a reversed decision gets a new ADR that supersedes the old one rather than an edit in place.

## Consequences
- Every non-obvious architectural choice in EPIC-2 onward (deterministic decision engine, yente integration, persistence, corridor-data provenance) gets an ADR before or alongside the implementing PR.
- Reviewers (including a future employer or collaborator browsing this public repo) can see the reasoning, not just the diff.
