package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CategoryEntity
import com.example.data.CreditCardEntity
import com.example.data.TransactionEntity
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    initialTransaction: TransactionEntity? = null,
    categories: List<CategoryEntity>,
    creditCards: List<CreditCardEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        dateMillis: Long,
        paymentMethod: String,
        cardId: Long?,
        installments: Int,
        notes: String
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: "EXPENSE") }
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountText by remember {
        mutableStateOf(if (initialTransaction != null) String.format(Locale.US, "%.2f", initialTransaction.amount) else "")
    }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: "Alimentação"
        )
    }
    var selectedDateMillis by remember {
        mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis())
    }
    var paymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: "Pix")
    }
    var selectedCardId by remember {
        mutableStateOf(initialTransaction?.cardId ?: creditCards.firstOrNull()?.id)
    }
    var isRecurringOrInstallment by remember {
        mutableStateOf((initialTransaction?.totalInstallments ?: 1) > 1)
    }
    var installments by remember {
        mutableIntStateOf(initialTransaction?.totalInstallments ?: 1)
    }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }

    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }
    var installmentsExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    // Lista fixa das categorias principais solicitadas na especificação:
    // Alimentação, Mercado, Transporte, Casa, Saúde, Lazer, Educação e Outros.
    val defaultCategoryItems = listOf(
        CategoryEntity(name = "Alimentação", iconName = "restaurant", colorHex = 0xFFF59E0B),
        CategoryEntity(name = "Mercado", iconName = "shopping_cart", colorHex = 0xFF10B981),
        CategoryEntity(name = "Transporte", iconName = "directions_car", colorHex = 0xFF3B82F6),
        CategoryEntity(name = "Casa", iconName = "home", colorHex = 0xFF8B5CF6),
        CategoryEntity(name = "Saúde", iconName = "medical_services", colorHex = 0xFFEF4444),
        CategoryEntity(name = "Lazer", iconName = "sports_esports", colorHex = 0xFFEC4899),
        CategoryEntity(name = "Educação", iconName = "school", colorHex = 0xFF14B8A6),
        CategoryEntity(name = "Outros", iconName = "category", colorHex = 0xFF6B7280)
    )

    // Agrupa e inclui categorias personalizadas criadas pelo usuário, sem duplicações
    val displayCategories = remember(categories) {
        val list = defaultCategoryItems.toMutableList()
        val defaultNames = defaultCategoryItems.map { it.name.lowercase() }.toSet()
        for (c in categories) {
            if (c.name.lowercase() !in defaultNames) {
                list.add(c)
            }
        }
        list
    }

    val paymentMethods = listOf(
        "Pix",
        "Dinheiro",
        "Cartão de Crédito",
        "Cartão de Débito",
        "Boleto",
        "Transferência"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(24.dp)),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Topo: Cabeçalho com botão fechar e título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction != null) "Editar Movimentação" else "Nova Movimentação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // No topo, um seletor grande Despesa / Entrada
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    // Botão Despesa
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "EXPENSE") ExpenseRed else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 12.dp)
                            .testTag("type_expense_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Despesa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (type == "EXPENSE") Color.White else Color(0xFF64748B)
                        )
                    }

                    // Botão Entrada
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "INCOME") IncomeGreen else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 12.dp)
                            .testTag("type_income_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Entrada",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (type == "INCOME") Color.White else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // O valor deve aparecer grande como "R$ 0,00"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, GreenLightCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Valor da movimentação",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val sanitized = input.replace(',', '.')
                            if (sanitized.isEmpty() || sanitized.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                amountText = sanitized
                            }
                        },
                        placeholder = {
                            Text(
                                text = "R$ 0,00",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center,
                            color = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // As categorias devem aparecer como botões circulares coloridos com ícones
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    displayCategories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val catColor = Color(cat.colorHex)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat.name }
                                .padding(4.dp)
                                .testTag("cat_pill_${cat.name}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) catColor else catColor.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) catColor else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = if (isSelected) Color.White else catColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Data da movimentação
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, GreenLightCardBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 12, 0)
                                    }
                                    selectedDateMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateFormatter.format(selectedDateMillis),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Selecionar Data",
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Descrição
                Text(
                    text = "Descrição",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: Supermercado, Almoço, Salário...") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = GreenLightCardBorder,
                        focusedBorderColor = Color(0xFF047857)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Forma de Pagamento
                Text(
                    text = "Forma de pagamento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = GreenLightCardBorder,
                            focusedBorderColor = Color(0xFF047857)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Opções extras para Cartão de Crédito
                if (paymentMethod == "Cartão de Crédito" && creditCards.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cartão",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = cardExpanded,
                        onExpandedChange = { cardExpanded = !cardExpanded }
                    ) {
                        val cardName = creditCards.find { it.id == selectedCardId }?.name ?: "Selecionar Cartão"
                        OutlinedTextField(
                            value = cardName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(Icons.Default.CreditCard, null, tint = Color(0xFF047857))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = cardExpanded,
                            onDismissRequest = { cardExpanded = false }
                        ) {
                            creditCards.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text("${card.name} (final ${card.lastFourDigits.ifEmpty { "0000" }})") },
                                    onClick = {
                                        selectedCardId = card.id
                                        cardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Opção de despesa recorrente / parcelamento
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, GreenLightCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Despesa recorrente / parcelada",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRecurringOrInstallment) "Dividir em parcelas mensais" else "Lançamento único",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Switch(
                        checked = isRecurringOrInstallment,
                        onCheckedChange = { isRecurringOrInstallment = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF047857)
                        )
                    )
                }

                if (isRecurringOrInstallment && initialTransaction == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = installmentsExpanded,
                        onExpandedChange = { installmentsExpanded = !installmentsExpanded }
                    ) {
                        OutlinedTextField(
                            value = "$installments parcelas mensais",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Número de Parcelas") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = installmentsExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = installmentsExpanded,
                            onDismissRequest = { installmentsExpanded = false }
                        ) {
                            (2..24).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count parcelas") },
                                    onClick = {
                                        installments = count
                                        installmentsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botão de salvar
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            errorMessage = "Por favor, digite um valor válido maior que zero."
                            return@Button
                        }
                        val finalTitle = title.ifBlank { selectedCategory }
                        onSave(
                            finalTitle,
                            parsedAmount,
                            type,
                            selectedCategory,
                            selectedDateMillis,
                            paymentMethod,
                            if (paymentMethod == "Cartão de Crédito") selectedCardId else null,
                            if (isRecurringOrInstallment) installments else 1,
                            notes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                ) {
                    Text(
                        text = if (initialTransaction != null) "Atualizar Movimentação" else "Salvar Movimentação",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                // Botão de excluir para edição
                if (initialTransaction != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Movimentação", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && initialTransaction != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir movimentação?") },
            text = { Text("Esta ação não poderá ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(initialTransaction.id)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Excluir", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
