package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringBillEntity::class,
        CreditCardEntity::class,
        LoanEntity::class,
        SavingsGoalEntity::class,
        BudgetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Alimentação", iconName = "restaurant", colorHex = 0xFFF59E0B, isDefault = true),
            CategoryEntity(name = "Mercado", iconName = "shopping_cart", colorHex = 0xFF10B981, isDefault = true),
            CategoryEntity(name = "Transporte", iconName = "directions_car", colorHex = 0xFF3B82F6, isDefault = true),
            CategoryEntity(name = "Casa", iconName = "home", colorHex = 0xFF8B5CF6, isDefault = true),
            CategoryEntity(name = "Saúde", iconName = "medical_services", colorHex = 0xFFEF4444, isDefault = true),
            CategoryEntity(name = "Lazer", iconName = "sports_esports", colorHex = 0xFFEC4899, isDefault = true),
            CategoryEntity(name = "Educação", iconName = "school", colorHex = 0xFF14B8A6, isDefault = true),
            CategoryEntity(name = "Salário / Renda", iconName = "payments", colorHex = 0xFF10B981, isDefault = true, type = "INCOME"),
            CategoryEntity(name = "Investimentos", iconName = "trending_up", colorHex = 0xFF059669, isDefault = true, type = "INCOME"),
            CategoryEntity(name = "Outros", iconName = "category", colorHex = 0xFF6B7280, isDefault = true)
        )

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN lastPaidMonth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isInvoicePayment INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_financeiro_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
