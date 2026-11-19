// Generated from Kotlin models by the module's generateTs task. Do not edit.
// Only model types shared with the back-end are generated; front-only helpers stay hand-written.

export interface CatanBuildIntObjectAction {
    type: 'CatanBuildIntObjectAction'
    objectType: CatanIntersectionObjectType
    position: Vector2D
}

export interface CatanBuildRoadAction {
    type: 'CatanBuildRoadAction'
    position: Vector2D
}

export interface CatanBuyDevelopmentCardAction {
    type: 'CatanBuyDevelopmentCardAction'
}

export enum CatanBuyItemType {
    ROAD = 'ROAD',
    SETTLEMENT = 'SETTLEMENT',
    CITY = 'CITY',
    DEVELOPMENT_CARD = 'DEVELOPMENT_CARD',
}

export enum CatanDevelopmentCardType {
    KNIGNT = 'KNIGNT',
    BUILD_ROADS = 'BUILD_ROADS',
    MONOPOLY = 'MONOPOLY',
    YEAR_OF_PLENTY = 'YEAR_OF_PLENTY',
    CHAPEL = 'CHAPEL',
    GREAT_HALL = 'GREAT_HALL',
    LIBRARY = 'LIBRARY',
    MARKET = 'MARKET',
    UNIVERSITY = 'UNIVERSITY',
}

export interface CatanDices {
    redDice: number
    yellowDice: number
}

export interface CatanDiscardResourceCards {
    type: 'CatanDiscardResourceCards'
    resources: Record<CatanResourceType, number>
}

export interface CatanEmbarkAction {
    type: 'CatanEmbarkAction'
    settlement: Vector2D
    road: Vector2D
}

export interface CatanEndTurnAction {
    type: 'CatanEndTurnAction'
}

export interface CatanField {
    width: number
    height: number
    hexes: CatanTerrainHex[]
    harbours: CatanHarbour[]
    roads: CatanRoad[]
    intersections: CatanIntersection[]
    robberPos: Vector2D
}

export interface CatanFieldGenerationSettings {
    fieldType: CatanGameFieldType
    spreadHexTypes: boolean
    spreadCircularNumbers: boolean
}

export enum CatanGameFieldType {
    CLASSIC = 'CLASSIC',
    EXTENDED = 'EXTENDED',
}

export enum CatanGamePhase {
    EMBARK_FIRST = 'EMBARK_FIRST',
    EMBARK_SECOND = 'EMBARK_SECOND',
    THROWING_DICE = 'THROWING_DICE',
    PLAYER_TURN = 'PLAYER_TURN',
    DISCARD_CARDS_7 = 'DISCARD_CARDS_7',
    MOVE_ROBBER = 'MOVE_ROBBER',
}

export interface CatanGameSettings {
    id: string
    fieldGenerationSettings: CatanFieldGenerationSettings
    field: CatanField
    maxPoints: number
    maxResourceCount: number
    resourcesForEachEvenSettlement: boolean
    resourceForRobberOnVacantHex: boolean
}

export interface CatanGameStatistics {
    turnCount: number
    resourcesReceived: Record<CatanResourceType, number>
    diceNumbersRolled: number[]
}

export interface CatanGenerateFieldAction {
    type: 'CatanGenerateFieldAction'
}

export interface CatanHarbour {
    position: Vector2D
    type: CatanHarbourType
}

export enum CatanHarbourType {
    THREE_TO_ONE = 'THREE_TO_ONE',
    CLAY = 'CLAY',
    ORE = 'ORE',
    GRAIN = 'GRAIN',
    WOOL = 'WOOL',
    WOOD = 'WOOD',
}

export interface CatanIntersection {
    position: Vector2D
    intersectionObjects: CatanIntersectionObject[]
}

export interface CatanIntersectionObject {
    playerId: string
    type: CatanIntersectionObjectType
}

export enum CatanIntersectionObjectType {
    SETTLEMENT = 'SETTLEMENT',
    CITY = 'CITY',
}

export interface CatanMoveRobberAction {
    type: 'CatanMoveRobberAction'
    position: Vector2D
    playerToRob?: string
}

export interface CatanPlayerPrivateState {
    playerId: string
    resources: Record<CatanResourceType, number>
    developmentCards: CatanDevelopmentCardType[]
    discardCardsCount: number
    freeBuildings: CatanBuyItemType[]
}

export interface CatanPlayerPublicState {
    playerId: string
    points: number
    openedDevelopmentCards: CatanDevelopmentCardType[]
    specialCards: CatanSpecialCard[]
    resourceCount: number
}

export interface CatanPlayerTradeOffer {
    playerId: string
    offered: Record<CatanResourceType, number>
    required: Record<CatanResourceType, number>
    answers: TradeAnswer[]
}

export interface CatanPrivateGameState {
    playersStates: CatanPlayerPrivateState[]
    developmentCardsDeck: CatanDevelopmentCardType[]
    developmentCardDiscardPile: CatanDevelopmentCardType[]
}

export interface CatanPublicGameState {
    field: CatanField
    phase: CatanGamePhase
    activePlayerIndex: number
    winnersIds: string[]
    playersStates: CatanPlayerPublicState[]
    dices: CatanDices
    playerThrowedDices: boolean
    playerTradeOffer?: CatanPlayerTradeOffer
    longestRoad: CatanRoad[]
    statistics: CatanGameStatistics
    onHandResources: Record<CatanResourceType, number>
}

export enum CatanResourceType {
    WOOD = 'WOOD',
    WOOL = 'WOOL',
    GRAIN = 'GRAIN',
    CLAY = 'CLAY',
    ORE = 'ORE',
}

export interface CatanRoad {
    position: Vector2D
    playerId: string
}

export interface CatanRollDicesAction {
    type: 'CatanRollDicesAction'
}

export enum CatanSpecialCard {
    BIGGEST_ARMY = 'BIGGEST_ARMY',
    LONGEST_ROAD = 'LONGEST_ROAD',
}

export interface CatanTerrainHex {
    position: Vector2D
    type: CatanTerrainHexType
    circularNumber: number
}

export enum CatanTerrainHexType {
    HILLS = 'HILLS',
    FOREST = 'FOREST',
    MOUNTAINS = 'MOUNTAINS',
    FIELDS = 'FIELDS',
    PASTURE = 'PASTURE',
    DESERT = 'DESERT',
}

export interface CatanTradeAction {
    type: 'CatanTradeAction'
    deal: CatanTradeDeal
}

export interface CatanTradeCloseAction {
    type: 'CatanTradeCloseAction'
    playerId?: string
}

export interface CatanTradeDeal {
    type: CatanTradeType
    offered: Record<CatanResourceType, number>
    required: Record<CatanResourceType, number>
}

export interface CatanTradeResponseAction {
    type: 'CatanTradeResponseAction'
    accepted: boolean
}

export enum CatanTradeType {
    BANK = 'BANK',
    PLAYER = 'PLAYER',
}

export interface CatanUseDevelopmentCardAction {
    type: 'CatanUseDevelopmentCardAction'
    developmentCard: CatanDevelopmentCardType
}

export interface CatanUseResourceDevelopmentCardAction {
    type: 'CatanUseDevelopmentCardAction'
    developmentCard: CatanDevelopmentCardType
    resources: Record<CatanResourceType, number>
}

export interface CatanUseResourceTypeDevelopmentCardAction {
    type: 'CatanUseDevelopmentCardAction'
    developmentCard: CatanDevelopmentCardType
    resourcesType: CatanResourceType
}

export interface TradeAnswer {
    playerId: string
    state: TradeAnswerState
}

export enum TradeAnswerState {
    WAITING_FOR_ANSWER = 'WAITING_FOR_ANSWER',
    ACCEPTED = 'ACCEPTED',
    REJECTED = 'REJECTED',
}

export interface Vector2D {
    x: number
    y: number
}

