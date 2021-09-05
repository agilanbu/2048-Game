package com.agilanbu.game2048kotlin

class GridTiles : CellItem {
    val mValue: Int
    var mMergedFrom: Array<GridTiles>? = null

    constructor(x: Int, y: Int, value: Int) : super(x, y) {
        this.mValue = value
    }

    constructor(cell: CellItem, value: Int) : super(cell.x, cell.y) {
        this.mValue = value
    }

    fun updatePosition(cell: CellItem) {
        x = cell.x
        y = cell.y
    }
}