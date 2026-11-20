package boardgame.catan

import boardgame.api.codegen.GenerateTs
import kotlinx.serialization.Serializable

const val CATAN_TYPE = "CATAN"

@GenerateTs
@Serializable
data class Vector2D(val x: Int = 0, val y: Int = 0) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun times(multiplier: Int) = Vector2D(x * multiplier, y * multiplier)

    companion object {
        fun equals(left: Vector2D?, right: Vector2D?): Boolean = left == right
    }
}

@GenerateTs
@Serializable
enum class CatanGamePhase {
    EMBARK_FIRST,
    EMBARK_SECOND,
    THROWING_DICE,
    PLAYER_TURN,
    DISCARD_CARDS_7,
    MOVE_ROBBER
}

@GenerateTs
@Serializable
enum class CatanResourceType {
    WOOD,
    WOOL,
    GRAIN,
    CLAY,
    ORE
}

@GenerateTs
@Serializable
enum class CatanDevelopmentCardType {
    KNIGNT,
    BUILD_ROADS,
    MONOPOLY,
    YEAR_OF_PLENTY,
    CHAPEL,
    GREAT_HALL,
    LIBRARY,
    MARKET,
    UNIVERSITY
}

@GenerateTs
@Serializable
enum class CatanHarbourType {
    THREE_TO_ONE,
    CLAY,
    ORE,
    GRAIN,
    WOOL,
    WOOD
}

@GenerateTs
@Serializable
enum class CatanIntersectionObjectType {
    SETTLEMENT,
    CITY
}

@GenerateTs
@Serializable
enum class CatanSpecialCard {
    BIGGEST_ARMY,
    LONGEST_ROAD
}

@GenerateTs
@Serializable
enum class CatanTradeType {
    BANK,
    PLAYER
}

@GenerateTs
@Serializable
enum class CatanBuyItemType {
    ROAD,
    SETTLEMENT,
    CITY,
    DEVELOPMENT_CARD
}

@GenerateTs
@Serializable
enum class CatanGameFieldType {
    CLASSIC,
    EXTENDED
}

@GenerateTs
@Serializable
enum class CatanTerrainHexType {
    HILLS,
    FOREST,
    MOUNTAINS,
    FIELDS,
    PASTURE,
    DESERT
}

@GenerateTs
@Serializable
enum class TradeAnswerState {
    WAITING_FOR_ANSWER,
    ACCEPTED,
    REJECTED
}

typealias CatanResources = MutableMap<CatanResourceType, Int>
typealias CatanResourcePrices = Map<CatanResourceType, Int>

@GenerateTs
@Serializable
data class CatanTerrainHex(
    val position: Vector2D = Vector2D(),
    val type: CatanTerrainHexType = CatanTerrainHexType.DESERT,
    val circularNumber: Int = 0
)

@GenerateTs
@Serializable
data class CatanRoad(
    val position: Vector2D = Vector2D(),
    val playerId: String = ""
)

@GenerateTs
@Serializable
data class CatanHarbour(
    val position: Vector2D = Vector2D(),
    val type: CatanHarbourType = CatanHarbourType.THREE_TO_ONE
)

@GenerateTs
@Serializable
data class CatanIntersectionObject(
    val playerId: String = "",
    var type: CatanIntersectionObjectType = CatanIntersectionObjectType.SETTLEMENT
)

@GenerateTs
@Serializable
data class CatanIntersection(
    val position: Vector2D = Vector2D(),
    val intersectionObjects: MutableList<CatanIntersectionObject> = mutableListOf()
)

@GenerateTs
@Serializable
data class CatanField(
    var width: Int = 0,
    var height: Int = 0,
    var hexes: MutableList<CatanTerrainHex> = mutableListOf(),
    var harbours: MutableList<CatanHarbour> = mutableListOf(),
    var roads: MutableList<CatanRoad> = mutableListOf(),
    var intersections: MutableList<CatanIntersection> = mutableListOf(),
    var robberPos: Vector2D = Vector2D()
)

@GenerateTs
@Serializable
data class CatanFieldGenerationSettings(
    var fieldType: CatanGameFieldType = CatanGameFieldType.CLASSIC,
    var spreadHexTypes: Boolean = true,
    var spreadCircularNumbers: Boolean = true
)

@GenerateTs
@Serializable
data class CatanGameSettings(
    val id: String = "",
    var fieldGenerationSettings: CatanFieldGenerationSettings = CatanFieldGenerationSettings(),
    var field: CatanField = CatanField(),
    var maxPoints: Int = 10,
    var maxResourceCount: Int = 7,
    var resourcesForEachEvenSettlement: Boolean = true,
    var resourceForRobberOnVacantHex: Boolean = true
)

@GenerateTs
@Serializable
data class CatanPlayerPublicState(
    val playerId: String = "",
    var points: Int = 0,
    var openedDevelopmentCards: MutableList<CatanDevelopmentCardType> = mutableListOf(),
    var specialCards: MutableList<CatanSpecialCard> = mutableListOf(),
    var resourceCount: Int = 0
)

@GenerateTs
@Serializable
data class CatanDices(
    var redDice: Int = 1,
    var yellowDice: Int = 1
)

@GenerateTs
@Serializable
data class TradeAnswer(
    val playerId: String = "",
    var state: TradeAnswerState = TradeAnswerState.WAITING_FOR_ANSWER
)

@GenerateTs
@Serializable
data class CatanPlayerTradeOffer(
    val playerId: String = "",
    val offered: CatanResources = initResources(),
    val required: CatanResources = initResources(),
    val answers: MutableList<TradeAnswer> = mutableListOf()
)

@GenerateTs
@Serializable
data class CatanPlayerStatistics(
    val playerId: String = "",
    var points: Int = 0,
    /** Hidden victory-point cards; only populated once the game is finished. */
    var privatePoints: Int? = null
)

@GenerateTs
@Serializable
data class CatanGameStatistics(
    var turnCount: Int = 0,
    var resourcesReceived: CatanResources = initResources(),
    var diceNumbersRolled: MutableList<Int> = mutableListOf(),
    var playersStatistics: MutableList<CatanPlayerStatistics> = mutableListOf()
)

@GenerateTs
@Serializable
data class CatanPublicGameState(
    var field: CatanField = CatanField(),
    var phase: CatanGamePhase = CatanGamePhase.EMBARK_FIRST,
    var activePlayerIndex: Int = 0,
    var winnersIds: MutableList<String> = mutableListOf(),
    var playersStates: MutableList<CatanPlayerPublicState> = mutableListOf(),
    var dices: CatanDices = CatanDices(),
    var playerThrowedDices: Boolean = false,
    var playerTradeOffer: CatanPlayerTradeOffer? = null,
    var longestRoad: MutableList<CatanRoad> = mutableListOf(),
    var statistics: CatanGameStatistics = CatanGameStatistics(),
    var onHandResources: CatanResources = initResources()
)

@GenerateTs
@Serializable
data class CatanPlayerPrivateState(
    val playerId: String = "",
    var resources: CatanResources = initResources(),
    var developmentCards: MutableList<CatanDevelopmentCardType> = mutableListOf(),
    var discardCardsCount: Int = 0,
    var freeBuildings: MutableList<CatanBuyItemType> = mutableListOf()
)

@GenerateTs
@Serializable
data class CatanPrivateGameState(
    var playersStates: MutableList<CatanPlayerPrivateState> = mutableListOf(),
    var developmentCardsDeck: MutableList<CatanDevelopmentCardType> = mutableListOf(),
    var developmentCardDiscardPile: MutableList<CatanDevelopmentCardType> = mutableListOf()
)

@GenerateTs
@Serializable
data class CatanTradeDeal(
    val type: CatanTradeType = CatanTradeType.BANK,
    val offered: CatanResources = initResources(),
    val required: CatanResources = initResources()
)

// ---------------------------------------------------------------- actions

@GenerateTs(typeLiteral = "CatanGenerateFieldAction")
@Serializable
data class CatanGenerateFieldAction(val type: String = "CatanGenerateFieldAction")

@GenerateTs(typeLiteral = "CatanEmbarkAction")
@Serializable
data class CatanEmbarkAction(
    val type: String = "CatanEmbarkAction",
    val settlement: Vector2D = Vector2D(),
    val road: Vector2D = Vector2D()
)

@GenerateTs(typeLiteral = "CatanRollDicesAction")
@Serializable
data class CatanRollDicesAction(val type: String = "CatanRollDicesAction")

@GenerateTs(typeLiteral = "CatanBuildIntObjectAction")
@Serializable
data class CatanBuildIntObjectAction(
    val type: String = "CatanBuildIntObjectAction",
    val objectType: CatanIntersectionObjectType = CatanIntersectionObjectType.SETTLEMENT,
    val position: Vector2D = Vector2D()
)

@GenerateTs(typeLiteral = "CatanBuildRoadAction")
@Serializable
data class CatanBuildRoadAction(
    val type: String = "CatanBuildRoadAction",
    val position: Vector2D = Vector2D()
)

@GenerateTs(typeLiteral = "CatanBuyDevelopmentCardAction")
@Serializable
data class CatanBuyDevelopmentCardAction(val type: String = "CatanBuyDevelopmentCardAction")

@GenerateTs(typeLiteral = "CatanUseDevelopmentCardAction")
@Serializable
data class CatanUseDevelopmentCardAction(
    val type: String = "CatanUseDevelopmentCardAction",
    val developmentCard: CatanDevelopmentCardType = CatanDevelopmentCardType.KNIGNT
)

@GenerateTs(typeLiteral = "CatanUseDevelopmentCardAction")
@Serializable
data class CatanUseResourceDevelopmentCardAction(
    val type: String = "CatanUseDevelopmentCardAction",
    val developmentCard: CatanDevelopmentCardType = CatanDevelopmentCardType.YEAR_OF_PLENTY,
    val resources: CatanResources = initResources()
)

@GenerateTs(typeLiteral = "CatanUseDevelopmentCardAction")
@Serializable
data class CatanUseResourceTypeDevelopmentCardAction(
    val type: String = "CatanUseDevelopmentCardAction",
    val developmentCard: CatanDevelopmentCardType = CatanDevelopmentCardType.MONOPOLY,
    val resourcesType: CatanResourceType = CatanResourceType.WOOD
)

@GenerateTs(typeLiteral = "CatanEndTurnAction")
@Serializable
data class CatanEndTurnAction(val type: String = "CatanEndTurnAction")

@GenerateTs(typeLiteral = "CatanDiscardResourceCards")
@Serializable
data class CatanDiscardResourceCards(
    val type: String = "CatanDiscardResourceCards",
    val resources: CatanResources = initResources()
)

@GenerateTs(typeLiteral = "CatanMoveRobberAction")
@Serializable
data class CatanMoveRobberAction(
    val type: String = "CatanMoveRobberAction",
    val position: Vector2D = Vector2D(),
    val playerToRob: String? = null
)

@GenerateTs(typeLiteral = "CatanTradeAction")
@Serializable
data class CatanTradeAction(
    val type: String = "CatanTradeAction",
    val deal: CatanTradeDeal = CatanTradeDeal()
)

@GenerateTs(typeLiteral = "CatanTradeResponseAction")
@Serializable
data class CatanTradeResponseAction(
    val type: String = "CatanTradeResponseAction",
    val accepted: Boolean = false
)

@GenerateTs(typeLiteral = "CatanTradeCloseAction")
@Serializable
data class CatanTradeCloseAction(
    val type: String = "CatanTradeCloseAction",
    val playerId: String? = null
)
