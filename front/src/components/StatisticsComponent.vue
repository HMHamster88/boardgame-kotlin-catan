<template>
    <div class="flex flex-col">
        <span>{{ t('turnCount') }} : {{ catanStatistics.turnCount }}</span>
        <h5>{{ t('playersPoints') }}</h5>
        <table class="points-table">
            <thead>
                <tr>
                    <th>{{ t('player') }}</th>
                    <th>{{ t('points') }}</th>
                    <th v-if="gameFinished">{{ t('privatePoints') }}</th>
                    <th v-if="gameFinished">{{ t('totalPoints') }}</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="playerPoints in playersPoints" :key="playerPoints.playerId">
                    <td>{{ playerPoints.name }}</td>
                    <td>{{ playerPoints.points }}</td>
                    <template v-if="gameFinished">
                        <td>{{ playerPoints.privatePoints }}</td>
                        <td>{{ playerPoints.totalPoints }}</td>
                    </template>
                </tr>
            </tbody>
        </table>
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
        player: 'Player',
        points: 'Public Points',
        privatePoints: 'Private Points',
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
        player: 'Игрок',
        points: 'Открытые очки',
        privatePoints: 'Личные очки',
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

const playersPoints = computed(() => {
    const statistics = catanStatistics.value.playersStatistics ?? []
    const players = props.game?.players ?? []
    const statisticsByPlayerId = new Map(statistics.map(stat => [stat.playerId, stat]))
    return players.map(player => {
        const stat = statisticsByPlayerId.get(player.userId)
        const points = stat?.points ?? 0
        const privatePoints = stat?.privatePoints ?? 0
        return {
            playerId: player.userId,
            name: player.name,
            points,
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
.points-table {
    width: 100%;
    margin-bottom: 0.5rem;
    border-collapse: collapse;
}

.points-table th,
.points-table td {
    padding: 0.15rem 0.4rem;
    text-align: left;
    border-bottom: 1px solid var(--oruga-control-border-color);
}

.points-table th:not(:first-child),
.points-table td:not(:first-child) {
    text-align: center;
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