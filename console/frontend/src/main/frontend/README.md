# FF! Console GUI

The Angular frontend for the Frank!Framework management console.

## Prerequisites

- **Node.js**
- **pnpm** (run `corepack enable` to use it)

Install dependencies:

```bash
pnpm install
```

## Development server

Start a local dev server with hot reload:

```bash
pnpm start          # ng serve
pnpm start:example  # ng serve against the FF! Example backend
pnpm start:test     # ng serve against the FF! Test backend
```

## Building

```bash
pnpm build            # production build
pnpm watch            # development build, rebuilds on change
pnpm build:analysis   # build with bundle stats for analysis
```

### Bundle analysis

After running `pnpm build:analysis`, upload `target/frontend/stats.json` to
[esbuild analyze](https://esbuild.github.io/analyze/) to inspect the bundle.

## Testing

Unit tests run with [Vitest](https://vitest.dev/):

```bash
pnpm test       # watch mode
pnpm test:ci    # single run (CI)
```

End-to-end tests:

```bash
pnpm e2e        # run e2e tests
pnpm e2e:ci     # run e2e tests (CI configuration)
```

### Cypress

```bash
pnpm cypress:open   # open the Cypress test runner
pnpm cypress:run    # run Cypress tests headless
pnpm cypress:ci     # run Cypress against the iaf-test gui
```

## Linting

```bash
pnpm lint   # ng lint (ESLint + Prettier)
```
