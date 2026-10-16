package com.lecrible.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifs", indices = [Index(value = ["notifKey"], unique = true)])
data class NotifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notifKey: String,
    val pkg: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val time: Long,
    val channelId: String? = null
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

@Database(entities = [NotifEntity::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): NotifDao

    companion object {
        @Volatile private var inst: AppDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifs ADD COLUMN channelId TEXT")
            }
        }
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "notifs.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { inst = it }
        }
    }
}
