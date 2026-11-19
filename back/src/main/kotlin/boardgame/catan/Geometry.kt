package boardgame.catan

/** Hex-grid geometry (flat-top layout, integer grid units) ported from the TS common package. */

fun hexVertices(position: Vector2D): List<Vector2D> = listOf(
    position + Vector2D(2, 2),
    position + Vector2D(4, -2),
    position + Vector2D(2, -4),
    position + Vector2D(-2, -2),
    position + Vector2D(-4, 2),
    position + Vector2D(-2, 4)
)

fun hexEdges(position: Vector2D): List<Vector2D> = listOf(
    position + Vector2D(3, 0),
    position + Vector2D(3, -3),
    position + Vector2D(0, -3),
    position + Vector2D(-3, 0),
    position + Vector2D(-3, 3),
    position + Vector2D(0, 3)
)

fun hexNeighborhoods(position: Vector2D): List<Vector2D> = listOf(
    position + Vector2D(0, 6),
    position + Vector2D(6, 0),
    position + Vector2D(6, -6),
    position + Vector2D(0, -6),
    position + Vector2D(-6, 0),
    position + Vector2D(-6, 6)
)

private fun edgeAngle(position: Vector2D): Int = when {
    position.y % 2 != 0 && position.x % 2 != 0 -> -60
    position.x % 2 == 0 -> 60
    else -> 0
}

fun edgeNeighborhoods(position: Vector2D): List<Vector2D> = when (edgeAngle(position)) {
    0 -> listOf(
        position + Vector2D(0, 3),
        position + Vector2D(0, -3),
        position + Vector2D(-3, 3),
        position + Vector2D(3, -3)
    )
    60 -> listOf(
        position + Vector2D(-3, 3),
        position + Vector2D(3, -3),
        position + Vector2D(3, 0),
        position + Vector2D(-3, 0)
    )
    -60 -> listOf(
        position + Vector2D(3, 0),
        position + Vector2D(-3, 0),
        position + Vector2D(0, 3),
        position + Vector2D(0, -3)
    )
    else -> emptyList()
}

fun edgeHexes(position: Vector2D): List<Vector2D> = when (edgeAngle(position)) {
    0 -> listOf(position + Vector2D(-3, 0), position + Vector2D(3, 0))
    -60 -> listOf(position + Vector2D(3, -3), position + Vector2D(-3, 3))
    60 -> listOf(position + Vector2D(0, -3), position + Vector2D(0, 3))
    else -> emptyList()
}

fun edgeVertices(position: Vector2D): List<Vector2D> = when (edgeAngle(position)) {
    0 -> listOf(position + Vector2D(-1, 2), position + Vector2D(1, -2))
    -60 -> listOf(position + Vector2D(-1, -1), position + Vector2D(1, 1))
    60 -> listOf(position + Vector2D(2, -1), position + Vector2D(-2, 1))
    else -> emptyList()
}

private fun isLowerVertex(position: Vector2D): Boolean =
    position.y % 2 == 0 && (position.y + 2) % 6 == 0

fun vertexEdges(position: Vector2D): List<Vector2D> = if (isLowerVertex(position)) {
    listOf(
        position + Vector2D(-1, 2),
        position + Vector2D(2, -1),
        position + Vector2D(-1, -1)
    )
} else {
    listOf(
        position + Vector2D(1, 1),
        position + Vector2D(1, -2),
        position + Vector2D(-2, 1)
    )
}

fun vertexHexes(position: Vector2D): List<Vector2D> = if (isLowerVertex(position)) {
    listOf(
        position + Vector2D(2, 2),
        position + Vector2D(2, -4),
        position + Vector2D(-4, 2)
    )
} else {
    listOf(
        position + Vector2D(-2, 4),
        position + Vector2D(4, -2),
        position + Vector2D(-2, -2)
    )
}

fun isOutEdge(position: Vector2D, hexPositions: List<Vector2D>): Boolean {
    val edgeHexes = edgeHexes(position)
    return hexPositions.count { it == edgeHexes[0] || it == edgeHexes[1] } == 1
}

fun <T> findByCoords(coords: List<Vector2D>, values: List<T>, position: (T) -> Vector2D): List<T> =
    coords.mapNotNull { coord -> values.firstOrNull { position(it) == coord } }

/** Extra helper used by the map generator: a random value in [min, max]. */
fun randomInt(min: Int, max: Int): Int =
    if (max <= min) min else min + kotlin.random.Random.nextInt(max - min + 1)

/** Extra helper used by the map generator: remove positions equal to any of the given ones. */
fun <T> removeByPositions(values: MutableList<T>, positions: List<Vector2D>, position: (T) -> Vector2D) {
    values.removeAll { value -> positions.any { it == position(value) } }
}

/** Small utility for map loops. */
fun <T> shuffled(values: List<T>): List<T> = values.toMutableList().also { list ->
    for (index in list.lastIndex downTo 1) {
        val other = kotlin.random.Random.nextInt(index + 1)
        val tmp = list[index]
        list[index] = list[other]
        list[other] = tmp
    }
}

/** Removes and returns the first element not present in [find], matching the TS generator. */
fun <T> MutableList<T>.removeFirstNotIn(find: List<T>, equals: (T, T) -> Boolean): T {
    var index = indexOfFirst { element -> find.none { equals(element, it) } }
    if (index <= 0) index = 0
    return removeAt(index)
}
