package com.lecrible.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifs", indices = [Index(value = ["notifKey"], unique = true)])
data class NotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notifKey: String,
    val pkg: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val time: Long
)

@Dao
interface NotifDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(n: NotifEntity)

    @Query("SELECT * FROM notifs ORDER BY time DESC")
    fun all(): Flow<List<NotifEntity>>

    @Query("DELETE FROM notifs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifs WHERE pkg = :pkg")
    suspend fun deleteApp(pkg: String)

    @Query("DELETE FROM notifs")
    suspend fun clear()
}

@Database(entities = [NotifEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): NotifDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "notifs.db")
                .build().also { inst = it }
        }
    }
}
