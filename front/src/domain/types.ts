export * from "./generated/types.js"

import {
    CatanBuyItemType,
    type CatanDevelopmentCardType,
    CatanGamePhase,
    CatanHarbourType,
    type CatanIntersectionObjectType,
    CatanResourceType
} from "./generated/types.js"

/** Resource counts: every resource type is present (initialised with 0). */
export type CatanResources = Record<CatanResourceType, number>

/** How many resources one unit costs at the bank, per resource type. */
export type CatanResourcePrices = Record<CatanResourceType, number>

/** Structural position type used by the existing front components. */
export interface Vector2DLike {
    x: number
    y: number
}

/** The wire keeps dice values as numbers (0 = not rolled yet). */
export enum CatanDiceValue {
    NONE = 0,
    ONE = 1,
    TWO = 2,
    THREE = 3,
    FOUR = 4,
    FIVE = 5,
    SIX = 6
}

export interface CatanBuyItem {
    type: CatanBuyItemType
    resources: CatanResources
}

export const catanEmbarkPhases: readonly CatanGamePhase[] = [
    CatanGamePhase.EMBARK_FIRST,
    CatanGamePhase.EMBARK_SECOND
]

export const catanHarbourResourceType: Record<CatanHarbourType, CatanResourceType | undefined> = {
    [CatanHarbourType.THREE_TO_ONE]: undefined,
    [CatanHarbourType.CLAY]: CatanResourceType.CLAY,
    [CatanHarbourType.ORE]: CatanResourceType.ORE,
    [CatanHarbourType.GRAIN]: CatanResourceType.GRAIN,
    [CatanHarbourType.WOOL]: CatanResourceType.WOOL,
    [CatanHarbourType.WOOD]: CatanResourceType.WOOD
}

export const developmentCardIsUsable: Record<CatanDevelopmentCardType, boolean> = {
    KNIGNT: true,
    BUILD_ROADS: true,
    MONOPOLY: true,
    YEAR_OF_PLENTY: true,
    CHAPEL: false,
    GREAT_HALL: false,
    LIBRARY: false,
    MARKET: false,
    UNIVERSITY: false
}

export const maxBuyItem: Record<CatanBuyItemType, number | undefined> = {
    [CatanBuyItemType.ROAD]: 15,
    [CatanBuyItemType.DEVELOPMENT_CARD]: undefined,
    [CatanBuyItemType.SETTLEMENT]: 5,
    [CatanBuyItemType.CITY]: 4
}

export function initResources(intVal: Partial<Record<CatanResourceType, number>> = {}): CatanResources {
    const result = {} as CatanResources
    Object.values(CatanResourceType).forEach((type) => {
        result[type] = intVal[type] ?? 0
    })
    return result
}

export function initResourcePrices(
    intVal: Partial<Record<CatanResourceType, number>> = {},
    defaultValue = 4
): CatanResourcePrices {
    const result = {} as CatanResourcePrices
    Object.values(CatanResourceType).forEach((type) => {
        result[type] = intVal[type] ?? defaultValue
    })
    return result
}

export function getBuyItems(): CatanBuyItem[] {
    return [
        {
            type: CatanBuyItemType.ROAD,
            resources: initResources({ [CatanResourceType.WOOD]: 1, [CatanResourceType.CLAY]: 1 })
        },
        {
            type: CatanBuyItemType.SETTLEMENT,
            resources: initResources({
                [CatanResourceType.WOOD]: 1,
                [CatanResourceType.CLAY]: 1,
                [CatanResourceType.GRAIN]: 1,
                [CatanResourceType.WOOL]: 1
            })
        },
        {
            type: CatanBuyItemType.CITY,
            resources: initResources({ [CatanResourceType.GRAIN]: 2, [CatanResourceType.ORE]: 3 })
        },
        {
            type: CatanBuyItemType.DEVELOPMENT_CARD,
            resources: initResources({
                [CatanResourceType.GRAIN]: 1,
                [CatanResourceType.WOOL]: 1,
                [CatanResourceType.ORE]: 1
            })
        }
    ]
}

export function buyItemToIntersectionObject(item: CatanBuyItemType): CatanIntersectionObjectType | undefined {
    if (item === CatanBuyItemType.ROAD || item === CatanBuyItemType.DEVELOPMENT_CARD) return undefined
    return item as unknown as CatanIntersectionObjectType
}

export function intersectionObjectRoBuyItem(item: CatanIntersectionObjectType): CatanBuyItemType | undefined {
    return item as unknown as CatanBuyItemType
}
