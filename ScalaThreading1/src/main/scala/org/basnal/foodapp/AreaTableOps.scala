package org.basnal.foodapp

object AreaTableOps {
  def markAreaClosed(table: AreaTable, areaIdx: Int): AreaTable = {
    require(areaIdx >= 0 && areaIdx < table.size, s"Invalid area index: $areaIdx")
    
    table.copy(
      isOpen = table.isOpen.updated(areaIdx, false)
    )
  }
} 