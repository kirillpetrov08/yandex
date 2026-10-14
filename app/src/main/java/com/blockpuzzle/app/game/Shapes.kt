package com.blockpuzzle.app.game

/**
 * Фигура — набор клеток в прямоугольной сетке width×height (row-major).
 * Чистая модель без Android-зависимостей: удобно тестировать.
 */
data class Shape(
    val id: String,
    val width: Int,
    val height: Int,
    val mask: IntArray,
    val weight: Int = 10,
) {
    val cellCount: Int = mask.count { it == 1 }

    fun isFilled(row: Int, col: Int): Boolean =
        row in 0 until height && col in 0 until width && mask[row * width + col] == 1

    override fun equals(other: Any?): Boolean = other is Shape && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** Каталог фигур для лотка: как в классических блочных головоломках. */
object Shapes {

    private fun shape(id: String, rows: List<String>, weight: Int = 10): Shape {
        val height = rows.size
        val width = rows.maxOf { it.length }
        val mask = IntArray(width * height)
        rows.forEachIndexed { r, line ->
            line.forEachIndexed { c, ch -> mask[r * width + c] = if (ch == '#') 1 else 0 }
        }
        return Shape(id, width, height, mask, weight)
    }

    val single: Shape = shape("1x1", listOf("#"), weight = 3)

    val line2H: Shape = shape("2h", listOf("##"), weight = 9)
    val line2V: Shape = shape("2v", listOf("#", "#"), weight = 9)
    val line3H: Shape = shape("3h", listOf("###"), weight = 8)
    val line3V: Shape = shape("3v", listOf("#", "#", "#"), weight = 8)
    val line4H: Shape = shape("4h", listOf("####"), weight = 5)
    val line4V: Shape = shape("4v", listOf("#", "#", "#", "#"), weight = 5)
    val line5H: Shape = shape("5h", listOf("#####"), weight = 3)
    val line5V: Shape = shape("5v", listOf("#", "#", "#", "#", "#"), weight = 3)

    val square2: Shape = shape("2x2", listOf("##", "##"), weight = 9)
    val square3: Shape = shape("3x3", listOf("###", "###", "###"), weight = 2)
    val rect2x3: Shape = shape("2x3", listOf("###", "###"), weight = 3)
    val rect3x2: Shape = shape("3x2", listOf("##", "##", "##"), weight = 3)

    val cornerSmall1: Shape = shape("c2a", listOf("##", "#."), weight = 7)
    val cornerSmall2: Shape = shape("c2b", listOf("##", ".#"), weight = 7)
    val cornerSmall3: Shape = shape("c2c", listOf("#.", "##"), weight = 7)
    val cornerSmall4: Shape = shape("c2d", listOf(".#", "##"), weight = 7)

    val cornerBig1: Shape = shape("c3a", listOf("###", "#..", "#.."), weight = 5)
    val cornerBig2: Shape = shape("c3b", listOf("###", "..#", "..#"), weight = 5)
    val cornerBig3: Shape = shape("c3c", listOf("#..", "#..", "###"), weight = 5)
    val cornerBig4: Shape = shape("c3d", listOf("..#", "..#", "###"), weight = 5)

    val lSmall1: Shape = shape("l2a", listOf("#.", "#.", "##"), weight = 6)
    val lSmall2: Shape = shape("l2b", listOf(".#", ".#", "##"), weight = 6)
    val lSmall3: Shape = shape("l2c", listOf("##", "#.", "#."), weight = 6)
    val lSmall4: Shape = shape("l2d", listOf("##", ".#", ".#"), weight = 6)

    val sShape1: Shape = shape("s1", listOf(".##", "##."), weight = 4)
    val sShape2: Shape = shape("s2", listOf("##.", ".##"), weight = 4)
    val tShape1: Shape = shape("t1", listOf("###", ".#."), weight = 4)
    val tShape2: Shape = shape("t2", listOf(".#.", "###"), weight = 4)
    val tShape3: Shape = shape("t3", listOf("#.", "##", "#."), weight = 4)
    val tShape4: Shape = shape("t4", listOf(".#", "##", ".#"), weight = 4)

    /** Все фигуры, которые могут попасть в лоток. */
    val all: List<Shape> = listOf(
        single,
        line2H, line2V, line3H, line3V, line4H, line4V, line5H, line5V,
        square2, square3, rect2x3, rect3x2,
        cornerSmall1, cornerSmall2, cornerSmall3, cornerSmall4,
        cornerBig1, cornerBig2, cornerBig3, cornerBig4,
        lSmall1, lSmall2, lSmall3, lSmall4,
        sShape1, sShape2, tShape1, tShape2, tShape3, tShape4,
    )

    /** Фигуры из трёх клеток — стартовый набор для приятной первой партии. */
    val easySet: List<Shape> = listOf(line2H, line2V, line3H, line3V, cornerSmall1, cornerSmall2, single)

    fun byId(id: String): Shape? = all.firstOrNull { it.id == id }
}
