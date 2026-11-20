package boardgame.catan

import boardgame.api.Game
import boardgame.api.GameStatus
import boardgame.api.Player
import boardgame.api.decodeTree
import boardgame.api.nowIso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatanServiceTest {

    private val service = CatanGameBackService()

    private fun game(playerCount: Int = 3): Game = Game(
        id = "game-1",
        owner = "p1",
        name = "test",
        type = CATAN_TYPE,
        players = (1..playerCount).map {
            Player(userId = "p$it", name = "Player $it", color = "#FF0000", online = true, isBot = false)
        }.toMutableList(),
        created = nowIso()
    )

    @Test
    fun classicFieldHasExpectedShape() {
        val settings = decodeTree<CatanGameSettings>(service.getDefaultSettings())
        val field = settings.field

        assertEquals(19, field.hexes.size)
        assertEquals(9, field.harbours.size)
        assertEquals(1, field.hexes.count { it.type == CatanTerrainHexType.DESERT })
        assertEquals(
            field.robberPos,
            field.hexes.first { it.type == CatanTerrainHexType.DESERT }.position
        )
        assertEquals(18, field.hexes.count { it.circularNumber != 0 })
        assertEquals(4, field.hexes.count { it.circularNumber == 6 || it.circularNumber == 8 })
    }

    @Test
    fun startGameInitialisesPublicAndPrivateState() {
        val game = game()
        val payload = service.startGame(game, service.getDefaultSettings())
        val publicState = decodeTree<CatanPublicGameState>(payload.publicState)
        val privateState = decodeTree<CatanPrivateGameState>(assertNotNull(payload.privateState))

        assertEquals(GameStatus.STARTED, game.status)
        assertEquals(CatanGamePhase.EMBARK_FIRST, publicState.phase)
        assertEquals(3, publicState.playersStates.size)
        assertEquals(3, privateState.playersStates.size)
        assertEquals(25, privateState.developmentCardsDeck.size)
        assertTrue(publicState.activePlayerIndex in 0..2)
        assertTrue(publicState.playersStates.all { it.points == 0 })
        assertEquals(3, publicState.statistics.playersStatistics.size)
        assertTrue(publicState.statistics.playersStatistics.all {
            it.points == 0 &&
                it.settlements == 0 &&
                it.settlementPoints == 0 &&
                it.cities == 0 &&
                it.cityPoints == 0 &&
                it.longestRoadPoints == 0 &&
                it.biggestArmyPoints == 0 &&
                it.privatePoints == null
        })
        assertTrue(privateState.playersStates.all { getAllResourcesCount(it.resources) == 0 })
    }

    @Test
    fun buyItemCostsMatchRules() {
        assertEquals(
            initResources(mapOf(CatanResourceType.WOOD to 1, CatanResourceType.CLAY to 1)),
            getBuyItems().first { it.type == CatanBuyItemType.ROAD }.resources
        )
        assertEquals(
            initResources(mapOf(CatanResourceType.GRAIN to 2, CatanResourceType.ORE to 3)),
            getBuyItems().first { it.type == CatanBuyItemType.CITY }.resources
        )
    }

    @Test
    fun bankDealMustBeExchangeable() {
        val prices = initResourcePrices(4)
        val available = initResources(mapOf(CatanResourceType.WOOD to 8))
        val deal = CatanTradeDeal(
            type = CatanTradeType.BANK,
            offered = initResources(mapOf(CatanResourceType.WOOD to 4)),
            required = initResources(mapOf(CatanResourceType.ORE to 1))
        )
        assertTrue(checkDeal(deal, prices, available))
    }
}
