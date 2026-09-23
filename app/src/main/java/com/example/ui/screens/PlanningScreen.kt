package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CreditCardEntity
import com.example.data.LoanEntity
import com.example.data.RecurringBillEntity
import com.example.data.SavingsGoalEntity
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.ProgressBarCustom
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GreenDarkPrimary
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.IncomeGreen
import com.example.viewmodel.BudgetProgress
import com.example.viewmodel.CardInvoiceInfo
import com.example.viewmodel.EconomyModeResult
import com.example.viewmodel.EconomyStatus
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.LoanSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PlanningScreen(
    viewModel: FinanceViewModel,
    initialTabKey: String? = null
) {
    val hideValues by viewModel.hideValues.collectAsStateWithLifecycle()
    val economyMode by viewModel.economyMode.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    val cardInvoices by viewModel.cardInvoices.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loanSummary by viewModel.loanSummary.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCal by viewModel.selectedCalendar.collectAsStateWithLifecycle()

    val tabs = listOf(
        "ECONOMIA" to "Modo Economia",
        "ORCAMENTO" to "Orçamentos",
        "METAS" to "Metas",
        "CARTAO" to "Cartões",
        "EMPRESTIMO" to "Empréstimos",
        "CONTAS" to "Contas Fixas"
    )

    var selectedTabIndex by remember {
        val idx = tabs.indexOfFirst { it.first == initialTabKey }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    // Dialog controllers
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var amortizeLoanTarget by remember { mutableStateOf<LoanEntity?>(null) }
    var depositGoalTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("planning_screen")
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = Color.White,
            contentColor = Color(0xFF047857),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF047857)
                )
            },
            modifier = Modifier.border(0.5.dp, GreenLightCardBorder)
        ) {
            tabs.forEachIndexed { index, (_, title) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) Color(0xFF047857) else Color(0xFF64748B)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (tabs[selectedTabIndex].first) {
            "ECONOMIA" -> EconomyModeView(economyMode, hideValues)
            "ORCAMENTO" -> BudgetsView(
                budgetProgress = budgetProgress,
                hideValues = hideValues,
                onAddBudget = { showSetBudgetDialog = true },
                onDeleteBudget = { b -> viewModel.setBudget(b.category, 0.0) }
            )
            "METAS" -> SavingsGoalsView(
                goals = savingsGoals,
                hideValues = hideValues,
                onAddGoal = { showAddGoalDialog = true },
                onDeposit = { goal -> depositGoalTarget = goal },
                onDelete = { goal -> viewModel.deleteSavingsGoal(goal) }
            )
            "CARTAO" -> CreditCardsView(
                cardInvoices = cardInvoices,
                hideValues = hideValues,
                onAddCard = { showAddCardDialog = true },
                onDeleteCard = { card -> viewModel.deleteCreditCard(card) },
                onTogglePaid = { card, amount -> viewModel.toggleCardInvoicePaid(card, amount) }
            )
            "EMPRESTIMO" -> LoansView(
                loans = loans,
                summary = loanSummary,
                hideValues = hideValues,
                onAddLoan = { showAddLoanDialog = true },
                onPayInstallment = { loan -> viewModel.payLoanInstallment(loan) },
                onAmortize = { loan -> amortizeLoanTarget = loan },
                onDelete = { loan -> viewModel.deleteLoan(loan) }
            )
            "CONTAS" -> RecurringBillsView(
                bills = recurringBills,
                hideValues = hideValues,
                curMonthKey = viewModel.getMonthYearKey(selectedCal),
                onAddBill = { showAddBillDialog = true },
                onTogglePaid = { bill -> viewModel.toggleBillPaid(bill) },
                onDelete = { bill -> viewModel.deleteRecurringBill(bill) }
            )
        }
    }

    // Dialogs Render
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

    if (showAddCardDialog) {
        AddCreditCardDialog(
            onDismiss = { showAddCardDialog = false },
            onSave = { name, limit, close, due, color, four ->
                viewModel.addCreditCard(name, limit, close, due, color, four)
                showAddCardDialog = false
            }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onSave = { inst, orig, bal, instAmt, total, paid, rate, due, notes ->
                viewModel.addLoan(inst, orig, bal, instAmt, total, paid, rate, due, notes)
                showAddLoanDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { name, target, initSaved, date, notes ->
                viewModel.addSavingsGoal(name, target, initSaved, date, notes)
                showAddGoalDialog = false
            }
        )
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            categories = categories,
            onDismiss = { showSetBudgetDialog = false },
            onSave = { cat, limit ->
                viewModel.setBudget(cat, limit)
                showSetBudgetDialog = false
            }
        )
    }

    if (amortizeLoanTarget != null) {
        AmortizeLoanDialog(
            loanName = amortizeLoanTarget!!.institution,
            currentBalance = amortizeLoanTarget!!.currentBalance,
            onDismiss = { amortizeLoanTarget = null },
            onConfirm = { amt ->
                viewModel.amortizeLoan(amortizeLoanTarget!!, amt)
                amortizeLoanTarget = null
            }
        )
    }

    if (depositGoalTarget != null) {
        QuickDepositGoalDialog(
            goalName = depositGoalTarget!!.name,
            onDismiss = { depositGoalTarget = null },
            onConfirm = { amt ->
                viewModel.depositToGoal(depositGoalTarget!!, amt)
                depositGoalTarget = null
            }
        )
    }
}

// -------------------------------------------------------------
// Sub-views
// -------------------------------------------------------------

@Composable
fun EconomyModeView(economy: EconomyModeResult, hideValues: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("economy_mode_detail_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Modo Economia Ativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (economy.status) {
                                        EconomyStatus.OTIMO -> IncomeGreen.copy(alpha = 0.2f)
                                        EconomyStatus.ATENCAO -> AlertOrange.copy(alpha = 0.2f)
                                        EconomyStatus.CRITICO -> ExpenseRed.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = economy.mensagemStatus,
                                color = when (economy.status) {
                                    EconomyStatus.OTIMO -> IncomeGreen
                                    EconomyStatus.ATENCAO -> AlertOrange
                                    EconomyStatus.CRITICO -> ExpenseRed
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Saldo Real Livre:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(economy.saldoRealLivre, hideValues),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (economy.saldoRealLivre < 0) ExpenseRed else GreenDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Livre por Dia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(economy.disponivelPorDia, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text("${economy.diasRestantes} dias restantes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Livre por Semana", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(economy.disponivelPorSemana, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text("Sem comprometer metas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = economy.dica,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Como chegamos a esse cálculo:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BreakdownRow("Saldo Disponível em Conta", "+ ${formatCurrency(economy.saldoDisponivel, hideValues)}", IncomeGreen)
                    BreakdownRow("Contas a Pagar no Mês", "- ${formatCurrency(economy.contasNaoPagas, hideValues)}", ExpenseRed)
                    BreakdownRow("Faturas Cartão no Mês", "- ${formatCurrency(economy.faturasCartao, hideValues)}", ExpenseRed)
                    BreakdownRow("Parcelas de Empréstimos", "- ${formatCurrency(economy.parcelasEmprestimo, hideValues)}", ExpenseRed)
                    BreakdownRow("Aportes Desejados em Metas", "- ${formatCurrency(economy.metasAporte, hideValues)}", GoldPrimary)
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun BudgetsView(
    budgetProgress: List<BudgetProgress>,
    hideValues: Boolean,
    onAddBudget: () -> Unit,
    onDeleteBudget: (BudgetProgress) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teto de Gastos por Categoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddBudget,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Definir Limite")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (budgetProgress.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhum orçamento configurado",
                    subMessage = "Defina limites mensais para controlar seus gastos em alimentação, lazer, compras etc.",
                    icon = Icons.Default.PieChart
                )
            }
        } else {
            items(budgetProgress) { b ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = b.category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Limite: ${formatCurrency(b.limitAmount, hideValues)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteBudget(b) }, modifier = Modifier.size(48.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir orçamento", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val barColor = when {
                            b.isOverBudget -> ExpenseRed
                            b.isNearBudget -> AlertOrange
                            else -> GreenDarkPrimary
                        }

                        ProgressBarCustom(
                            progress = (b.percentage / 100f).coerceIn(0f, 1f),
                            barColor = barColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gasto: ${formatCurrency(b.spentAmount, hideValues)} (${b.percentage.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = barColor
                            )
                            if (b.isOverBudget) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Acima do limite!", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            } else if (b.isNearBudget) {
                                Text("Atenção (80%+)", style = MaterialTheme.typography.labelSmall, color = AlertOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalsView(
    goals: List<SavingsGoalEntity>,
    hideValues: Boolean,
    onAddGoal: () -> Unit,
    onDeposit: (SavingsGoalEntity) -> Unit,
    onDelete: (SavingsGoalEntity) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("savings_goals_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Metas de Economia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddGoal,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova Meta")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (goals.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhuma meta cadastrada",
                    subMessage = "Crie metas para sua reserva de emergência, viagens ou conquistas pessoais!",
                    icon = Icons.Default.Savings
                )
            }
        } else {
            items(goals) { g ->
                val progress = if (g.targetAmount > 0) ((g.savedAmount / g.targetAmount) * 100).toFloat() else 0f
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = g.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Prazo: ${dateFormatter.format(g.targetDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(g) }, modifier = Modifier.size(48.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir meta", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ProgressBarCustom(
                            progress = (progress / 100f).coerceIn(0f, 1f),
                            barColor = GoldPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatCurrency(g.savedAmount, hideValues)} de ${formatCurrency(g.targetAmount, hideValues)} (${progress.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { onDeposit(g) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("+ Aporte", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardsView(
    cardInvoices: List<CardInvoiceInfo>,
    hideValues: Boolean,
    onAddCard: () -> Unit,
    onDeleteCard: (CreditCardEntity) -> Unit,
    onTogglePaid: (CreditCardEntity, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("credit_cards_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cartões de Crédito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddCard,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                    modifier = Modifier.testTag("add_card_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo Cartão")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (cardInvoices.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhum cartão cadastrado",
                    subMessage = "Cadastre seus cartões para gerenciar limites, faturas e parcelamentos automáticos.",
                    icon = Icons.Default.CreditCard
                )
            }
        } else {
            items(cardInvoices) { item ->
                val c = item.card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(c.colorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (c.lastFourDigits.isNotBlank()) {
                                        Text(text = "Final •••• ${c.lastFourDigits}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            IconButton(onClick = { onDeleteCard(c) }, modifier = Modifier.size(48.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir cartão", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Fatura Atual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(item.currentInvoiceTotal, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Limite Disponível", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(item.availableLimit, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDarkPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ProgressBarCustom(
                            progress = if (c.limitAmount > 0) (item.currentInvoiceTotal / c.limitAmount).toFloat() else 0f,
                            barColor = ExpenseRed
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Limite Total: ${formatCurrency(c.limitAmount, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Fecha dia ${c.closingDay} • Vence dia ${c.dueDay}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (item.currentInvoiceTotal > 0 || item.isPaidThisMonth) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isPaidThisMonth) {
                                    OutlinedButton(
                                        onClick = { onTogglePaid(c, item.currentInvoiceTotal) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IncomeGreen),
                                        modifier = Modifier.testTag("unpay_card_invoice_${c.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = IncomeGreen
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fatura Paga", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onTogglePaid(c, item.currentInvoiceTotal) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                                        modifier = Modifier.testTag("pay_card_invoice_${c.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pagar Fatura", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoansView(
    loans: List<LoanEntity>,
    summary: LoanSummary,
    hideValues: Boolean,
    onAddLoan: () -> Unit,
    onPayInstallment: (LoanEntity) -> Unit,
    onAmortize: (LoanEntity) -> Unit,
    onDelete: (LoanEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loans_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Empréstimos e Financiamentos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddLoan,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                    modifier = Modifier.testTag("add_loan_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cadastrar")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Summary Card
        if (loans.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Total das Dívidas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(summary.totalDebt, hideValues),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Compromisso mensal: ${formatCurrency(summary.totalMonthlyInstallments, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (summary.estimatedMonthsToFree > 0) {
                                Text(
                                    text = "Quitação em aprox. ${summary.estimatedMonthsToFree} meses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (loans.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhum empréstimo ou financiamento cadastrado",
                    subMessage = "Monitore o saldo devedor, controle amortizações e veja a previsão de quitação total.",
                    icon = Icons.Default.AccountBalance
                )
            }
        } else {
            items(loans) { loan ->
                val progress = if (loan.totalInstallments > 0) ((loan.paidInstallments.toFloat() / loan.totalInstallments) * 100) else 0f
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = loan.institution,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vencimento: dia ${loan.dueDay}${if (loan.interestRate != null) " • Juros: ${loan.interestRate}% a.m." else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(loan) }, modifier = Modifier.size(48.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir empréstimo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Saldo Devedor Atual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(loan.currentBalance, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Parcela Mensal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(loan.installmentAmount, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ProgressBarCustom(
                            progress = (progress / 100f).coerceIn(0f, 1f),
                            barColor = GreenDarkPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pagas: ${loan.paidInstallments} de ${loan.totalInstallments} (${progress.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val remaining = loan.totalInstallments - loan.paidInstallments
                            Text(
                                text = "$remaining restantes",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onAmortize(loan) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Amortizar", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onPayInstallment(loan) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Pagar Parcela", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringBillsView(
    bills: List<RecurringBillEntity>,
    hideValues: Boolean,
    curMonthKey: String,
    onAddBill: () -> Unit,
    onTogglePaid: (RecurringBillEntity) -> Unit,
    onDelete: (RecurringBillEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("recurring_bills_view")
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contas Fixas & Recorrentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddBill,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                    modifier = Modifier.testTag("add_bill_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova Conta")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (bills.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhuma conta fixa cadastrada",
                    subMessage = "Cadastre suas despesas recorrentes (energia, água, internet, streaming) para não esquecer nenhum vencimento.",
                    icon = Icons.Default.EventNote
                )
            }
        } else {
            items(bills) { bill ->
                val isPaid = bill.lastPaidMonth == curMonthKey
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isPaid) IncomeGreen.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DIA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${bill.dueDay}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = bill.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${bill.category} • ${bill.frequency}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatCurrency(bill.amount, hideValues),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaid) IncomeGreen else ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onTogglePaid(bill) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                    contentDescription = if (isPaid) "Conta paga" else "Marcar como paga",
                                    tint = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDelete(bill) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir conta",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickDepositGoalDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Aporte na Meta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = goalName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.') },
                    label = { Text("Valor do Depósito (R$)") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                error = "Informe um valor válido maior que zero."
                                return@Button
                            }
                            onConfirm(amt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Depositar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
