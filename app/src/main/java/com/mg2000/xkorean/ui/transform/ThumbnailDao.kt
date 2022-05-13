package com.mg2000.xkorean.ui.transform

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ThumbnailDao {
	@Query("SELECT * FROM ThumbnailInfo")
	fun loadThumbnailInfo(): Array<ThumbnailInfo>

	@Insert
	fun insertThumbnailInfo(thumbnailInfo: ThumbnailInfo)

	@Update
	fun updateThumbnailInfo(vararg thumbnailInfo: ThumbnailInfo)
}