package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SavedBlue
import com.example.viewmodel.CalendarDayItem
import com.example.viewmodel.CategoryExpense
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.MonthSummary
import java.util.Calendar
import kotlin.math.max

private val CalendarDayItem.isBill: Boolean
    get() = type == "BILL" || type == "LOAN" || type == "CARD"

@Composable
fun ReportsScreen(viewModel: FinanceViewModel) {
    val hideValues by viewModel.hideValues.collectAsStateWithLifecycle()
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.categoryExpenses.collectAsStateWithLifecycle()
    val monthHistory by viewModel.monthHistory.collectAsStateWithLifecycle()
    val calendarItems by viewModel.calendarItems.collectAsStateWithLifecycle()
    val selectedCal by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gráficos & Métricas, 1: Calendário Financeiro

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF047857),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF047857)
                )
            },
            modifier = Modifier.border(0.5.dp, GreenLightCardBorder)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 0) Color(0xFF047857) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Gráficos & Métricas",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color(0xFF047857) else Color(0xFF64748B)
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 1) Color(0xFF047857) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Calendário",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color(0xFF047857) else Color(0xFF64748B)
                        )
                    }
                }
            )
        }

        if (selectedTab == 0) {
            ReportsDetailView(
                hideValues = hideValues,
                monthIncome = monthIncome,
                monthExpense = monthExpense,
                categoryExpenses = categoryExpenses,
                monthHistory = monthHistory,
                budgetProgress = budgetProgress
            )
        } else {
            CalendarFinancialView(
                calendar = selectedCal,
                calendarItems = calendarItems,
                hideValues = hideValues
            )
        }
    }
}

@Composable
fun ReportsDetailView(
    hideValues: Boolean,
    monthIncome: Double,
    monthExpense: Double,
    categoryExpenses: List<CategoryExpense>,
    monthHistory: List<MonthSummary>,
    budgetProgress: List<com.example.viewmodel.BudgetProgress>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp)
    ) {
        // 1. Entradas vs Saídas do Mês
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "Entradas vs Saídas do Mês")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val netBalance = monthIncome - monthExpense
                    val savingsRate = if (monthIncome > 0) ((netBalance / monthIncome) * 100).coerceIn(0.0, 100.0) else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Saldo Líquido do Mês",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(netBalance, hideValues),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Taxa de Poupança",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${savingsRate.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SavedBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visual Side by Side Proportion Bar
                    val totalFlow = monthIncome + monthExpense
                    val incomeRatio = if (totalFlow > 0) (monthIncome / totalFlow).toFloat() else 0.5f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(incomeRatio)
                                    .height(14.dp)
                                    .background(IncomeGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(14.dp)
                                    .background(ExpenseRed)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Entradas: ${formatCurrency(monthIncome, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saídas: ${formatCurrency(monthExpense, hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Gastos por Categoria (Gráfico Circular Colorido)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "Gastos por Categoria")
            if (categoryExpenses.isEmpty()) {
                EmptyPlaceholder(
                    message = "Sem despesas registradas neste mês",
                    subMessage = "Lance despesas para visualizar o gráfico circular dos seus gastos."
                )
            } else {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Canvas Circular Donut Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(140.dp)) {
                                var startAngle = -90f
                                categoryExpenses.forEach { cat ->
                                    val sweep = (cat.percentage / 100f) * 360f
                                    drawArc(
                                        color = Color(cat.colorHex),
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 22.dp.toPx())
                                    )
                                    startAngle += sweep
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Total Gasto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCurrency(monthExpense, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category List breakdown
                        categoryExpenses.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = Color(cat.colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${formatCurrency(cat.amount, hideValues)} (${cat.percentage.toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Gráfico de Barras para Comparação Mensal (Evolução)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(title = "Comparação Mensal")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val maxVal = max(
                        100.0,
                        monthHistory.maxOfOrNull { max(it.income, it.expense) } ?: 100.0
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        monthHistory.forEach { m ->
                            val incH = ((m.income / maxVal) * 110).toFloat().coerceIn(4f, 110f)
                            val expH = ((m.expense / maxVal) * 110).toFloat().coerceIn(4f, 110f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Barra Entradas (Verde vivo)
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(incH.dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(IncomeGreen)
                                    )
                                    // Barra Saídas (Vermelho vivo)
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(expH.dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(ExpenseRed)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = m.displayLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Entradas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Saídas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarFinancialView(
    calendar: Calendar,
    calendarItems: Map<Int, List<CalendarDayItem>>,
    hideValues: Boolean
) {
    var selectedDay by remember {
        mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH))
    }

    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayItems = calendarItems[selectedDay] ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dias do Mês",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid de dias
                    val daysList = (1..maxDays).toList()
                    val rows = daysList.chunked(7)

                    rows.forEach { rowDays ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowDays.forEach { day ->
                                val isSelected = day == selectedDay
                                val itemsForDay = calendarItems[day] ?: emptyList()
                                val hasBill = itemsForDay.any { it.isBill }
                                val hasTransaction = itemsForDay.any { !it.isBill }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF047857)
                                            else if (hasBill) ExpenseRed.copy(alpha = 0.12f)
                                            else if (hasTransaction) IncomeGreen.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .clickable { selectedDay = day },
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White
                                        else if (hasBill) ExpenseRed
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                    if (!isSelected && (hasBill || hasTransaction)) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (hasBill) ExpenseRed else IncomeGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Lançamentos e Contas do Dia $selectedDay")
        }

        if (dayItems.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nenhum compromisso ou lançamento neste dia",
                    subMessage = "Selecione outro dia com marcadores ou lance uma nova movimentação.",
                    icon = Icons.Default.DateRange
                )
            }
        } else {
            items(dayItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(16.dp)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isBill) ExpenseRed.copy(alpha = 0.12f)
                                        else IncomeGreen.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isBill) Icons.Default.Circle else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (item.isBill) ExpenseRed else IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (item.isBill) "Conta a Pagar" else "Movimentação",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = formatCurrency(item.amount, hideValues),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isBill) ExpenseRed else IncomeGreen
                        )
                    }
                }
            }
        }
    }
}
