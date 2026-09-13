# Contributing

RemitMind is developed solo, but run through a full SDLC on GitHub as if it were a small team — the "team" is one person rotating through explicit roles (`role:product-owner`, `role:architect`, `role:backend`, `role:security`, `role:qa`, `role:devops`, `role:tech-writer`), tracked via issue labels. This file documents the conventions so the process stays legible from the outside, and so it doesn't quietly decay back into ad hoc commits.

## Backlog & board

- Work is tracked as GitHub Issues: `type:epic` (a themed body of work), `type:story` (a sprint-sized, independently valuable slice with Given/When/Then acceptance criteria), `type:spike` (a time-boxed investigation), `type:chore`, `bug`.
- Sprints are GitHub Milestones (soft-dated — closed when done, not when a date passes, since there's no fixed deadline).
- Releases are GitHub Releases (`vX.Y.Z`), cut when a milestone's increment is demoable, with a changelog.
- The board ([RemitMind Roadmap](https://github.com/users/faizalzafri/projects/6)) is Kanban-style: `Backlog → Ready → In Progress → In Review → Done`.

## Branching & PRs

- `main` is protected via a repository ruleset: changes land via PR (0 required approvals — solo project), not direct push, and force-push/deletion are blocked.
- Branch names: `feat/EPIC-N-short-slug`, `fix/short-slug`, `chore/short-slug`.
- Every PR links its issue (`Closes #NNN`) and states which role was worn for the change.
- Definition of Done (see PR template): tests pass, CI green, acceptance criteria checked off, docs updated where relevant, no new secrets/PII, no new critical CodeQL/Dependabot alerts.

## Local setup

See `README.md` for running the app. `GEMINI_API_KEY` and/or a local Ollama install are needed for the AI-calling paths.
