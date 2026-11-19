package boardgame.catan

import boardgame.api.Game
import boardgame.api.GameContext
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

    private val service = CatanGameBackService()

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
