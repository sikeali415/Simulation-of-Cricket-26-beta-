package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.models.MatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history ORDER BY dateTime DESC")
    fun getAllMatches(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistory)

    @Query("DELETE FROM match_history WHERE id = :id")
    suspend fun deleteMatchById(id: Int)

    @Query("DELETE FROM match_history")
    suspend fun clearAllMatches()
}

@Database(entities = [MatchHistory::class], version = 1, exportSchema = false)
abstract class CricketDatabase : RoomDatabase() {
    abstract fun matchHistoryDao(): MatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CricketDatabase? = null

        fun getDatabase(context: Context): CricketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketDatabase::class.java,
                    "cricket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CricketRepository(private val matchHistoryDao: MatchHistoryDao) {
    val allMatches: Flow<List<MatchHistory>> = matchHistoryDao.getAllMatches()

    suspend fun insertMatch(match: MatchHistory) {
        matchHistoryDao.insertMatch(match)
    }

    suspend fun deleteMatchById(id: Int) {
        matchHistoryDao.deleteMatchById(id)
    }

    suspend fun clearHistory() {
        matchHistoryDao.clearAllMatches()
    }
}
