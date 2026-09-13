# Contributing

RemitMind is developed solo, but run through a full SDLC on GitHub as if it were a small team — the "team" is one person rotating through explicit roles (`role:product-owner`, `role:architect`, `role:backend`, `role:security`, `role:qa`, `role:devops`, `role:tech-writer`), tracked via issue labels. This file documents the conventions so the process stays legible from the outside, and so it doesn't quietly decay back into ad hoc commits.

## Backlog & board

- Work is tracked as GitHub Issues: `type:epic` (a themed body of work), `type:story` (a sprint-sized, independently valuable slice with Given/When/Then acceptance criteria), `type:spike` (a time-boxed investigation), `type:chore`, `bug`.
- Sprints are GitHub Milestones (soft-dated — closed when done, not when a date passes, since there's no fixed deadline).
- Releases are GitHub Releases (`vX.Y.Z`), cut when a milestone's increment is demoable, with a changelog.
- The board (GitHub Projects) is Kanban-style: `Backlog → Ready → In Progress → In Review → Done`.

## Branching & PRs

- `main` is protected: changes land via PR, not direct push, and CI must be green.
- Branch names: `feat/EPIC-N-short-slug`, `fix/short-slug`, `chore/short-slug`.
- Every PR links its issue (`Closes #NNN`) and states which role was worn for the change.
- Definition of Done (see PR template): tests pass, CI green, acceptance criteria checked off, docs updated where relevant, no new secrets/PII, no new critical CodeQL/Dependabot alerts.

## Architecture decisions

Non-obvious architectural choices get an ADR in `docs/adr/` (see `docs/adr/0001-record-architecture-decisions.md` for the format) — before or alongside the PR that implements them, not after the fact.

## Docs split

`docs/adr/`, `docs/architecture.md`, `docs/api.md`, and `docs/research/` are tracked product docs. `docs/learning/` (the personal Spring AI learning journal) and the root-level spec/roadmap files stay gitignored and local — that's deliberate, not an oversight.

## Local setup

See `README.md` for running the app. `GEMINI_API_KEY` and/or a local Ollama install are needed for the AI-calling paths — see `docs/OLLAMA_SETUP.md` locally (gitignored, ask if you need it reproduced) for the Ollama route.
