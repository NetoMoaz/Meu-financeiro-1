package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LoanEntity
import com.example.data.RecurringBillEntity
import com.example.data.TransactionEntity
import com.example.viewmodel.CardInvoiceInfo
import com.example.viewmodel.EconomyModeResult
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.ProgressBarCustom
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.AvailableGold
import com.example.ui.theme.AvailableGoldDark
import com.example.ui.theme.CardGradientGreenCenter
import com.example.ui.theme.CardGradientGreenEnd
import com.example.ui.theme.CardGradientGreenStart
import com.example.ui.theme.EconomyCardBorder
import com.example.ui.theme.EconomyCardGreen
import com.example.ui.theme.EconomyCardText
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GreenDarkPrimary
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.GreenLightPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SavedBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftGoldBorder
import com.example.ui.theme.SoftGreenBorder
import com.example.ui.theme.SoftRedBorder
import com.example.viewmodel.EconomyStatus
import com.example.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToPlanning: (String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onQuickAdd: () -> Unit
) {
    val hideValues by viewModel.hideValues.collectAsStateWithLifecycle()
    val overallBalance by viewModel.overallBalance.collectAsStateWithLifecycle()
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalSavedInGoals.collectAsStateWithLifecycle()
    val economyMode by viewModel.economyMode.collectAsStateWithLifecycle()
    val budgetUsage by viewModel.totalBudgetUsage.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
    val cardInvoices by viewModel.cardInvoices.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCal by viewModel.selectedCalendar.collectAsStateWithLifecycle()

    var showAddBillDialog by remember { mutableStateOf(false) }

    val curMonthKey = viewModel.getMonthYearKey(selectedCal)

    val upcomingCommitments = remember(recurringBills, cardInvoices, loans, curMonthKey) {
        val list = mutableListOf<UpcomingCommitmentItem>()

        // 1. Contas fixas / a pagar pendentes no mês
        recurringBills.filter { it.lastPaidMonth != curMonthKey }.forEach { bill ->
            list.add(
                UpcomingCommitmentItem(
                    id = "bill_${bill.id}",
                    name = bill.name,
                    amount = bill.amount,
                    dueDay = bill.dueDay,
                    type = CommitmentType.BILL,
                    category = bill.category,
                    originalBill = bill
                )
            )
        }

        // 2. Faturas de cartão de crédito pendentes no mês com gastos
        cardInvoices.filter { !it.isPaidThisMonth && it.currentInvoiceTotal > 0.0 }.forEach { invoice ->
            list.add(
                UpcomingCommitmentItem(
                    id = "card_${invoice.card.id}",
                    name = "Fatura ${invoice.card.name}",
                    amount = invoice.currentInvoiceTotal,
                    dueDay = invoice.card.dueDay,
                    type = CommitmentType.CARD,
                    category = "Cartão",
                    originalCardInvoice = invoice
                )
            )
        }

        // 3. Parcelas de empréstimos/financiamentos pendentes no mês
        loans.filter { it.lastPaidMonth != curMonthKey && it.paidInstallments < it.totalInstallments && it.installmentAmount > 0.0 }.forEach { loan ->
            list.add(
                UpcomingCommitmentItem(
                    id = "loan_${loan.id}",
                    name = loan.institution,
                    amount = loan.installmentAmount,
                    dueDay = loan.dueDay,
                    type = CommitmentType.LOAN,
                    category = "Empréstimos",
                    originalLoan = loan
                )
            )
        }

        list.sortedWith(compareBy({ it.dueDay }, { it.name }))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list")
            .padding(bottom = 88.dp)
    ) {
        // 1. Grande cartão verde em degradê escrito "Saldo disponível"
        item {
            HeroBalanceGradientCard(
                overallBalance = overallBalance,
                hideValues = hideValues,
                onAddClick = onQuickAdd
            )
        }

        // 2. Quatro pequenos cartões em uma grade 2x2:
        // Entradas em verde; Saídas em vermelho; Economizado em azul; A gastar em amarelo/dourado.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Linha 1: Entradas (verde) & Saídas (vermelho)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("month_income_card"),
                        title = "Entradas",
                        value = monthIncome,
                        hideValues = hideValues,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = IncomeGreen,
                        valueColor = IncomeGreen,
                        borderColor = SoftGreenBorder
                    )
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("month_expense_card"),
                        title = "Saídas",
                        value = monthExpense,
                        hideValues = hideValues,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        iconColor = ExpenseRed,
                        valueColor = ExpenseRed,
                        borderColor = SoftRedBorder
                    )
                }

                // Linha 2: Economizado (azul) & A gastar (amarelo/dourado)
                val saldoRealLivre = economyMode.saldoRealLivre
                val aGastarValue = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
                val deficitAmount = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null
                val deficitText = deficitAmount?.let {
                    "Déficit previsto: ${formatCurrency(it, hideValues)}"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onNavigateToPlanning("METAS") }
                            .testTag("saved_goals_card"),
                        title = "Economizado",
                        value = totalSaved,
                        hideValues = hideValues,
                        icon = Icons.Default.Savings,
                        iconColor = SavedBlue,
                        valueColor = SavedBlue,
                        borderColor = SoftBlueBorder
                    )
                    StatCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onNavigateToPlanning("ECONOMIA") }
                            .testTag("free_to_spend_card"),
                        title = "A gastar",
                        value = aGastarValue,
                        hideValues = hideValues,
                        icon = Icons.Default.LocalAtm,
                        iconColor = AvailableGold,
                        valueColor = AvailableGoldDark,
                        borderColor = SoftGoldBorder,
                        deficitText = deficitText
                    )
                }
            }
        }

        // 3. Resumo do mês, com barra horizontal mostrando o percentual utilizado do orçamento
        item {
            MonthSummaryBudgetCard(
                budgetUsage = budgetUsage,
                monthExpense = monthExpense,
                monthIncome = monthIncome,
                economyMode = economyMode,
                hideValues = hideValues,
                onConfigureBudget = { onNavigateToPlanning(if (budgetUsage.second > 0) "ORCAMENTO" else "ECONOMIA") }
            )
        }

        // 4. Próximas contas / compromissos reais que ainda precisam ser pagos no mês
        item {
            SectionHeader(
                title = "Próximas contas",
                actionLabel = "Ver todas",
                onAction = { onNavigateToPlanning("ECONOMIA") }
            )
        }

        if (upcomingCommitments.isEmpty()) {
            item {
                EmptyUpcomingCommitmentsCard(
                    onAddBill = { showAddBillDialog = true }
                )
            }
        } else {
            items(upcomingCommitments.take(5)) { commitment ->
                WhiteCommitmentItemCard(
                    item = commitment,
                    hideValues = hideValues,
                    onItemClick = {
                        when (commitment.type) {
                            CommitmentType.BILL -> onNavigateToPlanning("CONTAS")
                            CommitmentType.CARD -> onNavigateToPlanning("CARTAO")
                            CommitmentType.LOAN -> onNavigateToPlanning("EMPRESTIMO")
                        }
                    },
                    onPayToggle = {
                        when (commitment.type) {
                            CommitmentType.BILL -> commitment.originalBill?.let { viewModel.toggleBillPaid(it) }
                            CommitmentType.CARD -> commitment.originalCardInvoice?.let {
                                viewModel.toggleCardInvoicePaid(it.card, it.currentInvoiceTotal)
                            }
                            CommitmentType.LOAN -> commitment.originalLoan?.let { viewModel.payLoanInstallment(it) }
                        }
                    }
                )
            }
        }

        // 5. Cartão suave verde-claro para Modo Economia / Dica do dia
        item {
            EconomyModeTipCard(
                economyMode = economyMode,
                hideValues = hideValues,
                onClick = { onNavigateToPlanning("ECONOMIA") }
            )
        }

        // 6. Movimentações Recentes
        item {
            SectionHeader(
                title = "Movimentações Recentes",
                actionLabel = "Ver extrato",
                onAction = onNavigateToTransactions
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhuma movimentação neste mês",
                    subMessage = "Toque no botão central '+' para lançar sua primeira receita ou despesa!",
                    icon = Icons.Default.LocalAtm
                )
            }
        } else {
            items(recentTransactions.take(5)) { tx ->
                WhiteTransactionItemCard(
                    transaction = tx,
                    hideValues = hideValues,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }
    }

    if (showAddBillDialog) {
        AddRecurringBillDialog(
            categories = categories,
            onDismiss = { showAddBillDialog = false },
            onSave = { name, amt, due, freq, cat, notes ->
                viewModel.addRecurringBill(name, amt, due, freq, cat, notes)
                showAddBillDialog = false
            }
        )
    }
}

// Grande cartão verde em degradê escrito "Saldo disponível"
@Composable
fun HeroBalanceGradientCard(
    overallBalance: Double,
    hideValues: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CardGradientGreenStart,
                            CardGradientGreenCenter,
                            CardGradientGreenEnd
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saldo disponível",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Conta & Reservas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Valor grande
                Text(
                    text = formatCurrency(overallBalance, hideValues),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão de ação rápida integrado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onAddClick() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Novo Lançamento Rápido",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.LocalAtm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Resumo do mês, com barra horizontal mostrando o percentual utilizado do orçamento ou balanço do Modo Economia
@Composable
fun MonthSummaryBudgetCard(
    budgetUsage: Pair<Double, Double>, // (percentage, totalBudget)
    monthExpense: Double,
    monthIncome: Double,
    economyMode: EconomyModeResult,
    hideValues: Boolean,
    onConfigureBudget: () -> Unit
) {
    val hasBudget = budgetUsage.second > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(18.dp))
            .clickable { onConfigureBudget() }
            .testTag("budget_usage_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val saldoRealLivre = economyMode.saldoRealLivre
            val totalCompromissos = economyMode.contasNaoPagas +
                    economyMode.faturasCartao +
                    economyMode.parcelasEmprestimo +
                    economyMode.metasAporte

            val (subtitle, badgeText, statusColor) = if (hasBudget) {
                val percent = budgetUsage.first
                val color = when {
                    percent > 100f -> ExpenseRed
                    percent >= 80f -> AlertOrange
                    else -> IncomeGreen
                }
                Triple("Consumo do teto orçamentário", "${percent.toInt()}% usado", color)
            } else {
                when {
                    saldoRealLivre < 0 -> Triple(
                        "Compromissos superam o saldo",
                        "Atenção",
                        ExpenseRed
                    )
                    saldoRealLivre == 0.0 -> Triple(
                        "Compromissos cobertos pelo saldo",
                        "Equilibrado",
                        AlertOrange
                    )
                    else -> Triple(
                        "Saldo real livre para o mês",
                        "Saldo livre",
                        IncomeGreen
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = GreenDarkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Resumo do mês",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("month_summary_badge")
                ) {
                    Text(
                        text = badgeText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra horizontal mostrando o percentual utilizado
            val progressFraction: Float = if (hasBudget) {
                (budgetUsage.first / 100.0).toFloat().coerceIn(0f, 1f)
            } else {
                val saldoConta = economyMode.saldoDisponivel
                if (saldoConta > 0) {
                    (totalCompromissos / saldoConta).toFloat().coerceIn(0f, 1f)
                } else {
                    if (totalCompromissos > 0) 1f else 0f
                }
            }

            val barColor = if (hasBudget) {
                when {
                    budgetUsage.first > 100f -> ExpenseRed
                    budgetUsage.first >= 80f -> AlertOrange
                    else -> IncomeGreen
                }
            } else {
                when {
                    saldoRealLivre < 0 -> ExpenseRed
                    saldoRealLivre == 0.0 -> AlertOrange
                    else -> IncomeGreen
                }
            }

            ProgressBarCustom(
                progress = progressFraction,
                barColor = barColor,
                modifier = Modifier.height(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasBudget) {
                    Text(
                        text = "Gasto: ${formatCurrency(monthExpense, hideValues)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("month_summary_left_text")
                    )
                    Text(
                        text = "Teto: ${formatCurrency(budgetUsage.second, hideValues)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("month_summary_right_text")
                    )
                } else {
                    when {
                        saldoRealLivre < 0 -> {
                            Text(
                                text = "A gastar: ${formatCurrency(0.0, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("month_summary_left_text")
                            )
                            val deficitAbs = kotlin.math.abs(saldoRealLivre)
                            Text(
                                text = "Déficit previsto: ${formatCurrency(deficitAbs, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                modifier = Modifier.testTag("month_summary_right_text")
                            )
                        }
                        saldoRealLivre == 0.0 -> {
                            Text(
                                text = "A gastar: ${formatCurrency(0.0, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("month_summary_left_text")
                            )
                            Text(
                                text = "Compromissos cobertos (R$ 0,00 livre)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AlertOrange,
                                modifier = Modifier.testTag("month_summary_right_text")
                            )
                        }
                        else -> {
                            Text(
                                text = "A gastar: ${formatCurrency(saldoRealLivre, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen,
                                modifier = Modifier.testTag("month_summary_left_text")
                            )
                            Text(
                                text = "Compromissos: ${formatCurrency(totalCompromissos, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("month_summary_right_text")
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class CommitmentType(val label: String) {
    BILL("Conta"),
    CARD("Cartão"),
    LOAN("Empréstimo")
}

data class UpcomingCommitmentItem(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val type: CommitmentType,
    val category: String = "",
    val originalBill: RecurringBillEntity? = null,
    val originalCardInvoice: CardInvoiceInfo? = null,
    val originalLoan: LoanEntity? = null
)

// Card discreto e elegante para o estado vazio da seção "Próximas contas"
@Composable
fun EmptyUpcomingCommitmentsCard(
    onAddBill: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(16.dp))
            .testTag("empty_upcoming_commitments_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nenhuma conta próxima",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Suas contas estão em dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onAddBill,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GreenLightPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGreenBorder),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("empty_upcoming_add_bill_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GreenLightPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cadastrar conta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenLightPrimary
                )
            }
        }
    }
}

// Próximas contas / compromissos: cartões brancos individuais contendo ícone, nome, vencimento, tipo e valor
@Composable
fun WhiteCommitmentItemCard(
    item: UpcomingCommitmentItem,
    hideValues: Boolean,
    onItemClick: () -> Unit,
    onPayToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(16.dp))
            .clickable { onItemClick() }
            .testTag("commitment_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Ícone circular colorido de acordo com o tipo
                val (iconVector, iconColor, iconBg) = when (item.type) {
                    CommitmentType.BILL -> Triple(
                        getCategoryIcon(item.category),
                        ExpenseRed,
                        ExpenseRed.copy(alpha = 0.1f)
                    )
                    CommitmentType.CARD -> Triple(
                        Icons.Default.CreditCard,
                        AvailableGoldDark,
                        AvailableGold.copy(alpha = 0.15f)
                    )
                    CommitmentType.LOAN -> Triple(
                        Icons.Default.AccountBalance,
                        AlertOrange,
                        AlertOrange.copy(alpha = 0.12f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Vence dia ${item.dueDay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (item.type) {
                                        CommitmentType.BILL -> MaterialTheme.colorScheme.surfaceVariant
                                        CommitmentType.CARD -> AvailableGold.copy(alpha = 0.18f)
                                        CommitmentType.LOAN -> AlertOrange.copy(alpha = 0.14f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.type.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = when (item.type) {
                                    CommitmentType.BILL -> MaterialTheme.colorScheme.onSurfaceVariant
                                    CommitmentType.CARD -> AvailableGoldDark
                                    CommitmentType.LOAN -> AlertOrange
                                }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(item.amount, hideValues),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed,
                    modifier = Modifier.testTag("commitment_amount_${item.id}")
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onPayToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("pay_toggle_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Marcar como pago",
                        tint = GreenDarkPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// Cartão suave verde-claro para Modo Economia / Dica do dia
@Composable
fun EconomyModeTipCard(
    economyMode: com.example.viewmodel.EconomyModeResult,
    hideValues: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, EconomyCardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("economy_mode_home_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EconomyCardGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF047857).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Modo Economia • Dica do dia",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EconomyCardText
                        )
                        Text(
                            text = economyMode.mensagemStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = EconomyCardText.copy(alpha = 0.8f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ver detalhes",
                        color = EconomyCardText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dica do dia
            Text(
                text = economyMode.dica,
                style = MaterialTheme.typography.bodyMedium,
                color = EconomyCardText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Limite diário e semanal sugerido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Disponível por dia: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(economyMode.disponivelPorDia, hideValues),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EconomyCardText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Por semana: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(economyMode.disponivelPorSemana, hideValues),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EconomyCardText
                    )
                }
            }
        }
    }
}

// Cartão branco individual para movimentação recente
@Composable
fun WhiteTransactionItemCard(
    transaction: TransactionEntity,
    hideValues: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val isIncome = transaction.type == "INCOME"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) IncomeGreen.copy(alpha = 0.12f)
                            else ExpenseRed.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category),
                        contentDescription = null,
                        tint = if (isIncome) IncomeGreen else ExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • ${dateFormatter.format(transaction.dateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isIncome) "+ " else "- "
                Text(
                    text = "$prefix${formatCurrency(transaction.amount, hideValues)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.paymentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
