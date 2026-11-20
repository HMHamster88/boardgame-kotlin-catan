package boardgame.catan

import boardgame.api.Game
import boardgame.api.GameContext
import boardgame.api.GameStatus
import boardgame.api.Player
import boardgame.api.decodeTree
import boardgame.api.encodeTree
import boardgame.api.nowIso
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatanActionTest {

    private val service = CatanGameBackService(diceAnimationMs = 0)

    private class FakeContext(
        override val game: Game,
        initialSettings: JsonObject,
        initialPublic: JsonElement,
        initialPrivate: JsonElement?
    ) : GameContext {
        private var settingsValue = initialSettings
        private var publicValue = initialPublic
        private var privateValue = initialPrivate

        val notifications = mutableListOf<String>()
        val publishedPublicStates = mutableListOf<JsonElement>()

        override val settings: JsonObject get() = settingsValue
        override val publicState: JsonElement get() = publicValue
        override val privateState: JsonElement? get() = privateValue

        override fun updatePublicState(value: JsonElement) {
            publicValue = value
        }

        override fun updatePrivateState(value: JsonElement?) {
            privateValue = value
        }

        override fun updateSettings(value: JsonElement) {
            settingsValue = value.jsonObject
        }

        override fun publishPublicState() {
            publishedPublicStates += publicValue
        }

        override fun notify(peerId: String?, message: String, params: JsonElement?) {
            notifications += message
        }
    }

    private fun game(): Game = Game(
        id = "catan-test",
        owner = "p1",
        name = "Catan",
        type = CATAN_TYPE,
        players = mutableListOf(
            Player("p1", "Alice", "#ff0000", online = true, isBot = false),
            Player("p2", "Bob", "#0000ff", online = true, isBot = false)
        ),
        created = nowIso()
    )

    private fun context(game: Game, settings: JsonObject, state: boardgame.api.GameStatePayload): FakeContext =
        FakeContext(game, settings, state.publicState, state.privateState)

    private fun activePlayerId(ctx: FakeContext): String {
        val public = decodeTree<CatanPublicGameState>(ctx.publicState)
        return ctx.game.players[public.activePlayerIndex].userId
    }

    @Test
    fun fourEmbarkPlacementsStartTheDicePhase() = runBlocking {
        val game = game()
        val settings = service.getDefaultSettings()
        val state = service.startGame(game, settings)
        val ctx = context(game, settings, state)

        val placements = listOf(
            CatanEmbarkAction(settlement = Vector2D(2, 2), road = Vector2D(3, 0)),
            CatanEmbarkAction(settlement = Vector2D(-2, -2), road = Vector2D(-3, 0)),
            CatanEmbarkAction(settlement = Vector2D(4, -2), road = Vector2D(3, -3)),
            CatanEmbarkAction(settlement = Vector2D(-4, 2), road = Vector2D(-3, 3))
        )

        placements.forEach { action ->
            val playerId = activePlayerId(ctx)
            service.performAction(ctx, encodeTree(action), playerId)
        }

        val public = decodeTree<CatanPublicGameState>(ctx.publicState)
        assertEquals(CatanGamePhase.THROWING_DICE, public.phase)
        assertEquals(4, public.field.roads.size)
        assertEquals(4, public.field.intersections.size)
        assertEquals(2, public.playersStates.size)
    }

    @Test
    fun rollAndEndTurnAdvanceThePhase() = runBlocking {
        val game = game()
        val settings = service.getDefaultSettings()
        val state = service.startGame(game, settings)
        val ctx = context(game, settings, state)

        repeat(4) {
            val playerId = activePlayerId(ctx)
            service.performAction(
                ctx,
                encodeTree(CatanEmbarkAction(settlement = Vector2D(it * 2, 2), road = Vector2D(it * 3, 0))),
                playerId
            )
        }

        val active = activePlayerId(ctx)
        service.performAction(ctx, encodeTree(CatanRollDicesAction()), active)
        val afterRoll = decodeTree<CatanPublicGameState>(ctx.publicState)
        assertTrue(ctx.publishedPublicStates.isNotEmpty())
        val zeroFrame = decodeTree<CatanPublicGameState>(ctx.publishedPublicStates.first())
        assertEquals(0, zeroFrame.dices.redDice)
        assertEquals(0, zeroFrame.dices.yellowDice)
        assertTrue(afterRoll.phase != CatanGamePhase.EMBARK_FIRST)
        assertTrue(afterRoll.phase != CatanGamePhase.EMBARK_SECOND)
        assertTrue(afterRoll.dices.redDice in 1..6)
        assertTrue(afterRoll.dices.yellowDice in 1..6)
        assertTrue(afterRoll.playerThrowedDices)

        if (afterRoll.phase == CatanGamePhase.PLAYER_TURN) {
            service.performAction(ctx, encodeTree(CatanEndTurnAction()), active)
            val afterEnd = decodeTree<CatanPublicGameState>(ctx.publicState)
            assertEquals(CatanGamePhase.THROWING_DICE, afterEnd.phase)
            assertEquals(1, afterEnd.statistics.turnCount)
            assertTrue(activePlayerId(ctx) != active)
        }
    }

    @Test
    fun privatePointsAreRevealedOnlyWhenTheGameIsFinished() = runBlocking {
        val game = game()
        val settings = decodeTree<CatanGameSettings>(service.getDefaultSettings()).copy(maxPoints = 2)
        val settingsJson = encodeTree(settings).jsonObject
        val state = service.startGame(game, settingsJson)
        val ctx = context(game, settingsJson, state)

        val public = decodeTree<CatanPublicGameState>(ctx.publicState)
        public.activePlayerIndex = 0
        val private = decodeTree<CatanPrivateGameState>(ctx.privateState!!)
        private.playersStates.first { it.playerId == "p1" }.developmentCards += CatanDevelopmentCardType.CHAPEL
        ctx.updatePublicState(encodeTree(public))
        ctx.updatePrivateState(encodeTree(private))

        service.performAction(
            ctx,
            encodeTree(CatanEmbarkAction(settlement = Vector2D(2, 2), road = Vector2D(3, 0))),
            "p1"
        )

        val after = decodeTree<CatanPublicGameState>(ctx.publicState)
        assertEquals(GameStatus.FINISHED, game.status)
        val p1 = after.statistics.playersStatistics.first { it.playerId == "p1" }
        assertEquals(1, p1.points)
        assertEquals(1, p1.settlements)
        assertEquals(0, p1.cities)
        assertEquals(0, p1.longestRoadPoints)
        assertEquals(0, p1.biggestArmyPoints)
        assertEquals(1, p1.privatePoints)
        val p2 = after.statistics.playersStatistics.first { it.playerId == "p2" }
        assertEquals(0, p2.points)
        assertEquals(0, p2.settlements)
        assertEquals(0, p2.cities)
        assertEquals(0, p2.privatePoints)
    }

    @Test
    fun statisticsBreakDownCitiesAndBiggestArmyPoints() = runBlocking {
        val game = game()
        val settings = service.getDefaultSettings()
        val state = service.startGame(game, settings)
        val ctx = context(game, settings, state)

        val public = decodeTree<CatanPublicGameState>(ctx.publicState)
        public.activePlayerIndex = 0
        public.playersStates.first { it.playerId == "p1" }.openedDevelopmentCards +=
            List(3) { CatanDevelopmentCardType.KNIGNT }
        public.field.intersections += CatanIntersection(
            position = Vector2D(10, 10),
            intersectionObjects = mutableListOf(
                CatanIntersectionObject(
                    playerId = "p1",
                    type = CatanIntersectionObjectType.CITY
                )
            )
        )
        ctx.updatePublicState(encodeTree(public))

        service.performAction(
            ctx,
            encodeTree(CatanEmbarkAction(settlement = Vector2D(2, 2), road = Vector2D(3, 0))),
            "p1"
        )

        val p1 = decodeTree<CatanPublicGameState>(ctx.publicState)
            .statistics.playersStatistics.first { it.playerId == "p1" }
        assertEquals(1, p1.settlements)
        assertEquals(1, p1.cities)
        assertEquals(0, p1.longestRoadPoints)
        assertEquals(2, p1.biggestArmyPoints)
        assertEquals(5, p1.points)
        assertEquals(null, p1.privatePoints)
    }

    @Test
    fun generateFieldIsASettingsAction() = runBlocking {
        val game = game()
        val settings = service.getDefaultSettings()
        val state = service.startGame(game, settings)
        val ctx = context(game, settings, state)

        service.performAction(ctx, encodeTree(CatanGenerateFieldAction()), "p1")
        val after = decodeTree<CatanGameSettings>(ctx.settings).field
        assertEquals(19, after.hexes.size)
        assertEquals(9, after.harbours.size)
        assertEquals(
            after.robberPos,
            after.hexes.first { it.type == CatanTerrainHexType.DESERT }.position
        )
    }
}
