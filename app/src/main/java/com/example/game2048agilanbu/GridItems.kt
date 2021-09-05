package com.example.game2048agilanbu

import java.util.ArrayList

class GridItems(sizeX: Int, sizeY: Int) {
    val field: Array<Array<GridTiles?>>
    val undoField: Array<Array<GridTiles?>>
    private val bufferField: Array<Array<GridTiles?>>

    fun randomAvailableCell(): CellItem? {
        val availableCells = getAvailableCells()
        return if (availableCells.size >= 1) availableCells[Math.floor(Math.random() * availableCells.size)
            .toInt()] else null
    }

    /*val availableCells: ArrayList<Cell>
        get() {
            val availableCells = ArrayList<Cell>()
            for (xx in field.indices) for (yy in 0 until field[0].size) if (field[xx].get(yy) == null) availableCells.add(
                Cell(xx, yy)
            )
            return availableCells
        }*/

    fun getAvailableCells(): ArrayList<CellItem> {
        val availableCells = ArrayList<CellItem>()
        for (xx in 0 until field.size) for (yy in 0 until field[0].size)
            if (field[xx][yy] == null) availableCells.add(
                CellItem(xx, yy)
            )
        return availableCells
    }

    val isCellsAvailable: Boolean
        get() = getAvailableCells().size >= 1

    fun isCellAvailable(cell: CellItem?): Boolean {
        return !isCellOccupied(cell)
    }

    fun isCellOccupied(cell: CellItem?): Boolean {
        return getCellContent(cell) != null
    }

    fun getCellContent(cell: CellItem?): GridTiles? {
        return if (cell != null && isCellWithinBounds(cell)) field[cell.x][cell.y] else null
    }

    fun getCellContent(x: Int, y: Int): GridTiles? {
        return if (isCellWithinBounds(x, y)) field[x][y] else null
    }

    fun isCellWithinBounds(cell: CellItem): Boolean {
        return 0 <= cell.x && cell.x < field.size && 0 <= cell.y && cell.y < field[0].size
    }

    private fun isCellWithinBounds(x: Int, y: Int): Boolean {
        return 0 <= x && x < field.size && 0 <= y && y < field[0].size
    }

    fun insertTile(tile: GridTiles) {
        field[tile.x][tile.y] = tile
    }

    fun removeTile(tile: GridTiles) {
        field[tile.x][tile.y] = null
    }

    fun saveTiles() {
        for (xx in bufferField.indices) for (yy in 0 until bufferField[0].size) if (bufferField[xx][yy] == null) undoField[xx][yy] =
            null else undoField[xx][yy] = GridTiles(xx, yy, bufferField[xx][yy]!!.value)
    }

    fun prepareSaveTiles() {
        for (xx in field.indices) for (yy in 0 until field[0].size) if (field[xx][yy] == null) bufferField[xx][yy] =
            null else bufferField[xx][yy] = GridTiles(xx, yy, field[xx][yy]!!.value)
    }

    fun revertTiles() {
        for (xx in undoField.indices) for (yy in 0 until undoField[0].size) if (undoField[xx][yy] == null) field[xx][yy] =
            null else field[xx][yy] = GridTiles(xx, yy, undoField[xx][yy]!!.value)
    }

    fun clearGrid() {
        for (xx in field.indices) for (yy in 0 until field[0].size) field[xx][yy] = null
    }

    fun clearUndoGrid() {
        for (xx in field.indices) for (yy in 0 until field[0].size) undoField[xx][yy] = null
    }

    init {
        field = Array(sizeX) { arrayOfNulls(sizeY) }
        undoField = Array(sizeX) { arrayOfNulls(sizeY) }
        bufferField = Array(sizeX) { arrayOfNulls(sizeY) }
        clearGrid()
        clearUndoGrid()
    }
}