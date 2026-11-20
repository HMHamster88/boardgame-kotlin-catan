<template>
    <div class="flex flex-col">
        <span>{{ t('turnCount') }} : {{ catanStatistics.turnCount }}</span>
        <h5>{{ t('playersPoints') }}</h5>
        <div v-for="playerPoints in playersPoints" :key="playerPoints.playerId" class="player-points">
            <div class="player-points-header">
                <span>{{ playerPoints.name }}</span>
                <span>{{ playerPoints.points }}</span>
            </div>
            <ul class="points-breakdown">
                <li>
                    <span>{{ t('settlements') }}</span>
                    <span>{{ playerPoints.settlements }} × 1 = {{ playerPoints.settlementPoints }}</span>
                </li>
                <li>
                    <span>{{ t('cities') }}</span>
                    <span>{{ playerPoints.cities }} × 2 = {{ playerPoints.cityPoints }}</span>
                </li>
                <li>
                    <span>{{ t('longestRoad') }}</span>
                    <span>{{ signedPoints(playerPoints.longestRoadPoints) }}</span>
                </li>
                <li>
                    <span>{{ t('biggestArmy') }}</span>
                    <span>{{ signedPoints(playerPoints.biggestArmyPoints) }}</span>
                </li>
                <li v-if="gameFinished">
                    <span>{{ t('developmentCards') }}</span>
                    <span>{{ playerPoints.privatePoints }}</span>
                </li>
                <li v-if="gameFinished" class="points-total">
                    <span>{{ t('totalPoints') }}</span>
                    <span>{{ playerPoints.totalPoints }}</span>
                </li>
            </ul>
        </div>
        <h5>{{ t('dices') }}</h5>
        <div v-for="diceVal in diceRollsStats" class="flex flex-row items-center">
            <span style="width: 3rem;">{{ diceVal.diceValue }} : {{ diceVal.count }}</span>
            <div class="progress" :data-label="diceVal.prcentage.toFixed(2) + '%'">
                <span class="value" :style="`width:${diceVal.prcentage}%;`"></span>
            </div>
        </div>
        <h5>{{ t('resources') }}</h5>
        <div v-for="resourceVal in resourcesStats" class="flex flex-row items-center">
            <img class="resource-icon" :src="resourcesImages[resourceVal.type]"></img>
            <span style="width: 3rem;"> : {{ resourceVal.count }}</span>
            <div class="progress" :data-label="resourceVal.prcentage.toFixed(2) + '%'">
                <span class="value" :style="`width:${resourceVal.prcentage}%;`"></span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { GameStatusEnum, type Game, type GameStatistics } from 'boardgame-web-common';
import { CatanResourceType, getAllResourcesCount, type CatanGameStatistics } from '../domain';
import { computed, type PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import { resourcesImages } from './graphics';

let localization: any = {
    en: {
        dices: 'Dices',
        turnCount: 'Turn Count',
        playersPoints: 'Players Points',
        settlements: 'Settlements',
        cities: 'Cities',
        longestRoad: 'Longest road',
        biggestArmy: 'Biggest army',
        developmentCards: 'Development cards',
        totalPoints: 'Total',
        resources: 'Resources Received',
        resourceType: {
            WOOD: "Wood",
            WOOL: "Whool",
            GRAIN: "Grain",
            CLAY: "Clay",
            ORE: "Ore"
        }
    },
    ru: {
        dices: 'Кубики',
        turnCount: 'Количество Ходов',
        playersPoints: 'Очки игроков',
        settlements: 'Поселения',
        cities: 'Города',
        longestRoad: 'Самая длинная дорога',
        biggestArmy: 'Самая большая армия',
        developmentCards: 'Карты развития',
        totalPoints: 'Всего',
        resources: 'Полученные Ресурсы',
        resourceType: {
            WOOD: "Дерево",
            WOOL: "Шерсть",
            GRAIN: "Зерно",
            CLAY: "Глина",
            ORE: "Руда"
        }
    }
}
const { t } = useI18n({
    locale: 'en',
    messages: localization
})

interface DiceStat {
    diceValue: number
    count: number
    prcentage: number
}

const diceRollsStats = computed(() => {
    const result: DiceStat[] = []
    const allCount = catanStatistics.value.diceNumbersRolled.filter(n => n != null).reduce((a, b) => a + b, 0)
    for (let i = 0; i < catanStatistics.value.diceNumbersRolled.length; i++) {
        const numberRolled = catanStatistics.value.diceNumbersRolled[i]
        if (numberRolled != null) {
            result.push({
                diceValue: i,
                count: numberRolled,
                prcentage: (numberRolled / allCount * 100)
            })
        }
    }
    return result
})

interface ResourceStat {
    type: CatanResourceType
    count: number
    prcentage: number
}

const resourcesStats = computed(() => {
    const result: ResourceStat[] = []
    const allCount = getAllResourcesCount(catanStatistics.value.resourcesReceived)
    Object.entries(catanStatistics.value.resourcesReceived).forEach(([resourcesType, resourceCount]) => {
        result.push({
            type: resourcesType as CatanResourceType,
            count: resourceCount,
            prcentage: (resourceCount / allCount * 100)
        })
    })
    return result
})

const catanStatistics = computed(() => {
    return props.statistics as CatanGameStatistics
})

const gameFinished = computed(() => props.game?.status === GameStatusEnum.FINISHED)

function signedPoints(points: number): string {
    return points === 0 ? '0' : `+${points}`
}

const playersPoints = computed(() => {
    const statistics = catanStatistics.value.playersStatistics ?? []
    const players = props.game?.players ?? []
    const statisticsByPlayerId = new Map(statistics.map(stat => [stat.playerId, stat]))
    return players.map(player => {
        const stat = statisticsByPlayerId.get(player.userId)
        const points = stat?.points ?? 0
        const privatePoints = stat?.privatePoints ?? 0
        const settlements = stat?.settlements ?? 0
        const cities = stat?.cities ?? 0
        return {
            playerId: player.userId,
            name: player.name,
            points,
            settlements,
            settlementPoints: settlements,
            cities,
            cityPoints: cities * 2,
            longestRoadPoints: stat?.longestRoadPoints ?? 0,
            biggestArmyPoints: stat?.biggestArmyPoints ?? 0,
            privatePoints,
            totalPoints: points + privatePoints
        }
    })
})

const props = defineProps({
    statistics: {
        type: Object as PropType<GameStatistics>,
        required: true
    },
    game: {
        type: Object as PropType<Game>,
        required: true
    }
})
</script>

<style>
.player-points {
    margin-bottom: 0.75rem;
}

.player-points-header {
    display: flex;
    justify-content: space-between;
    font-weight: 600;
    border-bottom: 1px solid var(--oruga-control-border-color);
    padding-bottom: 0.15rem;
}

.points-breakdown {
    list-style: none;
    margin: 0.25rem 0 0;
    padding-left: 1.25rem;
}

.points-breakdown li {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.1rem 0;
}

.points-total {
    font-weight: 600;
    border-top: 1px solid var(--oruga-control-border-color);
    margin-top: 0.15rem;
    padding-top: 0.25rem;
}

.progress {
    margin: 0.2rem;
    height: 1.5em;
    width: 100%;
    background-color: var(--oruga-control-background-color);
    position: relative;
    border-width: var(--oruga-control-border-width);
    border-style: solid;
    border-color: var(--oruga-control-border-color);
    border-radius: var(--oruga-border-radius);
}

.progress:before {
    content: attr(data-label);
    font-size: 0.8em;
    position: absolute;
    text-align: center;
    left: 0;
    right: 0;
}

.progress .value {
    background-color: var(--main-color);
    display: inline-block;
    height: 100%;
}

.resource-icon {
    width: 2rem;
    max-width: 2rem;
}
</style>