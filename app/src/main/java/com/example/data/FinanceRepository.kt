package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(private val dao: FinanceDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allRecurringBills: Flow<List<RecurringBillEntity>> = dao.getAllRecurringBills()
    val allCreditCards: Flow<List<CreditCardEntity>> = dao.getAllCreditCards()
    val allLoans: Flow<List<LoanEntity>> = dao.getAllLoans()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()

    suspend fun ensureDefaultCategories() {
        val existing = dao.getAllCategoriesList()
        if (existing.isEmpty()) {
            dao.insertCategories(AppDatabase.DEFAULT_CATEGORIES)
        } else {
            // Guarantee that each category name exists only once, remove duplicates
            val seen = mutableSetOf<String>()
            for (cat in existing) {
                val key = cat.name.trim().lowercase()
                if (!seen.add(key)) {
                    // Duplicate found, delete it from database
                    dao.deleteCategory(cat)
                }
            }
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun insertInstallmentPurchase(
        baseTitle: String,
        totalAmount: Double,
        installments: Int,
        category: String,
        firstDateMillis: Long,
        cardId: Long?,
        notes: String
    ) {
        val installmentAmount = totalAmount / installments
        val transactions = mutableListOf<TransactionEntity>()
        val cal = Calendar.getInstance()

        for (i in 1..installments) {
            cal.timeInMillis = firstDateMillis
            cal.add(Calendar.MONTH, i - 1)

            val titleWithInstallment = if (installments > 1) {
                "$baseTitle ($i/$installments)"
            } else {
                baseTitle
            }

            transactions.add(
                TransactionEntity(
                    title = titleWithInstallment,
                    amount = installmentAmount,
                    type = "EXPENSE",
                    category = category,
                    dateMillis = cal.timeInMillis,
                    paymentMethod = "Cartão de Crédito",
                    cardId = cardId,
                    installmentNumber = i,
                    totalInstallments = installments,
                    notes = notes
                )
            )
        }
        dao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        dao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return dao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        dao.deleteCategory(category)
    }

    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long {
        return dao.insertRecurringBill(bill)
    }

    suspend fun updateRecurringBill(bill: RecurringBillEntity) {
        dao.updateRecurringBill(bill)
    }

    suspend fun deleteRecurringBill(bill: RecurringBillEntity) {
        dao.deleteRecurringBill(bill)
    }

    suspend fun payRecurringBill(bill: RecurringBillEntity, currentMonthYear: String, createTransaction: Boolean) {
        dao.updateRecurringBill(bill.copy(lastPaidMonth = currentMonthYear))
        if (createTransaction) {
            dao.insertTransaction(
                TransactionEntity(
                    title = "Conta: ${bill.name}",
                    amount = bill.amount,
                    type = "EXPENSE",
                    category = bill.category,
                    dateMillis = System.currentTimeMillis(),
                    paymentMethod = "Boleto/Débito",
                    recurringBillId = bill.id,
                    notes = "Pagamento referente a $currentMonthYear"
                )
            )
        }
    }

    suspend fun unpayRecurringBill(bill: RecurringBillEntity) {
        dao.updateRecurringBill(bill.copy(lastPaidMonth = ""))
    }

    suspend fun insertCreditCard(card: CreditCardEntity): Long {
        return dao.insertCreditCard(card)
    }

    suspend fun updateCreditCard(card: CreditCardEntity) {
        dao.updateCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCardEntity) {
        dao.deleteCreditCard(card)
        dao.deleteTransactionsByCardId(card.id)
    }

    suspend fun payCreditCardInvoice(card: CreditCardEntity, currentMonthYear: String, invoiceAmount: Double) {
        dao.updateCreditCard(card.copy(lastPaidMonth = currentMonthYear))
        if (invoiceAmount > 0) {
            dao.insertTransaction(
                TransactionEntity(
                    title = "Pagamento Fatura: ${card.name}",
                    amount = invoiceAmount,
                    type = "EXPENSE",
                    category = "Pagamento de Fatura",
                    dateMillis = System.currentTimeMillis(),
                    paymentMethod = "Débito em Conta",
                    cardId = card.id,
                    notes = "Pagamento de fatura referente a $currentMonthYear",
                    isInvoicePayment = true
                )
            )
        }
    }

    suspend fun unpayCreditCardInvoice(card: CreditCardEntity, currentMonthYear: String) {
        dao.updateCreditCard(card.copy(lastPaidMonth = ""))
        dao.deleteInvoicePaymentTransactionsByCardId(card.id)
    }

    suspend fun insertLoan(loan: LoanEntity): Long {
        return dao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: LoanEntity) {
        dao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanEntity) {
        dao.deleteLoan(loan)
    }

    suspend fun payLoanInstallment(loan: LoanEntity, currentMonthYear: String) {
        val newPaid = loan.paidInstallments + 1
        val newBalance = (loan.currentBalance - loan.installmentAmount).coerceAtLeast(0.0)
        dao.updateLoan(
            loan.copy(
                paidInstallments = newPaid,
                currentBalance = newBalance,
                lastPaidMonth = currentMonthYear
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                title = "Parcela: ${loan.institution} ($newPaid/${loan.totalInstallments})",
                amount = loan.installmentAmount,
                type = "EXPENSE",
                category = "Empréstimos",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Débito em Conta",
                loanId = loan.id,
                installmentNumber = newPaid,
                totalInstallments = loan.totalInstallments,
                notes = "Pagamento de parcela"
            )
        )
    }

    suspend fun amortizeLoan(loan: LoanEntity, amount: Double) {
        val newBalance = (loan.currentBalance - amount).coerceAtLeast(0.0)
        dao.updateLoan(loan.copy(currentBalance = newBalance))
        dao.insertTransaction(
            TransactionEntity(
                title = "Amortização: ${loan.institution}",
                amount = amount,
                type = "EXPENSE",
                category = "Empréstimos",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência",
                loanId = loan.id,
                notes = "Amortização extraordinária do saldo devedor"
            )
        )
    }

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long {
        return dao.insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        dao.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        dao.deleteSavingsGoal(goal)
    }

    suspend fun depositToGoal(goal: SavingsGoalEntity, amount: Double) {
        val newAmount = goal.savedAmount + amount
        dao.updateSavingsGoal(goal.copy(savedAmount = newAmount))
        dao.insertTransaction(
            TransactionEntity(
                title = "Aporte Meta: ${goal.name}",
                amount = amount,
                type = "EXPENSE",
                category = "Investimentos",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência",
                notes = "Depósito na meta financeira"
            )
        )
    }

    suspend fun withdrawFromGoal(goal: SavingsGoalEntity, amount: Double) {
        val newAmount = (goal.savedAmount - amount).coerceAtLeast(0.0)
        dao.updateSavingsGoal(goal.copy(savedAmount = newAmount))
        dao.insertTransaction(
            TransactionEntity(
                title = "Resgate Meta: ${goal.name}",
                amount = amount,
                type = "INCOME",
                category = "Investimentos",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência",
                notes = "Resgate de meta de economia"
            )
        )
    }

    suspend fun saveBudget(category: String, limitAmount: Double) {
        dao.deleteBudgetByCategory(category)
        if (limitAmount > 0) {
            dao.insertBudget(BudgetEntity(category = category, limitAmount = limitAmount))
        }
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        dao.deleteBudget(budget)
    }

    suspend fun clearAllData() {
        dao.clearTransactions()
        dao.clearRecurringBills()
        dao.clearCreditCards()
        dao.clearLoans()
        dao.clearSavingsGoals()
        dao.clearBudgets()
    }
}
