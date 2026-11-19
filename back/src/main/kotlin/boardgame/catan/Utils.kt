package boardgame.catan

import boardgame.api.removeElement

val loaclityTypes = listOf(CatanIntersectionObjectType.CITY, CatanIntersectionObjectType.SETTLEMENT)

val developmentCardsCount: Map<CatanDevelopmentCardType, Int> = mapOf(
    CatanDevelopmentCardType.KNIGNT to 14,
    CatanDevelopmentCardType.BUILD_ROADS to 2,
    CatanDevelopmentCardType.MONOPOLY to 2,
    CatanDevelopmentCardType.YEAR_OF_PLENTY to 2,
    CatanDevelopmentCardType.CHAPEL to 1,
    CatanDevelopmentCardType.GREAT_HALL to 1,
    CatanDevelopmentCardType.LIBRARY to 1,
    CatanDevelopmentCardType.MARKET to 1,
    CatanDevelopmentCardType.UNIVERSITY to 1
)

val developmentCardPoints: Map<CatanDevelopmentCardType, Int> = mapOf(
    CatanDevelopmentCardType.KNIGNT to 0,
    CatanDevelopmentCardType.BUILD_ROADS to 0,
    CatanDevelopmentCardType.MONOPOLY to 0,
    CatanDevelopmentCardType.YEAR_OF_PLENTY to 0,
    CatanDevelopmentCardType.CHAPEL to 1,
    CatanDevelopmentCardType.GREAT_HALL to 1,
    CatanDevelopmentCardType.LIBRARY to 1,
    CatanDevelopmentCardType.MARKET to 1,
    CatanDevelopmentCardType.UNIVERSITY to 1
)

val developmentCardSaves: Map<CatanDevelopmentCardType, Boolean> = mapOf(
    CatanDevelopmentCardType.KNIGNT to true,
    CatanDevelopmentCardType.BUILD_ROADS to false,
    CatanDevelopmentCardType.MONOPOLY to false,
    CatanDevelopmentCardType.YEAR_OF_PLENTY to false,
    CatanDevelopmentCardType.CHAPEL to false,
    CatanDevelopmentCardType.GREAT_HALL to false,
    CatanDevelopmentCardType.LIBRARY to false,
    CatanDevelopmentCardType.MARKET to false,
    CatanDevelopmentCardType.UNIVERSITY to false
)

val catanHarbourResourceType: Map<CatanHarbourType, CatanResourceType?> = mapOf(
    CatanHarbourType.THREE_TO_ONE to null,
    CatanHarbourType.CLAY to CatanResourceType.CLAY,
    CatanHarbourType.ORE to CatanResourceType.ORE,
    CatanHarbourType.GRAIN to CatanResourceType.GRAIN,
    CatanHarbourType.WOOL to CatanResourceType.WOOL,
    CatanHarbourType.WOOD to CatanResourceType.WOOD
)

data class CatanBuyItem(val type: CatanBuyItemType, val resources: CatanResources)

fun initResources(values: Map<CatanResourceType, Int> = emptyMap()): CatanResources =
    mutableMapOf<CatanResourceType, Int>().apply {
        CatanResourceType.entries.forEach { put(it, values[it] ?: 0) }
    }

fun initResourcePrices(defaultValue: Int): CatanResourcePrices =
    CatanResourceType.entries.associateWith { defaultValue }

fun resourcesByTypes(vararg types: CatanResourceType): CatanResources {
    val result = initResources()
    types.forEach { result[it] = (result[it] ?: 0) + 1 }
    return result
}

fun getBuyItems(): List<CatanBuyItem> = listOf(
    CatanBuyItem(
        CatanBuyItemType.ROAD,
        initResources(mapOf(CatanResourceType.WOOD to 1, CatanResourceType.CLAY to 1))
    ),
    CatanBuyItem(
        CatanBuyItemType.SETTLEMENT,
        initResources(
            mapOf(
                CatanResourceType.WOOD to 1,
                CatanResourceType.CLAY to 1,
                CatanResourceType.GRAIN to 1,
                CatanResourceType.WOOL to 1
            )
        )
    ),
    CatanBuyItem(
        CatanBuyItemType.CITY,
        initResources(mapOf(CatanResourceType.GRAIN to 2, CatanResourceType.ORE to 3))
    ),
    CatanBuyItem(
        CatanBuyItemType.DEVELOPMENT_CARD,
        initResources(
            mapOf(
                CatanResourceType.GRAIN to 1,
                CatanResourceType.WOOL to 1,
                CatanResourceType.ORE to 1
            )
        )
    )
)

fun getPlayerLocalities(field: CatanField, playerId: String): List<CatanIntersection> =
    field.intersections.filter { intersection ->
        intersection.intersectionObjects.any { it.playerId == playerId && it.type in loaclityTypes }
    }

fun getPlayerPrices(field: CatanField, playerId: String): CatanResourcePrices {
    val result = initResourcePrices(4).toMutableMap()
    getPlayerLocalities(field, playerId).forEach { locality ->
        val harbourPositions = vertexEdges(locality.position)
        val harbours = findByCoords(harbourPositions, field.harbours) { it.position }
        harbours.forEach { harbour ->
            val resourceType = catanHarbourResourceType[harbour.type]
            if (resourceType == null) {
                result.keys.forEach { key -> result[key] = minOf(result[key] ?: 4, 3) }
            } else {
                result[resourceType] = minOf(result[resourceType] ?: 4, 2)
            }
        }
    }
    return result
}

fun getNonNullResourceTypes(resources: CatanResources): List<CatanResourceType> =
    resources.filterValues { it != 0 }.keys.toList()

fun addResources(destination: CatanResources, source: CatanResources) {
    source.forEach { (type, count) -> destination[type] = (destination[type] ?: 0) + count }
}

fun getAllResourcesCount(resources: CatanResources): Int = resources.values.sum()

fun moveAllResourcesByType(destination: CatanResources, source: CatanResources, type: CatanResourceType) {
    destination[type] = (destination[type] ?: 0) + (source[type] ?: 0)
    source[type] = 0
}

fun checkDeal(deal: CatanTradeDeal, resourcePrices: CatanResourcePrices, available: CatanResources): Boolean {
    if (getAllResourcesCount(deal.offered) == 0 || getAllResourcesCount(deal.required) == 0) return false
    if (deal.type != CatanTradeType.BANK) return true

    var allOfferCount = 0
    deal.offered.forEach { (type, offerCount) ->
        if (offerCount == 0) return@forEach
        val price = resourcePrices[type] ?: 4
        if (offerCount % price != 0) return false
        if (offerCount > (available[type] ?: 0)) return false
        allOfferCount += offerCount / price
    }
    return allOfferCount == getAllResourcesCount(deal.required)
}

fun getPlayerIntObjects(playerId: String, field: CatanField): List<CatanIntersectionObject> =
    field.intersections.flatMap { it.intersectionObjects }.filter { it.playerId == playerId }

// ---------------------------------------------------------------- longest road

private class RoadEdge(val road: CatanRoad) {
    val links = mutableListOf<RoadEdge>()
    val intersects = mutableListOf<IntersectNode>()
}

private class IntersectNode(val position: Vector2D) {
    val edges = mutableListOf<RoadEdge>()
    var stopFlag = false
}

private class RoadGraph {
    val roads = mutableListOf<RoadEdge>()
    val intersects = mutableListOf<IntersectNode>()
}

private class RoadPath {
    val intersects = mutableListOf<IntersectNode>()
    val roads = mutableListOf<RoadEdge>()
}

private fun processRoad(
    playerId: String,
    playerRoads: MutableList<CatanRoad>,
    field: CatanField,
    roadNode: RoadEdge,
    part: RoadGraph,
    depth: Int
): RoadGraph {
    playerRoads.removeElement(roadNode.road)
    part.roads += roadNode
    val roadVertices = edgeVertices(roadNode.road.position)
    for (vertex in roadVertices) {
        val intersection = field.intersections.firstOrNull { it.position == vertex }
        val nonPlayerObjects = intersection?.intersectionObjects?.filter { it.playerId != playerId }

        val node = part.intersects.firstOrNull { it.position == vertex }
            ?: IntersectNode(vertex).also {
                it.stopFlag = !nonPlayerObjects.isNullOrEmpty()
                part.intersects += it
            }
        if (node !in roadNode.intersects) roadNode.intersects += node

        if (!nonPlayerObjects.isNullOrEmpty()) continue

        val intersectionRoads = findByCoords(vertexEdges(vertex), playerRoads) { it.position }
        val subRoads = intersectionRoads.map { subRoad ->
            RoadEdge(subRoad).apply {
                links += roadNode
                intersects += node
            }.also { playerRoads.removeElement(subRoad) }
        }
        node.edges += subRoads
        subRoads.forEach { subRoad ->
            subRoad.links += subRoads.filter { it !== subRoad }
            roadNode.links += subRoad
            processRoad(playerId, playerRoads, field, subRoad, part, depth + 1)
        }
    }
    return part
}

private fun getRoadParts(playerId: String, playerRoads: MutableList<CatanRoad>, field: CatanField): List<RoadGraph> {
    val parts = mutableListOf<RoadGraph>()
    val remaining = playerRoads.toMutableList()
    while (remaining.isNotEmpty()) {
        val root = RoadEdge(remaining[0])
        parts += processRoad(playerId, remaining, field, root, RoadGraph(), 0)
    }
    return parts
}

private fun findLongestPathFromNode(
    start: IntersectNode,
    path: RoadPath,
    road: RoadEdge?,
    depth: Int
): RoadPath {
    val pathCopy = RoadPath().apply {
        intersects += path.intersects
        roads += path.roads
    }
    pathCopy.intersects += start
    if (road != null) pathCopy.roads += road
    var longest = pathCopy
    if (start.stopFlag) return longest

    for (edge in start.edges.filter { it !in pathCopy.roads }) {
        val nextIntersects = edge.intersects.filter { it != start }
        if (nextIntersects.isEmpty()) continue
        val next = findLongestPathFromNode(nextIntersects[0], pathCopy, edge, depth + 1)
        if (next.intersects.size > longest.intersects.size) longest = next
    }
    return longest
}

private fun findLongestPath(graph: RoadGraph): RoadPath {
    var longest = RoadPath()
    graph.intersects.forEach { start ->
        val path = findLongestPathFromNode(start, RoadPath(), null, 0)
        if (path.roads.size > longest.roads.size) longest = path
    }
    return longest
}

fun findLongestRoad(players: List<boardgame.api.Player>, field: CatanField, minLength: Int): List<CatanRoad> {
    var longest = RoadPath()
    players.forEach { player ->
        val playerRoads = field.roads.filter { it.playerId == player.userId }
        if (playerRoads.size < minLength) return@forEach
        getRoadParts(player.userId, playerRoads.toMutableList(), field).forEach { part ->
            val path = findLongestPath(part)
            if (path.intersects.size > longest.intersects.size) longest = path
        }
    }
    return longest.roads.map { it.road }
}

fun getAvailableBuyItems(playerId: String, field: CatanField): Map<CatanBuyItemType, Int?> {
    val result = mutableMapOf<CatanBuyItemType, Int?>()
    val intObjects = getPlayerIntObjects(playerId, field)
    result[CatanBuyItemType.ROAD] = 15 - field.roads.count { it.playerId == playerId }
    result[CatanBuyItemType.SETTLEMENT] =
        5 - intObjects.count { it.type == CatanIntersectionObjectType.SETTLEMENT }
    result[CatanBuyItemType.CITY] = 4 - intObjects.count { it.type == CatanIntersectionObjectType.CITY }
    result[CatanBuyItemType.DEVELOPMENT_CARD] = null
    return result
}
