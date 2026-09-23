package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionEntity
import com.example.ui.components.AppTopBar
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.IncomeGreen
import com.example.viewmodel.FinanceViewModel

sealed class Screen(val title: String, val icon: ImageVector, val tag: String) {
    object Home : Screen("Início", Icons.Default.Home, "nav_home")
    object Transactions : Screen("Movimentações", Icons.Default.ReceiptLong, "nav_transactions")
    object Planning : Screen("Planejamento", Icons.Default.Shield, "nav_planning")
    object Reports : Screen("Relatórios", Icons.Default.Insights, "nav_reports")
    object More : Screen("Mais", Icons.Default.MoreHoriz, "nav_more")
}

@Composable
fun MainScreen(viewModel: FinanceViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var planningInitialTab by remember { mutableStateOf<String?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val hideValues by viewModel.hideValues.collectAsStateWithLifecycle()
    val selectedCal by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userPhotoPath by viewModel.userPhotoPath.collectAsStateWithLifecycle()
    val userInitials = remember(userName) { viewModel.getUserInitials(userName) }

    val monthDisplay = viewModel.getMonthYearDisplay(selectedCal)

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (currentScreen == Screen.Home) (if (userName.isNotBlank()) userName else "Meu Financeiro") else currentScreen.title,
                greeting = null,
                isHomeScreen = currentScreen == Screen.Home,
                userName = userName,
                userPhotoPath = userPhotoPath,
                userInitials = userInitials,
                onProfileClick = { showProfileDialog = true },
                monthDisplay = if (currentScreen != Screen.More) monthDisplay else null,
                onPrevMonth = if (currentScreen != Screen.More) { { viewModel.previousMonth() } } else null,
                onNextMonth = if (currentScreen != Screen.More) { { viewModel.nextMonth() } } else null,
                hideValues = hideValues,
                onToggleHideValues = { viewModel.toggleHideValues() }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentScreen = currentScreen,
                onSelectScreen = { screen ->
                    currentScreen = screen
                    if (screen != Screen.Planning) {
                        planningInitialTab = null
                    }
                },
                onQuickAdd = {
                    editingTransaction = null
                    showAddTransactionDialog = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { currentScreen = Screen.Transactions },
                    onNavigateToPlanning = { tabKey ->
                        planningInitialTab = tabKey
                        currentScreen = Screen.Planning
                    },
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        showAddTransactionDialog = true
                    },
                    onQuickAdd = {
                        editingTransaction = null
                        showAddTransactionDialog = true
                    }
                )
                Screen.Transactions -> TransactionsScreen(
                    viewModel = viewModel,
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        showAddTransactionDialog = true
                    }
                )
                Screen.Planning -> PlanningScreen(
                    viewModel = viewModel,
                    initialTabKey = planningInitialTab
                )
                Screen.Reports -> ReportsScreen(viewModel = viewModel)
                Screen.More -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenProfile = { showProfileDialog = true }
                )
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showAddTransactionDialog) {
        AddEditTransactionDialog(
            initialTransaction = editingTransaction,
            categories = categories,
            creditCards = creditCards,
            onDismiss = {
                showAddTransactionDialog = false
                editingTransaction = null
            },
            onSave = { title, amount, type, category, dateMillis, paymentMethod, cardId, installments, notes ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            dateMillis = dateMillis,
                            paymentMethod = paymentMethod,
                            cardId = cardId,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateMillis = dateMillis,
                        paymentMethod = paymentMethod,
                        cardId = cardId,
                        installments = installments,
                        notes = notes
                    )
                }
                showAddTransactionDialog = false
                editingTransaction = null
            },
            onDelete = if (editingTransaction != null) {
                { id ->
                    viewModel.deleteTransaction(id)
                    showAddTransactionDialog = false
                    editingTransaction = null
                }
            } else null
        )
    }
}

// A barra inferior permanece branca, com Início, Movimentações, botão + central verde perfeitamente centralizado e elevado sem corte, Planejamento, Relatórios e Mais.
@Composable
fun AppBottomNavigation(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit,
    onQuickAdd: () -> Unit
) {
    val fabSize = 54.dp
    val fabElevation = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Barra Branca Inferior (com espaçamento superior para o FAB elevado não ser recortado)
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(0.75.dp, GreenLightCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = fabElevation)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metade esquerda: Início e Movimentações perfeitamente balanceados
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            screen = Screen.Home,
                            isSelected = currentScreen == Screen.Home,
                            onClick = { onSelectScreen(Screen.Home) }
                        )
                        BottomNavItem(
                            screen = Screen.Transactions,
                            isSelected = currentScreen == Screen.Transactions,
                            onClick = { onSelectScreen(Screen.Transactions) }
                        )
                    }

                    // Espaço central do FAB (garante simetria exata de 50% entre esquerda e direita)
                    Spacer(modifier = Modifier.width(56.dp))

                    // Metade direita: Planejamento, Relatórios e Mais perfeitamente balanceados
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            screen = Screen.Planning,
                            isSelected = currentScreen == Screen.Planning,
                            onClick = { onSelectScreen(Screen.Planning) }
                        )
                        BottomNavItem(
                            screen = Screen.Reports,
                            isSelected = currentScreen == Screen.Reports,
                            onClick = { onSelectScreen(Screen.Reports) }
                        )
                        BottomNavItem(
                            screen = Screen.More,
                            isSelected = currentScreen == Screen.More,
                            onClick = { onSelectScreen(Screen.More) }
                        )
                    }
                }
            }
        }

        // 2. Botão central "+" flutuante, perfeitamente centralizado horizontalmente e levemente elevado
        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                onClick = onQuickAdd,
                shape = CircleShape,
                color = IncomeGreen,
                shadowElevation = 6.dp,
                border = BorderStroke(3.dp, Color.White),
                modifier = Modifier
                    .size(fabSize)
                    .testTag("fab_add_transaction")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(IncomeGreen, Color(0xFF059669))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Lançamento",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF047857) // Dark vibrant green
    val inactiveColor = Color(0xFF64748B) // Slate muted gray

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag(screen.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.title,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = screen.title,
            fontSize = 10.sp,
            letterSpacing = (-0.2).sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
