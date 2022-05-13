package com.mg2000.xkorean.ui.transform

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(primaryKeys = ["id"])
data class ThumbnailInfo(val id: String, val info: String) {
}