package com.mg2000.xkorean.ui.transform

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ThumbnailInfo::class], version = 1, exportSchema = false)
abstract class XKoreanDatabase : RoomDatabase() {
	abstract fun thumbnailDao() : ThumbnailDao

	companion object {
		private var mInstance: XKoreanDatabase? = null

		@Synchronized
		fun getInstance(context: Context) : XKoreanDatabase {
			if (mInstance == null) {
				mInstance = Room.databaseBuilder(context.applicationContext, XKoreanDatabase::class.java, "xKorean")
					.fallbackToDestructiveMigration()
					.build()
			}

			return mInstance!!
		}
	}
}