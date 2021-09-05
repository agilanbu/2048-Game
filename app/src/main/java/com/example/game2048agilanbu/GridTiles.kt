package com.example.game2048agilanbu

class GridTiles : CellItem {
    val value: Int
    var mergedFrom: Array<GridTiles>? = null

    constructor(x: Int, y: Int, value: Int) : super(x, y) {
        this.value = value
    }

    constructor(cell: CellItem, value: Int) : super(cell.x, cell.y) {
        this.value = value
    }

    fun updatePosition(cell: CellItem) {
        x = cell.x
        y = cell.y
    }
}