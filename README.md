# boardgame-kotlin-catan

Kotlin Catan module and its Vue front.

## Layout

```
back/    :back    Kotlin game plugin (rules, state, bot actions)
front/            Vue Module Federation remote (copied from boardgame-web-catan/front)
```

## Status

Implemented: `back/` — field generation (classic/extended), embark, dice and resource production,
robber, building roads/settlements/cities, bank/player trades, development cards, largest army and
longest road, points and winners. The dice roll publishes a zero-dice frame and waits 1 s before the
result (`CatanGameBackService(diceAnimationMs = …)` overrides it), matching the TS animation. Tests
cover field generation, state start and the first turns.

Bots are not implemented yet (`nextBotAction` returns null, the front disables "Add bot").

Verified: `:back:test`, the release asset, and a two-player launcher smoke test (start, four embarks,
roll, end turn). The front builds against generated Kotlin types.

## Build / test

```bash
gradle :back:test
gradle :back:jar        # -> back/build/libs/back-<version>.jar
```

## Development build

```bash
gradle devBuild         # :back:generateTs + npm run build-front + :back:jar
gradle devFront         # front only (-> front/dist)
```

The launcher's `DEV=true` watcher reloads the module when `back/build/libs/back-<version>.jar`
changes and copies `front/dist` for the front. Requires `npm` on `PATH`.

IntelliJ IDEA: the shared **Build Catan (dev)** run configuration (`.idea/runConfigurations`) runs
`devBuild`.

## Version

The version is resolved in this order:

1. `-PcatanVersion=…`
2. `RELEASE_VERSION` environment variable
3. the latest reachable git tag (`v0.2.0` → `0.2.0`)
4. default `0.1.0`

Tag the release before building; the **Build Catan release** configuration then produces
`catan-0.2.0.zip` and `back-0.2.0.jar` with `version.properties = 0.2.0`. Pass
`-PcatanVersion=…` in the run configuration's Arguments field for a one-off override.

## Release packaging

```bash
gradle releaseBuild     # :devFront + :back:releaseZip -> back/build/dist/catan-<version>.zip
gradle :back:releaseZip # same, but assumes front/dist is already built
```

The asset is `back/catan.jar` (classes + `classes.dex` + resources/service) and `front/**`, versioned
in the file name (`catan-<version>.zip`). It installs as `games-modules/catan/<version>/`, so a new
release does not overwrite the running one and the core loads the newest version.

IntelliJ IDEA: the shared **Build Catan release** run configuration runs `releaseBuild`.

## TypeScript model codegen

```bash
gradle :back:generateTs   # -> front/src/domain/generated/types.ts (from Kotlin models)
```

Models are discovered via `@GenerateTs` on the Kotlin classes/enums (add `name`/`typeLiteral` when the
TS name or discriminator differs); the task scans compiled classes, so there is no manual list.

`front/src/domain/types.ts` re-exports the generated types (including string enums) and keeps the
front-only helpers: numeric `CatanDiceValue`, resource/buy helpers and price checks.
`front/src/domain/utils.ts` has the price/deal/available-item helpers used by the views.

After changing Kotlin models:

```bash
gradle :back:generateTs
git diff --exit-code front/src/domain/generated/types.ts   # CI guard
npm run build-front
```

`CatanDiceValue` stays numeric on the wire (`0..6`); all other Catan enums are strings and are
generated from the Kotlin enums.

The front depends on `boardgame-web-common` via `file:../../core/common`.
