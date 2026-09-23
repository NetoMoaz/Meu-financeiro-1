package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val dateMillis: Long,
    val paymentMethod: String,
    val cardId: Long? = null,
    val installmentNumber: Int = 1,
    val totalInstallments: Int = 1,
    val notes: String = "",
    val recurringBillId: Long? = null,
    val loanId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isInvoicePayment: Boolean = false
)

fun TransactionEntity.isCreditCardInvoicePayment(): Boolean {
    if (this.isInvoicePayment) return true
    val cat = this.category.trim().lowercase()
    if (cat == "pagamento de fatura" || cat == "fatura de cartão" || cat == "fatura cartão" || cat == "pagamento fatura") return true
    val t = this.title.trim().lowercase()
    if (t.startsWith("pagamento fatura") || t.startsWith("pagamento de fatura") || t.startsWith("fatura paga")) return true
    val method = this.paymentMethod.trim().lowercase()
    if (method == "pagamento de fatura") return true
    if (this.notes.lowercase().contains("pagamento de fatura referente a")) return true
    return false
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: Long,
    val isDefault: Boolean = false,
    val type: String = "ALL" // "ALL", "EXPENSE", "INCOME"
)

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int, // 1 to 31
    val frequency: String = "MENSAL", // "MENSAL", "SEMANAL", "ANUAL"
    val category: String,
    val lastPaidMonth: String = "", // e.g. "2026-09"
    val notes: String = ""
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitAmount: Double,
    val closingDay: Int,
    val dueDay: Int,
    val colorHex: Long = 0xFF10B981,
    val lastFourDigits: String = "",
    val lastPaidMonth: String = ""
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val institution: String,
    val originalAmount: Double? = null,
    val currentBalance: Double,
    val installmentAmount: Double,
    val totalInstallments: Int,
    val paidInstallments: Int,
    val interestRate: Double? = null, // % a.m.
    val dueDay: Int,
    val lastPaidMonth: String = "",
    val notes: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDateMillis: Long,
    val notes: String = ""
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val limitAmount: Double,
    val monthYear: String = "" // empty means recurring default
)
