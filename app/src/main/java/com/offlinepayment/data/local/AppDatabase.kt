package com.offlinepayment.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OfflineUser::class, OfflineWallet::class, LocalTransaction::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineUserDao(): OfflineUserDao
    abstract fun offlineWalletDao(): OfflineWalletDao
    abstract fun localTransactionDao(): LocalTransactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Add balance columns
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE offline_users ADD COLUMN totalBalance REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE offline_users ADD COLUMN offlineBalance REAL NOT NULL DEFAULT 0.0")
            }
        }
        
        // Migration from version 2 to 3: Add offline_wallets table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_wallets (
                        walletId INTEGER NOT NULL PRIMARY KEY,
                        userId INTEGER NOT NULL,
                        walletType TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        balance TEXT NOT NULL,
                        publicKey TEXT,
                        privateKeyEncrypted TEXT,
                        bankAccountNumber TEXT,
                        isActive INTEGER NOT NULL,
                        lastSyncedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 3 to 4: Add offline_transactions table (old format)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_transactions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        transactionId TEXT NOT NULL UNIQUE,
                        amount TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        status TEXT NOT NULL,
                        senderUserId INTEGER NOT NULL,
                        senderWalletId INTEGER NOT NULL,
                        senderName TEXT NOT NULL,
                        senderEmail TEXT NOT NULL,
                        senderAccount TEXT NOT NULL,
                        senderPublicKey TEXT NOT NULL,
                        receiverUserId INTEGER NOT NULL,
                        receiverWalletId INTEGER NOT NULL,
                        receiverEmail TEXT NOT NULL,
                        receiverPublicKey TEXT NOT NULL,
                        transactionSignature TEXT NOT NULL,
                        receiptHash TEXT NOT NULL,
                        receiptData TEXT NOT NULL,
                        deviceFingerprint TEXT,
                        createdAtDevice TEXT NOT NULL,
                        syncedAt INTEGER,
                        syncError TEXT
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 4 to 5: Replace offline_transactions with local_transactions (new simplified format)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop old table
                database.execSQL("DROP TABLE IF EXISTS offline_transactions")
                
                // Create new simplified table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_transactions (
                        txId TEXT NOT NULL PRIMARY KEY,
                        payerId TEXT NOT NULL,
                        payeeId TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        direction TEXT NOT NULL,
                        rawPayload TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 5 to 6: Update local_transactions to match backend sync requirements
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with all required fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_transactions_new (
                        txId TEXT NOT NULL PRIMARY KEY,
                        senderWalletId INTEGER NOT NULL,
                        receiverPublicKey TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        transactionSignature TEXT NOT NULL,
                        nonce TEXT NOT NULL,
                        receiptHash TEXT NOT NULL,
                        receiptData TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAtDevice INTEGER NOT NULL,
                        syncedAt INTEGER,
                        errorReason TEXT,
                        deviceFingerprint TEXT,
                        payerId TEXT,
                        payeeId TEXT,
                        direction TEXT,
                        rawPayload TEXT
                    )
                """.trimIndent())
                
                // Create index on nonce
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_transactions_nonce ON local_transactions_new (nonce)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_local_transactions_status ON local_transactions_new (status)")
                
                // Migrate existing data if any (with defaults)
                database.execSQL("""
                    INSERT INTO local_transactions_new (
                        txId, senderWalletId, receiverPublicKey, amount, currency,
                        transactionSignature, nonce, receiptHash, receiptData,
                        status, createdAtDevice, payerId, payeeId, direction, rawPayload
                    )
                    SELECT 
                        txId, 0, '', CAST(amount AS TEXT), 'PKR',
                        '', txId, '', '',
                        'pending', timestamp, payerId, payeeId, direction, rawPayload
                    FROM local_transactions
                """.trimIndent())
                
                // Drop old table and rename new one
                database.execSQL("DROP TABLE local_transactions")
                database.execSQL("ALTER TABLE local_transactions_new RENAME TO local_transactions")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_payment_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

