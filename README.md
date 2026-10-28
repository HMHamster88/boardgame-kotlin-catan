# boardgame-kotlin-catan

Kotlin Catan module and its Vue front.

## Layout

```
back/    :back    Kotlin game plugin (rules, state, bot actions)
front/            Vue Module Federation remote (copied from boardgame-web-catan/front)
```

## Status

Scaffold: `front/` copied; the Kotlin module is not created yet and the front does not compile until
the domain extraction below is done.

## Front copy TODO

The front imports a large slice of domain code from the old TS `catan-back`: types/enums,
`initResources`, `initResourcePrices`, `checkDeal`, `getAllResourcesCount`, `resourcesByTypes`,
`developmentCardIsUsable`, `TradeAnswerState`. Extract those into `front/src/domain/` first.

`front/package.json` now depends on `boardgame-web-common` via `file:../../core/common`.
