package id.kasku.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalance: Long = 0
)

/**
 * type: "in" (masuk), "out" (keluar), "tf" (transfer antar dompet). date: yyyy-MM-dd
 * auto: dibuat otomatis oleh transaksi berulang. goalId: terkait target tabungan.
 * ref: pengembalian/penarikan dari target (mengurangi hitungan tabungan, bukan pemasukan).
 */
@Entity(tableName = "transactions")
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val walletId: Long,
    val toWalletId: Long? = null,
    val category: String? = null,
    val note: String = "",
    val date: String,
    val auto: Boolean = false,
    val goalId: Long? = null,
    val ref: Boolean = false
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val limitAmount: Long
)

@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val walletId: Long,
    val toWalletId: Long? = null,
    val category: String? = null,
    val note: String = "",
    val day: Int,
    val last: String,
    val paused: Boolean = false
)

/** closedDate kosong = target masih aktif. */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val saved: Long = 0,
    val date: String = "",
    val colorIdx: Int = 0,
    val closedDate: String = "",
    val closedWallet: Long = 0,
    val closedAmount: Long = 0,
    val closedKept: Long = 0
)

@Entity(tableName = "goal_log")
data class GoalLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val txId: Long? = null,
    val date: String,
    val type: String,
    val amount: Long,
    val walletId: Long = 0,
    val note: String
)

@Entity(tableName = "shop")
data class ShopItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Long = 0,
    val done: Boolean = false
)

@Dao
interface KaskuDao {
    @Query("SELECT * FROM wallets ORDER BY id") fun wallets(): Flow<List<Wallet>>
    @Query("SELECT * FROM wallets ORDER BY id") suspend fun walletsOnce(): List<Wallet>
    @Insert suspend fun insertWallet(w: Wallet): Long
    @Update suspend fun updateWallet(w: Wallet)
    @Delete suspend fun deleteWallet(w: Wallet)
    @Query("SELECT (SELECT COUNT(*) FROM transactions WHERE walletId = :id OR toWalletId = :id) + (SELECT COUNT(*) FROM recurring WHERE walletId = :id OR toWalletId = :id)")
    suspend fun walletUsage(id: Long): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun transactions(): Flow<List<Tx>>
    @Insert suspend fun insertTx(t: Tx): Long
    @Update suspend fun updateTx(t: Tx)
    @Delete suspend fun deleteTx(t: Tx)

    @Query("SELECT * FROM budgets") fun budgets(): Flow<List<Budget>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBudget(b: Budget)
    @Query("DELETE FROM budgets WHERE category = :c") suspend fun deleteBudget(c: String)

    @Query("SELECT * FROM recurring ORDER BY id") fun recurring(): Flow<List<Recurring>>
    @Query("SELECT * FROM recurring ORDER BY id") suspend fun recurringOnce(): List<Recurring>
    @Insert suspend fun insertRec(r: Recurring): Long
    @Update suspend fun updateRec(r: Recurring)
    @Delete suspend fun deleteRec(r: Recurring)

    @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM goals WHERE id = :id") suspend fun goalById(id: Long): Goal?
    @Insert suspend fun insertGoal(g: Goal): Long
    @Update suspend fun updateGoal(g: Goal)
    @Delete suspend fun deleteGoal(g: Goal)
    @Query("SELECT * FROM goal_log ORDER BY id") fun goalLogs(): Flow<List<GoalLog>>
    @Insert suspend fun insertLog(l: GoalLog): Long
    @Query("DELETE FROM goal_log WHERE txId = :txId") suspend fun deleteLogByTx(txId: Long)

    @Query("SELECT * FROM shop ORDER BY id") fun shop(): Flow<List<ShopItem>>
    @Insert suspend fun insertShop(s: ShopItem): Long
    @Update suspend fun updateShop(s: ShopItem)
    @Delete suspend fun deleteShop(s: ShopItem)
    @Query("DELETE FROM shop WHERE done = 1") suspend fun deleteDoneShop()

    @Query("DELETE FROM transactions") suspend fun clearTx()
    @Query("DELETE FROM wallets") suspend fun clearWallets()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM recurring") suspend fun clearRec()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM goal_log") suspend fun clearLogs()
    @Query("DELETE FROM shop") suspend fun clearShop()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN auto INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN goalId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN ref INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS recurring (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, walletId INTEGER NOT NULL, toWalletId INTEGER, category TEXT, note TEXT NOT NULL, day INTEGER NOT NULL, last TEXT NOT NULL, paused INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, target INTEGER NOT NULL, saved INTEGER NOT NULL, date TEXT NOT NULL, colorIdx INTEGER NOT NULL, closedDate TEXT NOT NULL, closedWallet INTEGER NOT NULL, closedAmount INTEGER NOT NULL, closedKept INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS goal_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL, txId INTEGER, date TEXT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, walletId INTEGER NOT NULL, note TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS shop (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, price INTEGER NOT NULL, done INTEGER NOT NULL)")
    }
}

@Database(
    entities = [Wallet::class, Tx::class, Budget::class, Recurring::class, Goal::class, GoalLog::class, ShopItem::class],
    version = 2,
    exportSchema = false
)
abstract class KaskuDb : RoomDatabase() {
    abstract fun dao(): KaskuDao

    companion object {
        @Volatile private var inst: KaskuDb? = null

        fun get(ctx: Context): KaskuDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, KaskuDb::class.java, "kasku.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        listOf("Tunai", "Bank", "E-wallet", "Tabungan").forEach {
                            db.execSQL("INSERT INTO wallets(name, initialBalance) VALUES ('$it', 0)")
                        }
                    }
                })
                .build()
                .also { inst = it }
        }
    }
}
