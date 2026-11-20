package boardgame.catan

import boardgame.api.BotAction
import boardgame.api.BotGameContext
import boardgame.api.Game
import boardgame.api.GameBackModule
import boardgame.api.GameBackService
import boardgame.api.GameContext
import boardgame.api.GameStatePayload
import boardgame.api.GameStaticSettings
import boardgame.api.GameStatus
import boardgame.api.decodeTree
import boardgame.api.encodeTree
import boardgame.api.removeElement
import boardgame.api.str
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs
import kotlin.math.roundToInt

class CatanGameModule : GameBackModule {
    override fun createService(): GameBackService = CatanGameBackService()
}

class CatanGameBackService(private val diceAnimationMs: Long = 1_000L) : GameBackService {

    override val type: String = CATAN_TYPE
    override val version: String = BuildInfo.version
    override val homepage: String = "https://github.com/HMHamster88/boardgame-kotlin-catan"
    override val gameStaticSettings: GameStaticSettings = GameStaticSettings(minPlayers = 2, maxPlayers = 6)

    override val localizedName: JsonObject = buildJsonObject {
        putJsonObject("en") { put(CATAN_TYPE, "Catan") }
        putJsonObject("ru") { put(CATAN_TYPE, "Колонизаторы") }
    }

    override fun getDefaultSettings(): JsonObject {
        val generation = CatanFieldGenerationSettings()
        val settings = CatanGameSettings(
            id = "",
            fieldGenerationSettings = generation,
            field = generateField(generation),
            maxPoints = 10,
            maxResourceCount = 7,
            resourcesForEachEvenSettlement = true,
            resourceForRobberOnVacantHex = true
        )
        return encodeTree(settings).jsonObject
    }

    override fun startGame(game: Game, settings: JsonObject): GameStatePayload {
        val gameSettings = decodeTree<CatanGameSettings>(settings)
        val publicPlayers = game.players.map { CatanPlayerPublicState(playerId = it.userId) }
        val statistics = CatanGameStatistics(
            playersStatistics = game.players
                .map { CatanPlayerStatistics(playerId = it.userId) }
                .toMutableList()
        )
        val publicState = CatanPublicGameState(
            field = gameSettings.field,
            phase = CatanGamePhase.EMBARK_FIRST,
            activePlayerIndex = randomInt(0, game.players.size - 1),
            playersStates = publicPlayers.toMutableList(),
            dices = CatanDices(),
            statistics = statistics
        )
        val privatePlayers = game.players.map { CatanPlayerPrivateState(playerId = it.userId) }
        val deck = shuffled(
            developmentCardsCount.flatMap { (card, count) -> List(count) { card } }
        ).toMutableList()
        val privateState = CatanPrivateGameState(
            playersStates = privatePlayers.toMutableList(),
            developmentCardsDeck = deck
        )
        game.status = GameStatus.STARTED
        return GameStatePayload(
            id = game.id,
            publicState = encodeTree(publicState),
            privateState = encodeTree(privateState)
        )
    }

    override suspend fun performAction(ctx: GameContext, action: JsonElement, playerId: String): JsonElement? {
        val settings = decodeTree<CatanGameSettings>(ctx.settings)
        val actionObject = action.jsonObject
        val actionType = actionObject.str("type") ?: return null

        if (actionType == "CatanGenerateFieldAction") {
            settings.field = generateField(settings.fieldGenerationSettings)
            ctx.updateSettings(encodeTree(settings))
            return null
        }

        if (ctx.publicState is JsonNull || ctx.privateState == null) return null
        val publicState = decodeTree<CatanPublicGameState>(ctx.publicState)
        val privateState = decodeTree<CatanPrivateGameState>(ctx.privateState!!)
        val game = ctx.game
        val field = publicState.field
        val activePlayer = game.players.getOrNull(publicState.activePlayerIndex)
        val activePlayerId = activePlayer?.userId
        val privatePlayerState = privateState.playersStates.firstOrNull { it.playerId == playerId } ?: return null
        val publicPlayerState = publicState.playersStates.firstOrNull { it.playerId == playerId } ?: return null
        val activePrivateState = privateState.playersStates.getOrNull(publicState.activePlayerIndex)
        val isActivePlayer = playerId == activePlayerId

        fun closeTrade(offer: CatanPlayerTradeOffer, chosenPlayerId: String?) {
            if (chosenPlayerId == null) {
                publicState.playerTradeOffer = null
                return
            }
            val tradePlayerState = privateState.playersStates.firstOrNull { it.playerId == offer.playerId } ?: return
            val chosenPlayerState = privateState.playersStates.firstOrNull { it.playerId == chosenPlayerId } ?: return
            removeResources(chosenPlayerState, offer.required)
            addResourcesToPlayer(chosenPlayerState, offer.offered)
            removeResources(tradePlayerState, offer.offered)
            addResourcesToPlayer(tradePlayerState, offer.required)
            publicState.playerTradeOffer = null
            ctx.notify(
                offer.playerId,
                "playerAcceptedDeal",
                buildJsonObject { put("player", game.players.firstOrNull { it.userId == chosenPlayerId }?.name) }
            )
        }

        when (actionType) {
            "CatanEmbarkAction" -> {
                val embark = decodeTree<CatanEmbarkAction>(action)
                if (!isActivePlayer || activePrivateState == null) return null
                val settlement = CatanIntersection(
                    position = embark.settlement,
                    intersectionObjects = mutableListOf(
                        CatanIntersectionObject(playerId = playerId, type = CatanIntersectionObjectType.SETTLEMENT)
                    )
                )
                field.intersections += settlement
                field.roads += CatanRoad(position = embark.road, playerId = playerId)

                if (publicState.phase == CatanGamePhase.EMBARK_SECOND) {
                    val resources = getResourcesForIntersection(field, settlement.position)
                    addResourcesToPlayer(activePrivateState, resources)
                    addResources(publicState.statistics.resourcesReceived, resources)
                }

                val roadsCount = field.roads.size
                if (publicState.phase == CatanGamePhase.EMBARK_FIRST) {
                    if (roadsCount >= game.players.size) {
                        publicState.phase = CatanGamePhase.EMBARK_SECOND
                    } else {
                        publicState.activePlayerIndex = (publicState.activePlayerIndex + 1) % game.players.size
                    }
                } else if (publicState.phase == CatanGamePhase.EMBARK_SECOND) {
                    publicState.activePlayerIndex--
                    if (publicState.activePlayerIndex < 0) {
                        publicState.activePlayerIndex = game.players.size - 1
                    }
                    if (roadsCount >= game.players.size * EMBARK_ROADS_COUNT) {
                        publicState.phase = CatanGamePhase.THROWING_DICE
                    }
                }
            }

            "CatanRollDicesAction" -> {
                if (!isActivePlayer || activePrivateState == null) return null
                publicState.dices.redDice = 0
                publicState.dices.yellowDice = 0
                ctx.updatePublicState(encodeTree(publicState))
                ctx.publishPublicState()
                if (diceAnimationMs > 0) delay(diceAnimationMs)

                publicState.dices.redDice = randomInt(1, 6)
                publicState.dices.yellowDice = randomInt(1, 6)
                val sum = publicState.dices.redDice + publicState.dices.yellowDice
                publicState.playerThrowedDices = true
                while (publicState.statistics.diceNumbersRolled.size <= sum) {
                    publicState.statistics.diceNumbersRolled += 0
                }
                publicState.statistics.diceNumbersRolled[sum]++

                if (sum == 7) {
                    var anyoneOver = false
                    privateState.playersStates.forEach { state ->
                        val count = getAllResourcesCount(state.resources)
                        if (count > maxPlayerResources(settings)) {
                            state.discardCardsCount = count / 2
                            anyoneOver = true
                        }
                    }
                    publicState.phase = if (anyoneOver) CatanGamePhase.DISCARD_CARDS_7 else CatanGamePhase.MOVE_ROBBER
                    return finishAction(ctx, publicState, privateState, settings)
                }

                field.hexes.filter { it.circularNumber == sum }.forEach { hex ->
                    if (hex.position == field.robberPos) return@forEach
                    val objects = findByCoords(hexVertices(hex.position), field.intersections) { it.position }
                        .flatMap { it.intersectionObjects }
                    objects.forEach { obj ->
                        val resources = getHexResources(hex.type, obj.type)
                        val playerState = privateState.playersStates.firstOrNull { it.playerId == obj.playerId }
                            ?: return@forEach
                        addResourcesToPlayer(playerState, resources)
                        addResources(publicState.statistics.resourcesReceived, resources)
                    }
                }
                publicState.phase = CatanGamePhase.PLAYER_TURN
            }

            "CatanBuildRoadAction" -> {
                val build = decodeTree<CatanBuildRoadAction>(action)
                if (!isActivePlayer || activePrivateState == null) return null
                val freeRoad = activePrivateState.freeBuildings.any { it == CatanBuyItemType.ROAD }
                val resources = if (freeRoad) initResources() else buyItemCost(CatanBuyItemType.ROAD)
                if (!checkPlayerHasResources(activePrivateState, resources)) return null
                if (field.roads.any { it.position == build.position }) return null
                field.roads += CatanRoad(position = build.position, playerId = playerId)
                if (freeRoad) {
                    activePrivateState.freeBuildings.removeElement(CatanBuyItemType.ROAD)
                } else {
                    removeResources(activePrivateState, resources)
                }
            }

            "CatanBuildIntObjectAction" -> {
                val build = decodeTree<CatanBuildIntObjectAction>(action)
                if (!isActivePlayer || activePrivateState == null) return null
                val buyItemType = when (build.objectType) {
                    CatanIntersectionObjectType.SETTLEMENT -> CatanBuyItemType.SETTLEMENT
                    CatanIntersectionObjectType.CITY -> CatanBuyItemType.CITY
                }
                val resources = buyItemCost(buyItemType)
                if (!checkPlayerHasResources(activePrivateState, resources)) return null

                var intersection = field.intersections.firstOrNull { it.position == build.position }
                if (intersection == null) {
                    intersection = CatanIntersection(position = build.position)
                    field.intersections += intersection
                }
                val existing = intersection.intersectionObjects.firstOrNull {
                    it.playerId == playerId && it.type == build.objectType
                }
                if (existing != null) return null

                if (build.objectType == CatanIntersectionObjectType.CITY) {
                    val settlement = intersection.intersectionObjects.firstOrNull {
                        it.playerId == playerId && it.type == CatanIntersectionObjectType.SETTLEMENT
                    } ?: return null
                    settlement.type = CatanIntersectionObjectType.CITY
                } else {
                    if (settings.resourcesForEachEvenSettlement) {
                        val localities = field.intersections.flatMap { it.intersectionObjects }
                            .count { it.playerId == playerId && it.type in loaclityTypes }
                        if ((localities + 1) % 2 == 0) {
                            val resourcesForSettlement = getResourcesForIntersection(field, build.position)
                            addResourcesToPlayer(activePrivateState, resourcesForSettlement)
                            addResources(publicState.statistics.resourcesReceived, resourcesForSettlement)
                        }
                    }
                    intersection.intersectionObjects += CatanIntersectionObject(
                        playerId = playerId,
                        type = CatanIntersectionObjectType.SETTLEMENT
                    )
                }
                removeResources(activePrivateState, resources)
            }

            "CatanEndTurnAction" -> {
                if (!isActivePlayer || activePrivateState == null) return null
                publicState.activePlayerIndex = (publicState.activePlayerIndex + 1) % game.players.size
                publicState.playerThrowedDices = false
                publicState.phase = CatanGamePhase.THROWING_DICE
                publicState.statistics.turnCount++
            }

            "CatanDiscardResourceCards" -> {
                val discard = decodeTree<CatanDiscardResourceCards>(action)
                if (privatePlayerState.discardCardsCount != getAllResourcesCount(discard.resources)) return null
                removeResources(privatePlayerState, discard.resources)
                privatePlayerState.discardCardsCount = 0
                if (privateState.playersStates.all { it.discardCardsCount == 0 }) {
                    publicState.phase = CatanGamePhase.MOVE_ROBBER
                }
            }

            "CatanMoveRobberAction" -> {
                val move = decodeTree<CatanMoveRobberAction>(action)
                field.robberPos = move.position
                val targetId = move.playerToRob
                if (targetId != null) {
                    val target = privateState.playersStates.firstOrNull { it.playerId == targetId }
                    if (target != null && getAllResourcesCount(target.resources) > 0) {
                        val resourceType = getNonNullResourceTypes(target.resources).randomOrNull()
                        if (resourceType != null) {
                            val robbed = initResources(mapOf(resourceType to 1))
                            removeResources(target, robbed)
                            addResourcesToPlayer(privatePlayerState, robbed)
                            val targetName = game.players.firstOrNull { it.userId == target.playerId }?.name
                            ctx.notify(
                                playerId,
                                "youRobbed",
                                buildJsonObject {
                                    put("playerName", targetName)
                                    put("t_resource", "resourceType.$resourceType")
                                }
                            )
                            ctx.notify(
                                target.playerId,
                                "playerStoleFromYou",
                                buildJsonObject {
                                    put("playerName", activePlayer?.name)
                                    put("t_resource", "resourceType.$resourceType")
                                }
                            )
                        }
                    }
                } else if (settings.resourceForRobberOnVacantHex) {
                    val hex = field.hexes.firstOrNull { it.position == move.position }
                    val resources = getHexResources(hex?.type ?: CatanTerrainHexType.DESERT, CatanIntersectionObjectType.SETTLEMENT)
                    addResourcesToPlayer(privatePlayerState, resources)
                    addResources(publicState.statistics.resourcesReceived, resources)
                }
                publicState.phase = if (publicState.playerThrowedDices) {
                    CatanGamePhase.PLAYER_TURN
                } else {
                    CatanGamePhase.THROWING_DICE
                }
            }

            "CatanTradeAction" -> {
                if (!isActivePlayer) return null
                val trade = decodeTree<CatanTradeAction>(action)
                if (!checkDeal(trade.deal, getPlayerPrices(field, playerId), privatePlayerState.resources)) {
                    return null
                }
                if (trade.deal.type == CatanTradeType.BANK) {
                    removeResources(privatePlayerState, trade.deal.offered)
                    addResourcesToPlayer(privatePlayerState, trade.deal.required)
                } else {
                    publicState.playerTradeOffer = CatanPlayerTradeOffer(
                        playerId = playerId,
                        offered = trade.deal.offered.toMutableMap(),
                        required = trade.deal.required.toMutableMap(),
                        answers = game.players.filter { it.userId != playerId }.map {
                            TradeAnswer(playerId = it.userId)
                        }.toMutableList()
                    )
                }
            }

            "CatanTradeResponseAction" -> {
                val response = decodeTree<CatanTradeResponseAction>(action)
                val offer = publicState.playerTradeOffer ?: return null
                val answer = offer.answers.firstOrNull { it.playerId == playerId } ?: return null
                answer.state = if (response.accepted) TradeAnswerState.ACCEPTED else TradeAnswerState.REJECTED
                if (offer.answers.any { it.state == TradeAnswerState.WAITING_FOR_ANSWER }) return null
                if (offer.answers.all { it.state == TradeAnswerState.REJECTED }) {
                    ctx.notify(offer.playerId, "playersRejectedDeal", null)
                    publicState.playerTradeOffer = null
                }
            }

            "CatanTradeCloseAction" -> {
                val close = decodeTree<CatanTradeCloseAction>(action)
                val offer = publicState.playerTradeOffer ?: return null
                closeTrade(offer, close.playerId)
            }

            "CatanBuyDevelopmentCardAction" -> {
                if (!isActivePlayer) return null
                if (privateState.developmentCardsDeck.isEmpty()) {
                    privateState.developmentCardsDeck = shuffled(privateState.developmentCardDiscardPile).toMutableList()
                }
                if (privateState.developmentCardsDeck.isEmpty()) return null
                privateState.developmentCardsDeck.removeAt(privateState.developmentCardsDeck.lastIndex)
                    .let { privatePlayerState.developmentCards += it }
                removeResources(privatePlayerState, buyItemCost(CatanBuyItemType.DEVELOPMENT_CARD))
            }

            "CatanUseDevelopmentCardAction" -> {
                val cardName = actionObject.str("developmentCard")
                val card = CatanDevelopmentCardType.entries.firstOrNull { it.name == cardName } ?: return null
                if (card == CatanDevelopmentCardType.YEAR_OF_PLENTY) {
                    val use = decodeTree<CatanUseResourceDevelopmentCardAction>(action)
                    removeDevelopmentCard(privatePlayerState, publicPlayerState, privateState, card)
                    addResources(publicState.statistics.resourcesReceived, use.resources)
                    addResourcesToPlayer(privatePlayerState, use.resources)
                } else if (card == CatanDevelopmentCardType.MONOPOLY) {
                    val use = decodeTree<CatanUseResourceTypeDevelopmentCardAction>(action)
                    removeDevelopmentCard(privatePlayerState, publicPlayerState, privateState, card)
                    privateState.playersStates.filter { it.playerId != playerId }.forEach { state ->
                        moveAllResourcesByType(privatePlayerState.resources, state.resources, use.resourcesType)
                    }
                } else {
                    removeDevelopmentCard(privatePlayerState, publicPlayerState, privateState, card)
                    when (card) {
                        CatanDevelopmentCardType.KNIGNT -> publicState.phase = CatanGamePhase.MOVE_ROBBER
                        CatanDevelopmentCardType.BUILD_ROADS -> {
                            privatePlayerState.freeBuildings += CatanBuyItemType.ROAD
                            privatePlayerState.freeBuildings += CatanBuyItemType.ROAD
                        }
                        else -> Unit
                    }
                }
            }
        }

        return finishAction(ctx, publicState, privateState, settings)
    }

    private fun removeDevelopmentCard(
        privatePlayerState: CatanPlayerPrivateState,
        publicPlayerState: CatanPlayerPublicState,
        privateState: CatanPrivateGameState,
        card: CatanDevelopmentCardType
    ) {
        privatePlayerState.developmentCards.removeElement(card)
        if (developmentCardSaves[card] == true) {
            publicPlayerState.openedDevelopmentCards += card
        } else {
            privateState.developmentCardDiscardPile += card
        }
    }

    private fun finishAction(
        ctx: GameContext,
        publicState: CatanPublicGameState,
        privateState: CatanPrivateGameState,
        settings: CatanGameSettings
    ): JsonElement? {
        val game = ctx.game
        val field = publicState.field
        val publicPoints = game.players.associate { it.userId to 0 }.toMutableMap()
        val privatePoints = game.players.associate { it.userId to 0 }.toMutableMap()
        val settlements = game.players.associate { it.userId to 0 }.toMutableMap()
        val cities = game.players.associate { it.userId to 0 }.toMutableMap()

        field.intersections.flatMap { it.intersectionObjects }.forEach { obj ->
            when (obj.type) {
                CatanIntersectionObjectType.SETTLEMENT -> {
                    publicPoints[obj.playerId] = (publicPoints[obj.playerId] ?: 0) + 1
                    settlements[obj.playerId] = (settlements[obj.playerId] ?: 0) + 1
                }

                CatanIntersectionObjectType.CITY -> {
                    publicPoints[obj.playerId] = (publicPoints[obj.playerId] ?: 0) + 2
                    cities[obj.playerId] = (cities[obj.playerId] ?: 0) + 1
                }
            }
        }
        privateState.playersStates.forEach { state ->
            state.developmentCards.forEach { card ->
                privatePoints[state.playerId] =
                    (privatePoints[state.playerId] ?: 0) + (developmentCardPoints[card] ?: 0)
            }
        }

        val biggestArmy = publicState.playersStates.maxByOrNull { armyCount(it) }
        if (biggestArmy != null && armyCount(biggestArmy) >= MIN_LARGEST_ARMY) {
            val current = publicState.playersStates.firstOrNull {
                CatanSpecialCard.BIGGEST_ARMY in it.specialCards
            }
            if (current == null || current.playerId == biggestArmy.playerId ||
                armyCount(biggestArmy) > armyCount(current)
            ) {
                publicPoints[biggestArmy.playerId] =
                    (publicPoints[biggestArmy.playerId] ?: 0) + SPECIAL_CARD_POINTS
                if (current != null && current.playerId != biggestArmy.playerId) {
                    current.specialCards.removeElement(CatanSpecialCard.BIGGEST_ARMY)
                }
                if (CatanSpecialCard.BIGGEST_ARMY !in biggestArmy.specialCards) {
                    biggestArmy.specialCards += CatanSpecialCard.BIGGEST_ARMY
                }
            }
        }

        val longestRoad = findLongestRoad(game.players, field, MIN_LONGEST_ROAD)
        if (longestRoad.size >= MIN_LONGEST_ROAD) {
            val longestRoadPlayerId = longestRoad.first().playerId
            val currentId = publicState.longestRoad.firstOrNull()?.playerId
            val player = publicState.playersStates.firstOrNull { it.playerId == longestRoadPlayerId }
            if (player != null && (currentId == null || longestRoadPlayerId == currentId ||
                    longestRoad.size > publicState.longestRoad.size)
            ) {
                publicPoints[longestRoadPlayerId] =
                    (publicPoints[longestRoadPlayerId] ?: 0) + SPECIAL_CARD_POINTS
                if (longestRoadPlayerId != currentId) {
                    publicState.playersStates.firstOrNull { it.playerId == currentId }
                        ?.specialCards?.removeElement(CatanSpecialCard.LONGEST_ROAD)
                    if (CatanSpecialCard.LONGEST_ROAD !in player.specialCards) {
                        player.specialCards += CatanSpecialCard.LONGEST_ROAD
                    }
                }
                publicState.longestRoad = longestRoad.toMutableList()
            }
        }

        publicState.playersStates.forEach { state ->
            state.points = publicPoints[state.playerId] ?: 0
            val privatePlayer = privateState.playersStates.firstOrNull { it.playerId == state.playerId }
            state.resourceCount = privatePlayer?.let { getAllResourcesCount(it.resources) } ?: 0
        }

        val allPoints = game.players.associate {
            it.userId to (publicPoints[it.userId] ?: 0) + (privatePoints[it.userId] ?: 0)
        }
        val winner = allPoints.maxByOrNull { it.value }
        if (winner != null && winner.value >= settings.maxPoints) {
            game.status = GameStatus.FINISHED
            publicState.winnersIds = mutableListOf(winner.key)
        }

        val finished = game.status == GameStatus.FINISHED
        publicState.statistics.playersStatistics = game.players.map { player ->
            val playerPublicState = publicState.playersStates.firstOrNull { it.playerId == player.userId }
            val settlementCount = settlements[player.userId] ?: 0
            val cityCount = cities[player.userId] ?: 0
            CatanPlayerStatistics(
                playerId = player.userId,
                points = publicPoints[player.userId] ?: 0,
                settlements = settlementCount,
                settlementPoints = settlementCount,
                cities = cityCount,
                cityPoints = cityCount * 2,
                longestRoadPoints = if (playerPublicState?.specialCards?.contains(
                        CatanSpecialCard.LONGEST_ROAD
                    ) == true
                ) {
                    SPECIAL_CARD_POINTS
                } else {
                    0
                },
                biggestArmyPoints = if (playerPublicState?.specialCards?.contains(
                        CatanSpecialCard.BIGGEST_ARMY
                    ) == true
                ) {
                    SPECIAL_CARD_POINTS
                } else {
                    0
                },
                privatePoints = if (finished) privatePoints[player.userId] ?: 0 else null
            )
        }.toMutableList()

        val onHand = initResources()
        privateState.playersStates.forEach { addResources(onHand, it.resources) }
        publicState.onHandResources = onHand

        ctx.updatePublicState(encodeTree(publicState))
        ctx.updatePrivateState(encodeTree(privateState))
        return null
    }

    private fun getResourcesForIntersection(field: CatanField, position: Vector2D): CatanResources {
        val result = initResources()
        vertexHexes(position).forEach { hexPosition ->
            val hex = field.hexes.firstOrNull { it.position == hexPosition } ?: return@forEach
            addResources(result, getHexResources(hex.type, CatanIntersectionObjectType.SETTLEMENT))
        }
        return result
    }

    private fun armyCount(state: CatanPlayerPublicState): Int =
        state.openedDevelopmentCards.count { it == CatanDevelopmentCardType.KNIGNT }

    private fun maxPlayerResources(settings: CatanGameSettings): Int = settings.maxResourceCount

    private fun checkPlayerHasResources(state: CatanPlayerPrivateState, resources: CatanResources): Boolean =
        resources.all { (type, count) -> (state.resources[type] ?: 0) >= count }

    private fun removeResources(state: CatanPlayerPrivateState, resources: CatanResources): Boolean {
        if (!checkPlayerHasResources(state, resources)) return false
        resources.forEach { (type, count) ->
            state.resources[type] = (state.resources[type] ?: 0) - count
        }
        return true
    }

    private fun addResourcesToPlayer(state: CatanPlayerPrivateState, resources: CatanResources) {
        resources.forEach { (type, count) ->
            state.resources[type] = (state.resources[type] ?: 0) + count
        }
    }

    private fun getHexResources(hex: CatanTerrainHexType, objectType: CatanIntersectionObjectType): CatanResources {
        val resource = when (hex) {
            CatanTerrainHexType.HILLS -> CatanResourceType.CLAY
            CatanTerrainHexType.FOREST -> CatanResourceType.WOOD
            CatanTerrainHexType.MOUNTAINS -> CatanResourceType.ORE
            CatanTerrainHexType.FIELDS -> CatanResourceType.GRAIN
            CatanTerrainHexType.PASTURE -> CatanResourceType.WOOL
            CatanTerrainHexType.DESERT -> null
        } ?: return initResources()
        val count = when (objectType) {
            CatanIntersectionObjectType.SETTLEMENT -> 1
            CatanIntersectionObjectType.CITY -> 2
        }
        return initResources(mapOf(resource to count))
    }

    private fun buyItemCost(type: CatanBuyItemType): CatanResources =
        getBuyItems().first { it.type == type }.resources

    private fun generateField(settings: CatanFieldGenerationSettings): CatanField {
        val props = FIELD_PROPERTIES[settings.fieldType] ?: FIELD_PROPERTIES.getValue(CatanGameFieldType.CLASSIC)
        val terrainTypes = shuffled(props.terrains.flatMap { (type, count) -> List(count) { type } }).toMutableList()
        val circularNumbers = shuffled(props.numbers.flatMap { (number, count) -> List(count) { number } }).toMutableList()
        val halfHeight = (props.height / 2.0).roundToInt()
        val hexes = mutableListOf<CatanTerrainHex>()
        val hexPositions = mutableListOf<Vector2D>()

        for (y in 0 until props.height) {
            val rowWidth = props.width - abs(halfHeight - y - 1)
            val shift = if (y < halfHeight) y else halfHeight - 1
            for (x in 0 until rowWidth) {
                val position = Vector2D(x - shift, y) * 6
                val neighbours = findByCoords(hexNeighborhoods(position), hexes) { it.position }
                val neighbourNumbers = neighbours.map { it.circularNumber }
                val neighbourTypes = neighbours.map { it.type }
                val hexType = if (settings.spreadHexTypes) {
                    terrainTypes.removeFirstNotIn(neighbourTypes) { a, b -> a == b }
                } else {
                    terrainTypes.removeAt(terrainTypes.lastIndex)
                }
                val circularNumber = if (hexType == CatanTerrainHexType.DESERT) {
                    0
                } else if (settings.spreadCircularNumbers) {
                    circularNumbers.removeFirstNotIn(neighbourNumbers) { a, b -> a == b }
                } else {
                    circularNumbers.removeAt(circularNumbers.lastIndex)
                }
                hexes += CatanTerrainHex(position, hexType, circularNumber)
                hexPositions += position
            }
        }

        val harbourTypes = shuffled(props.harbours.flatMap { (type, count) -> List(count) { type } }).toMutableList()
        val outRoads = hexes.flatMap { hexEdges(it.position) }
            .filter { isOutEdge(it, hexPositions) }
            .toMutableList()
        val harbours = mutableListOf<CatanHarbour>()
        while (harbourTypes.isNotEmpty()) {
            val position = outRoads[randomInt(0, outRoads.lastIndex)]
            val type = harbourTypes.removeAt(harbourTypes.lastIndex)
            val neighbourhood = edgeNeighborhoods(position)
            harbours += CatanHarbour(position, type)
            outRoads.remove(position)
            removeByPositions(outRoads, neighbourhood) { it }
        }

        return CatanField(
            width = props.width,
            height = props.height,
            hexes = hexes,
            harbours = harbours,
            roads = mutableListOf(),
            intersections = mutableListOf(),
            robberPos = hexes.first { it.type == CatanTerrainHexType.DESERT }.position
        )
    }

    override suspend fun nextBotAction(ctx: BotGameContext): BotAction? = null

    private data class FieldProperties(
        val width: Int,
        val height: Int,
        val terrains: Map<CatanTerrainHexType, Int>,
        val numbers: Map<Int, Int>,
        val harbours: Map<CatanHarbourType, Int>
    )

    companion object {
        private const val EMBARK_ROADS_COUNT = 2
        private const val MIN_LARGEST_ARMY = 3
        private const val MIN_LONGEST_ROAD = 5
        private const val SPECIAL_CARD_POINTS = 2

        private val FIELD_PROPERTIES = mapOf(
            CatanGameFieldType.CLASSIC to FieldProperties(
                width = 5,
                height = 5,
                terrains = mapOf(
                    CatanTerrainHexType.FIELDS to 4,
                    CatanTerrainHexType.MOUNTAINS to 3,
                    CatanTerrainHexType.HILLS to 3,
                    CatanTerrainHexType.DESERT to 1,
                    CatanTerrainHexType.FOREST to 4,
                    CatanTerrainHexType.PASTURE to 4
                ),
                numbers = mapOf(
                    6 to 2, 3 to 2, 8 to 2, 2 to 1, 4 to 2,
                    5 to 2, 10 to 2, 9 to 2, 11 to 2, 12 to 1
                ),
                harbours = mapOf(
                    CatanHarbourType.WOOD to 1,
                    CatanHarbourType.ORE to 1,
                    CatanHarbourType.CLAY to 1,
                    CatanHarbourType.WOOL to 1,
                    CatanHarbourType.GRAIN to 1,
                    CatanHarbourType.THREE_TO_ONE to 4
                )
            ),
            CatanGameFieldType.EXTENDED to FieldProperties(
                width = 6,
                height = 7,
                terrains = mapOf(
                    CatanTerrainHexType.FIELDS to 6,
                    CatanTerrainHexType.MOUNTAINS to 5,
                    CatanTerrainHexType.HILLS to 5,
                    CatanTerrainHexType.DESERT to 2,
                    CatanTerrainHexType.FOREST to 6,
                    CatanTerrainHexType.PASTURE to 6
                ),
                numbers = mapOf(
                    6 to 3, 3 to 3, 8 to 3, 2 to 2, 4 to 3,
                    5 to 2, 10 to 3, 9 to 3, 11 to 3, 12 to 3
                ),
                harbours = mapOf(
                    CatanHarbourType.WOOD to 1,
                    CatanHarbourType.ORE to 1,
                    CatanHarbourType.CLAY to 1,
                    CatanHarbourType.WOOL to 2,
                    CatanHarbourType.GRAIN to 1,
                    CatanHarbourType.THREE_TO_ONE to 4
                )
            )
        )
    }
}
