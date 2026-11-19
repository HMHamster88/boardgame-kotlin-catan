import {
    findByCoordsArray,
    getVertexEdgesPositions
} from "boardgame-web-common/back"
import {
    catanHarbourResourceType,
    initResourcePrices,
    initResources,
    maxBuyItem,
    CatanBuyItemType,
    CatanIntersectionObjectType,
    CatanResourceType,
    CatanTradeType,
    type CatanResourcePrices,
    type CatanResources,
    type CatanTradeDeal
} from "./types.js"
import type { CatanField } from "./generated/types.js"

export const loaclityTypes = [CatanIntersectionObjectType.CITY, CatanIntersectionObjectType.SETTLEMENT]

export function getPlayerLocalities(field: CatanField, playerId: string) {
    return field.intersections.filter((int) =>
        int.intersectionObjects.some((obj) => obj.playerId === playerId && loaclityTypes.includes(obj.type))
    )
}

export function getPlayerPrices(field: CatanField, playerId: string): CatanResourcePrices {
    const result = initResourcePrices({}, 4)
    const locals = getPlayerLocalities(field, playerId)
    for (const local of locals) {
        const harbourPositions = getVertexEdgesPositions(local.position)
        const harbours = findByCoordsArray(harbourPositions, field.harbours)
        for (const harbour of harbours) {
            const resourceType = catanHarbourResourceType[harbour.type]
            if (!resourceType) {
                Object.values(CatanResourceType).forEach((key) => {
                    result[key] = Math.min(result[key], 3)
                })
            } else {
                result[resourceType] = Math.min(result[resourceType], 2)
            }
        }
    }
    return result
}

export function getAllResourcesCount(resources: CatanResources): number {
    return Object.values(resources).reduce((total, count) => total + count, 0)
}

export function addResources(destination: CatanResources, source: CatanResources) {
    for (const [type, count] of Object.entries(source)) {
        destination[type as CatanResourceType] += count
    }
}

export function getNonNullResourceTypes(resources: CatanResources): CatanResourceType[] {
    return (Object.entries(resources) as [CatanResourceType, number][])
        .filter(([, count]) => count !== 0)
        .map(([type]) => type)
}

export function resourcesByTypes(...resourceTypes: CatanResourceType[]): CatanResources {
    const result = initResources()
    resourceTypes.forEach((type) => result[type]++)
    return result
}

export function checkDeal(
    deal: CatanTradeDeal,
    resourcePrices: CatanResourcePrices,
    availableResources: CatanResources
): boolean {
    if (getAllResourcesCount(deal.offered) === 0 || getAllResourcesCount(deal.required) === 0) return false
    if (deal.type === CatanTradeType.BANK) {
        let allOfferCount = 0
        for (const [offerKey, offerCount] of Object.entries(deal.offered) as [CatanResourceType, number][]) {
            const price = resourcePrices[offerKey]
            if (offerCount % price !== 0) return false
            if (offerCount > 0) {
                const available = availableResources[offerKey]
                if (!available || offerCount > available) return false
                allOfferCount += offerCount / price
            }
        }
        return allOfferCount === getAllResourcesCount(deal.required)
    }
    return true
}

export function getAvailableBuyItems(
    playerId: string,
    field: CatanField
): Record<CatanBuyItemType, number | undefined> {
    const result = {} as Record<CatanBuyItemType, number | undefined>
    const playerObjects = field.intersections.flatMap((int) => int.intersectionObjects)
        .filter((obj) => obj.playerId === playerId)
    Object.entries(maxBuyItem).forEach(([key, value]) => {
        const type = key as CatanBuyItemType
        if (type === CatanBuyItemType.ROAD) {
            result[type] = (value as number) - field.roads.filter((road) => road.playerId === playerId).length
        } else if (type === CatanBuyItemType.SETTLEMENT) {
            result[type] = (value as number) -
                playerObjects.filter((obj) => obj.type === CatanIntersectionObjectType.SETTLEMENT).length
        } else if (type === CatanBuyItemType.CITY) {
            result[type] = (value as number) -
                playerObjects.filter((obj) => obj.type === CatanIntersectionObjectType.CITY).length
        } else {
            result[type] = undefined
        }
    })
    return result
}
