package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BudgetEntity
import com.example.data.CategoryEntity
import com.example.data.CreditCardEntity
import com.example.data.FinanceRepository
import com.example.data.LoanEntity
import com.example.data.RecurringBillEntity
import com.example.data.SavingsGoalEntity
import com.example.data.TransactionEntity
import com.example.data.isCreditCardInvoicePayment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("meu_financeiro_prefs", Context.MODE_PRIVATE)
    private val repository: FinanceRepository

    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    private val _hideValues = MutableStateFlow(prefs.getBoolean("hide_values", false))
    val hideValues: StateFlow<Boolean> = _hideValues.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _biometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_profile_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhotoPath = MutableStateFlow(
        prefs.getString("user_profile_photo", null)?.takeIf { path ->
            try { File(path).exists() } catch (_: Exception) { false }
        }
    )
    val userPhotoPath: StateFlow<String?> = _userPhotoPath.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = FinanceRepository(db.financeDao())
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
        val savedPhoto = prefs.getString("user_profile_photo", null)
        if (savedPhoto != null && !File(savedPhoto).exists()) {
            prefs.edit().remove("user_profile_photo").apply()
            _userPhotoPath.value = null
        }
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringBills: StateFlow<List<RecurringBillEntity>> = repository.allRecurringBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions on Calendar Navigation
    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedCalendar.value.timeInMillis
            add(Calendar.MONTH, -1)
        }
        _selectedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedCalendar.value.timeInMillis
            add(Calendar.MONTH, 1)
        }
        _selectedCalendar.value = cal
    }

    fun currentMonth() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    fun toggleHideValues() {
        val newVal = !_hideValues.value
        _hideValues.value = newVal
        prefs.edit().putBoolean("hide_values", newVal).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        _biometricsEnabled.value = enabled
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    fun saveProfile(name: String, photoPath: String?) {
        val trimmed = name.trim()
        val oldPath = _userPhotoPath.value

        // Only delete the old file if it actually changed and differs from the new path
        if (oldPath != null && oldPath != photoPath) {
            try {
                val oldFile = File(oldPath)
                if (oldFile.exists()) oldFile.delete()
            } catch (_: Exception) {}
        }

        _userName.value = trimmed
        _userPhotoPath.value = photoPath
        val editor = prefs.edit()
        editor.putString("user_profile_name", trimmed)
        if (photoPath != null) {
            editor.putString("user_profile_photo", photoPath)
        } else {
            editor.remove("user_profile_photo")
        }
        editor.apply()
    }

    fun removeProfilePhoto() {
        val oldPath = _userPhotoPath.value
        _userPhotoPath.value = null
        prefs.edit().remove("user_profile_photo").apply()
        if (oldPath != null) {
            try {
                val f = File(oldPath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
    }

    fun savePhotoFromUri(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val destinationFile = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destinationFile.exists() && destinationFile.length() > 0) {
                destinationFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getUserInitials(name: String = _userName.value): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "MF"
        val parts = trimmed.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.isNotEmpty() -> parts[0].take(1).uppercase()
            else -> "MF"
        }
    }

    // Helper month-year string: "2026-09"
    fun getMonthYearKey(cal: Calendar = _selectedCalendar.value): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getMonthYearDisplay(cal: Calendar = _selectedCalendar.value): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        val str = sdf.format(cal.time)
        return str.replaceFirstChar { it.uppercase() }
    }

    // Helper to check if a transaction represents an invoice payment
    fun isInvoicePayment(tx: TransactionEntity): Boolean = tx.isCreditCardInvoicePayment()

    // Database Actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        dateMillis: Long,
        paymentMethod: String,
        cardId: Long?,
        installments: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val isInvPayment = category.trim().equals("Pagamento de Fatura", ignoreCase = true) ||
                    title.trim().startsWith("Pagamento Fatura", ignoreCase = true) ||
                    title.trim().startsWith("Pagamento de Fatura", ignoreCase = true) ||
                    paymentMethod.trim().equals("Pagamento de Fatura", ignoreCase = true)

            if (paymentMethod == "Cartão de Crédito" && installments > 1 && !isInvPayment) {
                repository.insertInstallmentPurchase(
                    baseTitle = title,
                    totalAmount = amount,
                    installments = installments,
                    category = category,
                    firstDateMillis = dateMillis,
                    cardId = cardId,
                    notes = notes
                )
            } else {
                repository.insertTransaction(
                    TransactionEntity(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateMillis = dateMillis,
                        paymentMethod = paymentMethod,
                        cardId = cardId,
                        notes = notes,
                        isInvoicePayment = isInvPayment
                    )
                )
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val isInv = transaction.isInvoicePayment ||
                    transaction.category.trim().equals("Pagamento de Fatura", ignoreCase = true) ||
                    transaction.title.trim().startsWith("Pagamento Fatura", ignoreCase = true) ||
                    transaction.paymentMethod.trim().equals("Pagamento de Fatura", ignoreCase = true)
            repository.updateTransaction(transaction.copy(isInvoicePayment = isInv))
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addCategory(name: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = false
                )
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addRecurringBill(name: String, amount: Double, dueDay: Int, frequency: String, category: String, notes: String) {
        viewModelScope.launch {
            repository.insertRecurringBill(
                RecurringBillEntity(
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    frequency = frequency,
                    category = category,
                    notes = notes
                )
            )
        }
    }

    fun updateRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.updateRecurringBill(bill)
        }
    }

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
        }
    }

    fun toggleBillPaid(bill: RecurringBillEntity) {
        viewModelScope.launch {
            val currentMonth = getMonthYearKey()
            if (bill.lastPaidMonth == currentMonth) {
                repository.unpayRecurringBill(bill)
            } else {
                repository.payRecurringBill(bill, currentMonth, createTransaction = true)
            }
        }
    }

    fun addCreditCard(name: String, limit: Double, closingDay: Int, dueDay: Int, colorHex: Long, lastFour: String) {
        viewModelScope.launch {
            repository.insertCreditCard(
                CreditCardEntity(
                    name = name,
                    limitAmount = limit,
                    closingDay = closingDay,
                    dueDay = dueDay,
                    colorHex = colorHex,
                    lastFourDigits = lastFour
                )
            )
        }
    }

    fun updateCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.updateCreditCard(card)
        }
    }

    fun deleteCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
        }
    }

    fun toggleCardInvoicePaid(card: CreditCardEntity, invoiceAmount: Double) {
        viewModelScope.launch {
            val currentMonth = getMonthYearKey()
            if (card.lastPaidMonth == currentMonth) {
                repository.unpayCreditCardInvoice(card, currentMonth)
            } else {
                repository.payCreditCardInvoice(card, currentMonth, invoiceAmount)
            }
        }
    }

    fun addLoan(
        institution: String,
        originalAmount: Double?,
        currentBalance: Double,
        installmentAmount: Double,
        totalInstallments: Int,
        paidInstallments: Int,
        interestRate: Double?,
        dueDay: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertLoan(
                LoanEntity(
                    institution = institution,
                    originalAmount = originalAmount,
                    currentBalance = currentBalance,
                    installmentAmount = installmentAmount,
                    totalInstallments = totalInstallments,
                    paidInstallments = paidInstallments,
                    interestRate = interestRate,
                    dueDay = dueDay,
                    notes = notes
                )
            )
        }
    }

    fun updateLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.updateLoan(loan)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun payLoanInstallment(loan: LoanEntity) {
        viewModelScope.launch {
            repository.payLoanInstallment(loan, getMonthYearKey())
        }
    }

    fun amortizeLoan(loan: LoanEntity, amount: Double) {
        viewModelScope.launch {
            repository.amortizeLoan(loan, amount)
        }
    }

    fun addSavingsGoal(name: String, targetAmount: Double, initialSaved: Double, targetDateMillis: Long, notes: String) {
        viewModelScope.launch {
            repository.insertSavingsGoal(
                SavingsGoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    savedAmount = initialSaved,
                    targetDateMillis = targetDateMillis,
                    notes = notes
                )
            )
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun depositToGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            repository.depositToGoal(goal, amount)
        }
    }

    fun withdrawFromGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            repository.withdrawFromGoal(goal, amount)
        }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(category, limit)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.ensureDefaultCategories()
        }
    }

    // ===============================================
    // Reactive Calculations and Computed Summaries
    // ===============================================

    // Filtered transactions for selected month
    val currentMonthTransactions = combine(transactions, selectedCalendar) { txList, cal ->
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val itemCal = Calendar.getInstance()
        txList.filter { tx ->
            itemCal.timeInMillis = tx.dateMillis
            itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthIncome = currentMonthTransactions.combine(currentMonthTransactions) { txList, _ ->
        txList.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Despesas do mês representam o consumo mensal total (inclui compras no cartão de crédito,
    // mas EXCLUI pagamento de fatura para não gerar contabilização dupla).
    val monthExpense = currentMonthTransactions.combine(currentMonthTransactions) { txList, _ ->
        txList.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Saldo Disponível em Conta:
    // Total de entradas menos despesas pagas imediatamente da conta (Pix, Dinheiro, Débito, Boleto, etc.)
    // e pagamentos de faturas efetivados. Compras no cartão de crédito pendentes NÃO reduzem o saldo em conta.
    val overallBalance = transactions.combine(transactions) { txList, _ ->
        val inc = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expFromAccount = txList.filter { tx ->
            tx.type == "EXPENSE" && tx.paymentMethod != "Cartão de Crédito"
        }.sumOf { it.amount }
        inc - expFromAccount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSavedInGoals = savingsGoals.combine(savingsGoals) { goals, _ ->
        goals.sumOf { it.savedAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Expense Breakdown for current month (apenas consumo, sem pagamento de fatura duplicado)
    val categoryExpenses = combine(currentMonthTransactions, categories) { txList, cats ->
        val expenseTx = txList.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }
        val totalExp = expenseTx.sumOf { it.amount }
        if (totalExp <= 0.0) {
            emptyList()
        } else {
            expenseTx.groupBy { it.category }.map { (catName, list) ->
                val amount = list.sumOf { it.amount }
                val catEntity = cats.find { it.name.equals(catName, ignoreCase = true) }
                val colorHex = catEntity?.colorHex ?: 0xFF10B981
                val iconName = catEntity?.iconName ?: "category"
                val pct = ((amount / totalExp) * 100).toFloat()
                CategoryExpense(
                    category = catName,
                    amount = amount,
                    percentage = pct,
                    colorHex = colorHex,
                    iconName = iconName
                )
            }.sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Progress for current month
    val budgetProgressList = combine(currentMonthTransactions, budgets) { txList, budgetList ->
        val expenseTx = txList.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }
        val spentByCategory = expenseTx.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        budgetList.map { b ->
            val spent = spentByCategory[b.category] ?: 0.0
            val pct = if (b.limitAmount > 0) ((spent / b.limitAmount) * 100).toFloat() else 0f
            BudgetProgress(
                category = b.category,
                limitAmount = b.limitAmount,
                spentAmount = spent,
                percentage = pct,
                isOverBudget = spent > b.limitAmount,
                isNearBudget = spent >= b.limitAmount * 0.8 && spent <= b.limitAmount
            )
        }.sortedByDescending { it.percentage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Budget usage %
    val totalBudgetUsage = combine(budgetProgressList, monthExpense) { progressList, _ ->
        val totalBudget = progressList.sumOf { it.limitAmount }
        if (totalBudget > 0) {
            val totalSpentInBudgets = progressList.sumOf { it.spentAmount }
            val pct = (totalSpentInBudgets / totalBudget) * 100
            Pair(pct.coerceIn(0.0, 999.0), totalBudget)
        } else {
            Pair(0.0, 0.0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Card Invoices
    val cardInvoices = combine(creditCards, currentMonthTransactions, selectedCalendar) { cards, txList, cal ->
        val curMonthKey = getMonthYearKey(cal)
        cards.map { card ->
            val cardTx = txList.filter { 
                it.cardId == card.id && 
                it.paymentMethod == "Cartão de Crédito" && 
                !it.isCreditCardInvoicePayment() 
            }
            val invoiceTotal = cardTx.sumOf { it.amount }
            val isPaid = card.lastPaidMonth == curMonthKey || 
                    txList.any { it.isCreditCardInvoicePayment() && it.cardId == card.id }
            val available = (card.limitAmount - if (isPaid) 0.0 else invoiceTotal).coerceAtLeast(0.0)
            CardInvoiceInfo(
                card = card,
                currentInvoiceTotal = invoiceTotal,
                availableLimit = available,
                upcomingInstallmentsCount = cardTx.count { it.totalInstallments > 1 },
                isPaidThisMonth = isPaid
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCardInvoicesTotal = combine(cardInvoices, currentMonthTransactions) { invoices, txList ->
        val fromCards = invoices.filter { !it.isPaidThisMonth }.sumOf { it.currentInvoiceTotal }
        val hasUnassignedInvoicePayment = txList.any { 
            it.isCreditCardInvoicePayment() && it.cardId == null
        }
        val unassigned = if (hasUnassignedInvoicePayment) {
            0.0
        } else {
            txList.filter { 
                it.paymentMethod == "Cartão de Crédito" && 
                it.cardId == null && 
                !it.isCreditCardInvoicePayment() 
            }.sumOf { it.amount }
        }
        fromCards + unassigned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Loans Summary
    val loanSummary = loans.combine(loans) { loanList, _ ->
        val totalDebt = loanList.sumOf { it.currentBalance }
        val monthly = loanList.sumOf { it.installmentAmount }
        val count = loanList.size
        val estMonths = if (count > 0 && monthly > 0) (totalDebt / monthly).roundToInt() else 0
        LoanSummary(
            totalDebt = totalDebt,
            totalMonthlyInstallments = monthly,
            loansCount = count,
            estimatedMonthsToFree = estMonths
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanSummary(0.0, 0.0, 0, 0))

    // Monthly commitments calculated from flows (5 flows)
    private val commitmentsSummary = combine(
        recurringBills,
        loans,
        pendingCardInvoicesTotal,
        savingsGoals,
        selectedCalendar
    ) { bills, loanList, cardExpense, goals, cal ->
        val curMonthKey = getMonthYearKey(cal)
        val now = Calendar.getInstance()
        val isCurrentMonth = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

        // Contas não pagas neste mês
        val unpaidBills = bills.filter { it.lastPaidMonth != curMonthKey }.sumOf { it.amount }

        // Parcelas de empréstimos do mês pendentes
        val pendingLoans = loanList.filter { it.lastPaidMonth != curMonthKey }
            .filter { it.paidInstallments < it.totalInstallments }
            .sumOf { it.installmentAmount }

        // Metas mensais sugeridas (aporte recomendado)
        val goalsTarget = goals.filter { it.savedAmount < it.targetAmount }.sumOf { g ->
            val diff = (g.targetAmount - g.savedAmount).coerceAtLeast(0.0)
            val calTarget = Calendar.getInstance().apply { timeInMillis = g.targetDateMillis }
            val targetYear = calTarget.get(Calendar.YEAR)
            val targetMonth = calTarget.get(Calendar.MONTH)
            val curYear = now.get(Calendar.YEAR)
            val curMonth = now.get(Calendar.MONTH)
            val monthsDiff = max(1, (targetYear - curYear) * 12 + (targetMonth - curMonth))
            diff / monthsDiff
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else 1
        val diasRestantes = max(1, daysInMonth - currentDay + 1)
        val semanasRestantes = max(1.0, diasRestantes / 7.0)

        CommitmentTotals(
            unpaidBills = unpaidBills,
            cardExpense = cardExpense,
            pendingLoans = pendingLoans,
            goalsTarget = goalsTarget,
            diasRestantes = diasRestantes,
            semanasRestantes = semanasRestantes
        )
    }

    // Modo Economia Engine Calculation:
    // Parte do Saldo Disponível em Conta e desconta os compromissos pendentes do mês.
    val economyMode = combine(
        overallBalance,
        commitmentsSummary
    ) { balance, commitments ->
        val saldoDisponivelAtual = kotlin.math.max(0.0, balance)
        val compromissosFuturos = commitments.unpaidBills + commitments.cardExpense + commitments.pendingLoans + commitments.goalsTarget
        val saldoRealLivre = saldoDisponivelAtual - compromissosFuturos

        val livrePositivo = kotlin.math.max(0.0, saldoRealLivre)
        val disponivelPorDia = livrePositivo / commitments.diasRestantes
        val disponivelPorSemana = livrePositivo / commitments.semanasRestantes

        val status: EconomyStatus
        val msg: String
        val dica: String

        if (saldoRealLivre > 500.0) {
            status = EconomyStatus.OTIMO
            msg = "Situação Confortável"
            dica = "Seu orçamento está equilibrado com R$ ${String.format(Locale("pt", "BR"), "%.2f", disponivelPorDia)} livres por dia. Considere destinar parte dessa sobra para suas metas de economia."
        } else if (saldoRealLivre >= 0.0) {
            status = EconomyStatus.ATENCAO
            msg = "Atenção ao Orçamento"
            dica = "Seus compromissos consomem quase todo o saldo. Limite os gastos diários a R$ ${String.format(Locale("pt", "BR"), "%.2f", disponivelPorDia)} para fechar o mês no azul."
        } else {
            status = EconomyStatus.CRITICO
            msg = "Déficit Previsto"
            dica = "Seus compromissos (contas, cartões e parcelas) superam seu saldo em R$ ${String.format(Locale("pt", "BR"), "%.2f", -saldoRealLivre)}. Evite novos gastos com cartão e priorize as contas essenciais."
        }

        EconomyModeResult(
            saldoDisponivel = saldoDisponivelAtual,
            contasNaoPagas = commitments.unpaidBills,
            faturasCartao = commitments.cardExpense,
            parcelasEmprestimo = commitments.pendingLoans,
            metasAporte = commitments.goalsTarget,
            saldoRealLivre = saldoRealLivre,
            diasRestantes = commitments.diasRestantes,
            disponivelPorDia = disponivelPorDia,
            disponivelPorSemana = disponivelPorSemana,
            status = status,
            mensagemStatus = msg,
            dica = dica
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EconomyModeResult(
            saldoDisponivel = 0.0,
            contasNaoPagas = 0.0,
            faturasCartao = 0.0,
            parcelasEmprestimo = 0.0,
            metasAporte = 0.0,
            saldoRealLivre = 0.0,
            diasRestantes = 30,
            disponivelPorDia = 0.0,
            disponivelPorSemana = 0.0,
            status = EconomyStatus.OTIMO,
            mensagemStatus = "Pronto para começar",
            dica = "Cadastre suas contas e receitas para ativar o cálculo inteligente do Modo Economia."
        )
    )

    // Upcoming Commitments (Próximas contas)
    val upcomingCommitments = combine(
        recurringBills,
        loans,
        creditCards,
        selectedCalendar
    ) { bills, loanList, cards, cal ->
        val curMonthKey = getMonthYearKey(cal)
        val list = mutableListOf<CalendarDayItem>()

        bills.forEach { b ->
            list.add(
                CalendarDayItem(
                    day = b.dueDay,
                    title = "Conta: ${b.name}",
                    amount = b.amount,
                    type = "BILL",
                    isPaid = b.lastPaidMonth == curMonthKey,
                    category = b.category
                )
            )
        }

        loanList.forEach { l ->
            list.add(
                CalendarDayItem(
                    day = l.dueDay,
                    title = "Empréstimo: ${l.institution}",
                    amount = l.installmentAmount,
                    type = "LOAN",
                    isPaid = l.lastPaidMonth == curMonthKey || l.paidInstallments >= l.totalInstallments,
                    category = "Empréstimos"
                )
            )
        }

        cards.forEach { c ->
            list.add(
                CalendarDayItem(
                    day = c.dueDay,
                    title = "Fatura: ${c.name}",
                    amount = 0.0,
                    type = "CARD",
                    isPaid = c.lastPaidMonth == curMonthKey,
                    category = "Cartão de Crédito"
                )
            )
        }

        list.sortedBy { it.day }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar Items mapped by day of month
    val calendarItems = combine(
        currentMonthTransactions,
        upcomingCommitments
    ) { txList, commitments ->
        val map = mutableMapOf<Int, MutableList<CalendarDayItem>>()
        val itemCal = Calendar.getInstance()

        txList.forEach { tx ->
            itemCal.timeInMillis = tx.dateMillis
            val day = itemCal.get(Calendar.DAY_OF_MONTH)
            val list = map.getOrPut(day) { mutableListOf() }
            list.add(
                CalendarDayItem(
                    day = day,
                    title = tx.title,
                    amount = tx.amount,
                    type = tx.type,
                    isPaid = true,
                    category = tx.category
                )
            )
        }

        commitments.forEach { c ->
            val list = map.getOrPut(c.day) { mutableListOf() }
            if (list.none { it.title == c.title }) {
                list.add(c)
            }
        }

        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Month History (past 6 months) for Reports
    val monthHistory = transactions.combine(selectedCalendar) { txList, _ ->
        val list = mutableListOf<MonthSummary>()
        val cal = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("MMM/yy", Locale("pt", "BR"))

        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.MONTH, -i)
            }
            val m = c.get(Calendar.MONTH)
            val y = c.get(Calendar.YEAR)
            val key = sdfKey.format(c.time)
            val label = sdfDisplay.format(c.time).replaceFirstChar { it.uppercase() }

            val itemCal = Calendar.getInstance()
            val mTx = txList.filter {
                itemCal.timeInMillis = it.dateMillis
                itemCal.get(Calendar.MONTH) == m && itemCal.get(Calendar.YEAR) == y
            }
            val inc = mTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = mTx.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }

            list.add(
                MonthSummary(
                    yearMonth = key,
                    displayLabel = label,
                    income = inc,
                    expense = exp
                )
            )
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
