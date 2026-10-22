# Contributing

## Branches
`master` is protected: no direct pushes, no force pushes, no deletion. Every change goes through a pull request.

Branch names must start with one of these prefixes. GitHub rejects any other name.

| Prefix | Use for |
|---|---|
| `feature/` | new functionality |
| `fix/` | bug fixes |
| `docs/` | documentation |
| `chore/` | build, config, dependencies |
| `refactor/` | code changes without behaviour change |
| `test/` | tests only |

Examples: `feature/merge-items`, `docs/contributing`.

Branches made by GitHub's **Revert** button (`revert-*`) are also allowed.

## Commits
Format: `type: short description` ([Conventional Commits](https://www.conventionalcommits.org)).

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test` (same as the branch prefixes, but `feat` instead of `feature`).

Examples: `feat: merge two items via REST`, `docs: add contributing guide`.

## Pull requests
1. Branch off `master`: `git switch -c feature/my-thing`.
2. Push the branch and open a PR into `master`.
3. Resolve all review conversations.
4. Merge it yourself (any merge method, no approval required).
5. Delete the branch from the PR page after merging.

Not a collaborator? Fork the repo and open a PR; a maintainer will merge it.
