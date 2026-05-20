## Changes
<!--
Describe the changes that are made in this pull request.
-->



<!--
Checklist for completeness and clarity of the PR
-->
<details>
<summary>
<strong>Pull Request Checklist</strong>
</summary>
  
### Title
<!--
Goal: Make the value obvious to readers at a glance. Prefer the benefit over the implementation.
Example good: "Reduce adapter startup time ~30% by caching configuration"
Example bad: "Refactor AdapterManager"
-->
- [ ] Title expresses the business value (who benefits + what outcome)

### Issues
<!--
Link all relevant issues so they auto-close on merge and remain traceable.
Select issues in sidebar or use: Closes/Fixes/Resolves #123
-->
- [ ] Relevant issue(s) linked
- [ ] Confirmed with the issue creator(s) that the solution is satisfactory

### Backports
<!--
If this needs to land in supported release branches, open separate PRs and link them here.
Example: release/9.x, release/10.x
-->
- [ ] Backport PRs created (if needed) and linked

### Documentation
<!--
Keep user and developer docs in sync with the change.
Update where applicable: FF!Reference, Javadoc and if applicable the docs on [docs.frankframework.org](https://docs.frankframework.org/)
-->
- [ ] FF!Reference updated (user-facing behaviour/config)
- [ ] Javadoc updated/generated (developer-facing APIs)
- [ ] Frank!Docs updated (if applicable)

### Tests
<!--
Changes should be covered so behaviour is verified and regressions are prevented.
Add or update both unit and end-to-end/integration tests as needed.
-->
- [ ] Unit tests added/updated
- [ ] E2E/Integration tests added/updated (if applicable)

### Breaking changes
<!--
If behaviour or public APIs change in a non-backward-compatible way:
- Document in the repository’s breaking-changes BREAKING.md 
- Provide brief migration notes
-->
- [ ] Breaking change recorded in markdown file
- [ ] Migration notes included (if needed)
</details>
