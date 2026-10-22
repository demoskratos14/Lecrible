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
    val channelId: String? = null,
    /** Premier lien http(s) du texte : sert de secours quand l'intention d'origine a été perdue. */
    val link: String? = null,
    /** Notification permanente : reste dans la liste, même après l'avoir ouverte. */
    @ColumnInfo(defaultValue = "0") val ongoing: Boolean = false
)

@Dao
interface NotifDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(n: NotifEntity)

    @Query("SELECT * FROM notifs ORDER BY time DESC")
    fun all(): Flow<List<NotifEntity>>

    @Query("SELECT * FROM notifs WHERE notifKey = :key LIMIT 1")
    suspend fun byKey(key: String): NotifEntity?

    @Query("DELETE FROM notifs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifs WHERE pkg = :pkg AND ongoing = 0")
    suspend fun deleteApp(pkg: String)

    /** Vide tout sauf les notifications permanentes. */
    @Query("DELETE FROM notifs WHERE ongoing = 0")
    suspend fun clearRegular()

    @Query("DELETE FROM notifs")
    suspend fun clear()
}

@Database(entities = [NotifEntity::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): NotifDao

    companion object {
        @Volatile private var inst: AppDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifs ADD COLUMN channelId TEXT")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifs ADD COLUMN link TEXT")
                db.execSQL("ALTER TABLE notifs ADD COLUMN ongoing INTEGER NOT NULL DEFAULT 0")
            }
        }
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "notifs.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { inst = it }
        }
    }
}
