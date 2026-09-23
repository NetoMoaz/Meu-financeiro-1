package com.example.viewmodel

import com.example.data.BudgetEntity
import com.example.data.CategoryEntity
import com.example.data.CreditCardEntity
import com.example.data.LoanEntity
import com.example.data.RecurringBillEntity
import com.example.data.SavingsGoalEntity
import com.example.data.TransactionEntity

data class CategoryExpense(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val colorHex: Long,
    val iconName: String
)

data class BudgetProgress(
    val category: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val percentage: Float,
    val isOverBudget: Boolean,
    val isNearBudget: Boolean
)

data class MonthSummary(
    val yearMonth: String, // "2026-09"
    val displayLabel: String, // "Set 2026"
    val income: Double,
    val expense: Double
)

data class CalendarDayItem(
    val day: Int,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "BILL", "LOAN", "CARD"
    val isPaid: Boolean,
    val category: String
)

data class EconomyModeResult(
    val saldoDisponivel: Double,
    val contasNaoPagas: Double,
    val faturasCartao: Double,
    val parcelasEmprestimo: Double,
    val metasAporte: Double,
    val saldoRealLivre: Double,
    val diasRestantes: Int,
    val disponivelPorDia: Double,
    val disponivelPorSemana: Double,
    val status: EconomyStatus,
    val mensagemStatus: String,
    val dica: String
)

enum class EconomyStatus {
    OTIMO,
    ATENCAO,
    CRITICO
}

data class CardInvoiceInfo(
    val card: CreditCardEntity,
    val currentInvoiceTotal: Double,
    val availableLimit: Double,
    val upcomingInstallmentsCount: Int,
    val isPaidThisMonth: Boolean = false
)

data class LoanSummary(
    val totalDebt: Double,
    val totalMonthlyInstallments: Double,
    val loansCount: Int,
    val estimatedMonthsToFree: Int
)

data class CommitmentTotals(
    val unpaidBills: Double,
    val cardExpense: Double,
    val pendingLoans: Double,
    val goalsTarget: Double,
    val diasRestantes: Int,
    val semanasRestantes: Double
)
