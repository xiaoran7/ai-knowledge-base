# V1 Progress Update - 2026-04-16

## This round

- Added backend task cancellation for document ingestion and summary jobs.
- Added cancellation checkpoints so async document processing can stop after parse, summarize, or index boundaries.
- Added `CANCELED` document/task state handling in the document workspace.
- Upgraded the document task drawer into a dedicated route-level task center with:
  - sidebar entry
  - status filter
  - task summary cards
  - cancel action for running tasks
  - retry action for failed or canceled tasks
  - jump-to-document action that opens the matching document workspace
- Added a standalone retrieval debug workspace that shows:
  - original query
  - rewritten query
  - final used query
  - final/original/rewritten hit comparison
  - chunk type, chunk index, score, and document jump action

## Verified

- `frontend` type check passed with `vue-tsc -b`.
- Playwright browser validation passed for:
  - login
  - sidebar route entry rendering
  - `/document-tasks` page rendering
  - task center to document workspace jump
  - document preview auto-open from route query
- Full `vite build` still could not complete in the current environment because local process spawn was blocked by `spawn EPERM`.
- Backend compile is runnable with the local Maven binary in the machine wrapper directory, but was not executed as a full build in this round.

## Remaining high-value gaps

- Backend cancellation is still checkpoint-based, not a hard interruption of in-flight LLM/http calls.
- Task audit history is still focused on recent records and has no batch operations.
- Retrieval debug now has a standalone workspace, but advanced comparison metrics and non-developer explanations are still thin.
- Function calling remains an extension boundary rather than a user-facing capability.
