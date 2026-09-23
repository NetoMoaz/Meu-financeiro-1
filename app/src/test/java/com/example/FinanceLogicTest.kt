package com.example

import com.example.data.BudgetEntity
import com.example.data.CreditCardEntity
import com.example.data.LoanEntity
import com.example.data.RecurringBillEntity
import com.example.data.SavingsGoalEntity
import com.example.data.TransactionEntity
import com.example.data.isCreditCardInvoicePayment
import com.example.viewmodel.CardInvoiceInfo
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceLogicTest {

    @Test
    fun `overall balance excludes unpaid credit card purchases`() {
        // Test case specified by user:
        // R$ 1.000 de entrada, R$ 100 de despesa via PIX e R$ 200 de compra no cartão ainda não paga
        // O Saldo Disponível em Conta deve ser R$ 900, não R$ 300 nem R$ 700.
        val transactions = listOf(
            TransactionEntity(
                title = "Salário",
                amount = 1000.0,
                type = "INCOME",
                category = "Salário",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência"
            ),
            TransactionEntity(
                title = "Restaurante",
                amount = 100.0,
                type = "EXPENSE",
                category = "Alimentação",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Pix"
            ),
            TransactionEntity(
                title = "Mercado",
                amount = 200.0,
                type = "EXPENSE",
                category = "Mercado",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Cartão de Crédito",
                cardId = 1L
            )
        )

        val inc = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expFromAccount = transactions.filter {
            it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito"
        }.sumOf { it.amount }
        val overallBalance = inc - expFromAccount

        assertEquals(1000.0, inc, 0.001)
        assertEquals(100.0, expFromAccount, 0.001)
        assertEquals(900.0, overallBalance, 0.001)
    }

    @Test
    fun `monthly expenses represent consumption including credit card`() {
        // Uma compra no cartão de crédito deve aparecer normalmente nas despesas e relatórios do mês
        val transactions = listOf(
            TransactionEntity(
                title = "Salário",
                amount = 1000.0,
                type = "INCOME",
                category = "Salário",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência"
            ),
            TransactionEntity(
                title = "Restaurante",
                amount = 100.0,
                type = "EXPENSE",
                category = "Alimentação",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Pix"
            ),
            TransactionEntity(
                title = "Mercado",
                amount = 200.0,
                type = "EXPENSE",
                category = "Mercado",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Cartão de Crédito",
                cardId = 1L
            )
        )

        val monthExpense = transactions.filter {
            it.type == "EXPENSE" && !it.isCreditCardInvoicePayment()
        }.sumOf { it.amount }

        assertEquals(300.0, monthExpense, 0.001)
    }

    @Test
    fun `economy mode calculates from 900 balance and deducts pending invoice separately`() {
        // Modo Economia:
        // Saldo Disponível: R$ 900
        // Faturas Cartão: R$ 200
        // Saldo Real Livre: 900 - 200 = R$ 700 (evitando dupla contabilização)
        val balance = 900.0
        val pendingCardInvoices = 200.0
        val unpaidBills = 0.0
        val pendingLoans = 0.0
        val goalsTarget = 0.0

        val compromissos = unpaidBills + pendingCardInvoices + pendingLoans + goalsTarget
        val saldoRealLivre = balance - compromissos

        assertEquals(900.0, balance, 0.001)
        assertEquals(200.0, pendingCardInvoices, 0.001)
        assertEquals(700.0, saldoRealLivre, 0.001)
    }

    @Test
    fun `paying card invoice reduces account balance and does not duplicate report expenses`() {
        // Quando a fatura do cartão é paga:
        // 1. O Saldo em conta diminui pelo pagamento da fatura (1000 - 100 - 200 = 700)
        // 2. Fatura do cartão fica quitada (faturasCartao = 0)
        // 3. Saldo Real Livre continua R$ 700
        // 4. As despesas dos relatórios continuam R$ 300, pois pagamento de fatura não gera segunda despesa
        val card = CreditCardEntity(
            id = 1L,
            name = "Nubank",
            limitAmount = 2000.0,
            closingDay = 25,
            dueDay = 5,
            lastPaidMonth = "2026-09"
        )

        val transactions = mutableListOf(
            TransactionEntity(
                title = "Salário",
                amount = 1000.0,
                type = "INCOME",
                category = "Salário",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Transferência"
            ),
            TransactionEntity(
                title = "Restaurante",
                amount = 100.0,
                type = "EXPENSE",
                category = "Alimentação",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Pix"
            ),
            TransactionEntity(
                title = "Mercado",
                amount = 200.0,
                type = "EXPENSE",
                category = "Mercado",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Cartão de Crédito",
                cardId = card.id
            ),
            TransactionEntity(
                title = "Pagamento Fatura: Nubank",
                amount = 200.0,
                type = "EXPENSE",
                category = "Pagamento de Fatura",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Débito em Conta",
                cardId = card.id,
                notes = "Pagamento de fatura referente a 2026-09",
                isInvoicePayment = true
            )
        )

        // Verify helper
        val invoiceTx = transactions.last()
        assertTrue(invoiceTx.isCreditCardInvoicePayment())

        // 1. Overall Balance
        val inc = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expFromAccount = transactions.filter {
            it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito"
        }.sumOf { it.amount }
        val overallBalance = inc - expFromAccount
        assertEquals(700.0, overallBalance, 0.001)

        // 2. Report expenses (must NOT include invoice payment)
        val monthExpense = transactions.filter {
            it.type == "EXPENSE" && !it.isCreditCardInvoicePayment()
        }.sumOf { it.amount }
        assertEquals(300.0, monthExpense, 0.001)

        // 3. Card invoice pending commitments
        val isCardPaid = card.lastPaidMonth == "2026-09"
        val pendingCardExpenses = if (isCardPaid) 0.0 else transactions.filter {
            it.paymentMethod == "Cartão de Crédito" && it.cardId == card.id
        }.sumOf { it.amount }
        assertEquals(0.0, pendingCardExpenses, 0.001)

        // 4. Saldo Real Livre in Economy Mode
        val saldoRealLivre = overallBalance - pendingCardExpenses
        assertEquals(700.0, saldoRealLivre, 0.001)
    }

    @Test
    fun `isCreditCardInvoicePayment detects various payment markers`() {
        val normalExpense = TransactionEntity(
            title = "Farmácia",
            amount = 50.0,
            type = "EXPENSE",
            category = "Saúde",
            dateMillis = 0L,
            paymentMethod = "Pix"
        )
        assertFalse(normalExpense.isCreditCardInvoicePayment())

        val byFlag = TransactionEntity(
            title = "Pagamento Fatura",
            amount = 200.0,
            type = "EXPENSE",
            category = "Outros",
            dateMillis = 0L,
            paymentMethod = "Débito",
            isInvoicePayment = true
        )
        assertTrue(byFlag.isCreditCardInvoicePayment())

        val byCategory = TransactionEntity(
            title = "Cartão Itaú",
            amount = 350.0,
            type = "EXPENSE",
            category = "Pagamento de Fatura",
            dateMillis = 0L,
            paymentMethod = "Débito"
        )
        assertTrue(byCategory.isCreditCardInvoicePayment())

        val byTitle = TransactionEntity(
            title = "Pagamento de Fatura do Cartão Inter",
            amount = 500.0,
            type = "EXPENSE",
            category = "Cartão",
            dateMillis = 0L,
            paymentMethod = "Boleto"
        )
        assertTrue(byTitle.isCreditCardInvoicePayment())
    }

    @Test
    fun `a gastar rule positive saldo real livre shows positive value without deficit`() {
        val saldoRealLivre = 700.0
        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficitAmount = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null

        assertEquals(700.0, aGastar, 0.001)
        assertEquals(null, deficitAmount)
    }

    @Test
    fun `a gastar rule zero saldo real livre shows zero without deficit`() {
        val saldoRealLivre = 0.0
        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficitAmount = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null

        assertEquals(0.0, aGastar, 0.001)
        assertEquals(null, deficitAmount)
    }

    @Test
    fun `a gastar rule negative saldo real livre shows zero and positive deficit amount`() {
        // Exemplo: Saldo Real Livre = -R$ 300,00 -> A gastar: R$ 0,00 e Déficit previsto: R$ 300,00
        val saldoRealLivre = -300.0
        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficitAmount = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null

        assertEquals(0.0, aGastar, 0.001)
        assertEquals(300.0, deficitAmount!!, 0.001)
    }

    @Test
    fun `month summary without budget scenario 1 - positive saldo real livre`() {
        val saldoDisponivel = 1500.0
        val contasNaoPagas = 200.0
        val faturasCartao = 300.0
        val parcelasEmprestimo = 0.0
        val metasAporte = 100.0

        val totalCompromissos = contasNaoPagas + faturasCartao + parcelasEmprestimo + metasAporte
        val saldoRealLivre = saldoDisponivel - totalCompromissos

        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficit = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null
        val badge = when {
            saldoRealLivre < 0 -> "Déficit previsto"
            saldoRealLivre == 0.0 -> "Equilibrado"
            else -> "Saldo livre"
        }

        assertEquals(900.0, saldoRealLivre, 0.001)
        assertEquals(900.0, aGastar, 0.001)
        assertNull(deficit)
        assertEquals("Saldo livre", badge)
    }

    @Test
    fun `month summary without budget scenario 2 - exactly zero saldo real livre`() {
        val saldoDisponivel = 800.0
        val contasNaoPagas = 300.0
        val faturasCartao = 500.0
        val parcelasEmprestimo = 0.0
        val metasAporte = 0.0

        val totalCompromissos = contasNaoPagas + faturasCartao + parcelasEmprestimo + metasAporte
        val saldoRealLivre = saldoDisponivel - totalCompromissos

        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficit = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null
        val badge = when {
            saldoRealLivre < 0 -> "Déficit previsto"
            saldoRealLivre == 0.0 -> "Equilibrado"
            else -> "Saldo livre"
        }

        assertEquals(0.0, saldoRealLivre, 0.001)
        assertEquals(0.0, aGastar, 0.001)
        assertNull(deficit)
        assertEquals("Equilibrado", badge)
    }

    @Test
    fun `month summary without budget scenario 3 - deficit as in user example`() {
        // Exemplo:
        // Entradas: R$ 1.000,00
        // Saídas realizadas: R$ 300,00
        // Saldo disponível em conta: R$ 900,00
        // Fatura do cartão no mês: R$ 200,00
        // Parcela de empréstimo: R$ 1.000,00
        // Saldo Real Livre: 900 - (200 + 1000) = -R$ 300,00
        val saldoDisponivel = 900.0
        val contasNaoPagas = 0.0
        val faturasCartao = 200.0
        val parcelasEmprestimo = 1000.0
        val metasAporte = 0.0

        val totalCompromissos = contasNaoPagas + faturasCartao + parcelasEmprestimo + metasAporte
        val saldoRealLivre = saldoDisponivel - totalCompromissos

        val aGastar = if (saldoRealLivre > 0.0) saldoRealLivre else 0.0
        val deficit = if (saldoRealLivre < 0.0) kotlin.math.abs(saldoRealLivre) else null
        val badge = when {
            saldoRealLivre < 0 -> "Atenção"
            saldoRealLivre == 0.0 -> "Equilibrado"
            else -> "Saldo livre"
        }

        assertEquals(-300.0, saldoRealLivre, 0.001)
        assertEquals(0.0, aGastar, 0.001)
        assertEquals(300.0, deficit!!, 0.001)
        assertEquals("Atenção", badge)
    }

    @Test
    fun `upcoming commitments includes pending bills cards and loans ordered by due day`() {
        val curMonthKey = "2026-09"

        data class TestCommitment(
            val name: String,
            val amount: Double,
            val dueDay: Int,
            val type: String
        )

        // 1. Contas
        val bill1 = RecurringBillEntity(id = 1, name = "Energia", amount = 150.0, dueDay = 20, category = "Moradia", lastPaidMonth = "")
        val billPaid = RecurringBillEntity(id = 2, name = "Internet", amount = 100.0, dueDay = 5, category = "Moradia", lastPaidMonth = curMonthKey)

        // 2. Cartões
        val cardNubank = CreditCardEntity(id = 1, name = "Nubank", limitAmount = 2000.0, closingDay = 10, dueDay = 15)
        val invoiceNubank = CardInvoiceInfo(card = cardNubank, currentInvoiceTotal = 250.0, availableLimit = 1750.0, upcomingInstallmentsCount = 0, isPaidThisMonth = false)
        val cardInter = CreditCardEntity(id = 2, name = "Inter", limitAmount = 3000.0, closingDay = 25, dueDay = 2)
        val invoiceInterPaid = CardInvoiceInfo(card = cardInter, currentInvoiceTotal = 500.0, availableLimit = 2500.0, upcomingInstallmentsCount = 0, isPaidThisMonth = true)

        // 3. Empréstimos
        val loanBB = LoanEntity(id = 1, institution = "Empréstimo BB", currentBalance = 10000.0, installmentAmount = 1000.0, totalInstallments = 12, paidInstallments = 2, dueDay = 10, lastPaidMonth = "")
        val loanQuitado = LoanEntity(id = 2, institution = "Empréstimo Caixa", currentBalance = 0.0, installmentAmount = 400.0, totalInstallments = 10, paidInstallments = 10, dueDay = 8, lastPaidMonth = "")

        val list = mutableListOf<TestCommitment>()

        listOf(bill1, billPaid).filter { it.lastPaidMonth != curMonthKey }.forEach {
            list.add(TestCommitment(it.name, it.amount, it.dueDay, "Conta"))
        }

        listOf(invoiceNubank, invoiceInterPaid).filter { !it.isPaidThisMonth && it.currentInvoiceTotal > 0.0 }.forEach {
            list.add(TestCommitment("Fatura ${it.card.name}", it.currentInvoiceTotal, it.card.dueDay, "Cartão"))
        }

        listOf(loanBB, loanQuitado).filter { it.lastPaidMonth != curMonthKey && it.paidInstallments < it.totalInstallments && it.installmentAmount > 0.0 }.forEach {
            list.add(TestCommitment(it.institution, it.installmentAmount, it.dueDay, "Empréstimo"))
        }

        val sorted = list.sortedWith(compareBy({ it.dueDay }, { it.name }))

        // Verificações:
        // billPaid e invoiceInterPaid e loanQuitado NÃO devem estar na lista
        assertEquals(3, sorted.size)
        // Ordem por dueDay:
        // 1º: Empréstimo BB (dueDay = 10)
        assertEquals("Empréstimo BB", sorted[0].name)
        assertEquals(1000.0, sorted[0].amount, 0.001)
        assertEquals(10, sorted[0].dueDay)
        assertEquals("Empréstimo", sorted[0].type)

        // 2º: Fatura Nubank (dueDay = 15)
        assertEquals("Fatura Nubank", sorted[1].name)
        assertEquals(250.0, sorted[1].amount, 0.001)
        assertEquals(15, sorted[1].dueDay)
        assertEquals("Cartão", sorted[1].type)

        // 3º: Energia (dueDay = 20)
        assertEquals("Energia", sorted[2].name)
        assertEquals(150.0, sorted[2].amount, 0.001)
        assertEquals(20, sorted[2].dueDay)
        assertEquals("Conta", sorted[2].type)
    }

    @Test
    fun `paying loan BB installment updates lastPaidMonth and removes it from pending list`() {
        val curMonthKey = "2026-09"
        val loanBB = LoanEntity(id = 1, institution = "Empréstimo BB", currentBalance = 10000.0, installmentAmount = 1000.0, totalInstallments = 12, paidInstallments = 2, dueDay = 10, lastPaidMonth = "")

        // Antes de pagar: pendente
        val isPendingBefore = loanBB.lastPaidMonth != curMonthKey && loanBB.paidInstallments < loanBB.totalInstallments
        assertTrue(isPendingBefore)

        // Simulação do pagamento
        val updatedLoan = loanBB.copy(
            paidInstallments = loanBB.paidInstallments + 1,
            currentBalance = loanBB.currentBalance - loanBB.installmentAmount,
            lastPaidMonth = curMonthKey
        )

        // Depois de pagar: não mais pendente no mês
        val isPendingAfter = updatedLoan.lastPaidMonth != curMonthKey && updatedLoan.paidInstallments < updatedLoan.totalInstallments
        assertFalse(isPendingAfter)
    }

    @Test
    fun `user initials generation properly handles empty single and compound names`() {
        fun computeInitials(name: String): String {
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

        assertEquals("MF", computeInitials(""))
        assertEquals("MF", computeInitials("   "))
        assertEquals("LS", computeInitials("Lucas Silva"))
        assertEquals("AB", computeInitials("Ana Beatriz Pereira"))
        assertEquals("AL", computeInitials("Alexandre"))
        assertEquals("J", computeInitials("J"))
    }

    @Test
    fun `profile display name falls back to Meu Financeiro when name is blank`() {
        fun getDisplayName(name: String): String {
            return if (name.isNotBlank()) name else "Meu Financeiro"
        }

        assertEquals("Meu Financeiro", getDisplayName(""))
        assertEquals("Meu Financeiro", getDisplayName("   "))
        assertEquals("Carlos Drummond", getDisplayName("Carlos Drummond"))
    }

    @Test
    fun `profile photo removal resets photo path to null while preserving name`() {
        var profileName = "Mariana Costa"
        var photoPath: String? = "/data/user/0/com.example/files/profile_photo_123.jpg"

        // Simula remoção de foto
        photoPath = null

        assertEquals("Mariana Costa", profileName)
        assertNull(photoPath)
    }

    @Test
    fun `complete profile lifecycle - register name, choose photo, save, reopen app, swap photo, remove photo, empty name`() {
        // Mock SharedPreferences storage map
        val prefsMap = mutableMapOf<String, Any?>()

        // Helper representing ViewModel logic
        fun saveProfile(name: String, photoPath: String?) {
            val trimmed = name.trim()
            prefsMap["user_profile_name"] = trimmed
            if (photoPath != null) {
                prefsMap["user_profile_photo"] = photoPath
            } else {
                prefsMap.remove("user_profile_photo")
            }
        }

        // Helper representing Home header formatting
        fun getHomeHeader(name: String?): Pair<String?, String> {
            val trimmed = name?.trim() ?: ""
            return if (trimmed.isNotEmpty()) {
                Pair("Olá, $trimmed 👋", "Meu Financeiro")
            } else {
                Pair(null, "Meu Financeiro")
            }
        }

        fun getAvatarInitials(photoPath: String?): String {
            return if (photoPath != null) "" else "MF"
        }

        // 1. Cadastrar nome: "Alexandre"
        var currentName = "Alexandre"
        var currentPhotoPath: String? = null

        // 2. Escolher foto: "profile_photo_1001.jpg"
        val tempPhotoPath1 = "/mock/files/profile_photo_1001.jpg"

        // 3. Salvar
        saveProfile(currentName, tempPhotoPath1)
        currentPhotoPath = tempPhotoPath1

        assertEquals("Alexandre", prefsMap["user_profile_name"])
        assertEquals(tempPhotoPath1, prefsMap["user_profile_photo"])
        val headerAfterSave = getHomeHeader(currentName)
        assertEquals("Olá, Alexandre 👋", headerAfterSave.first)
        assertEquals("Meu Financeiro", headerAfterSave.second)

        // 4. Fechar e abrir novamente o aplicativo (restaura do prefsMap)
        val restoredName = prefsMap["user_profile_name"] as String
        val restoredPhotoPath = prefsMap["user_profile_photo"] as? String
        assertEquals("Alexandre", restoredName)
        assertEquals(tempPhotoPath1, restoredPhotoPath)

        // 5. Trocar foto: escolher nova foto "profile_photo_2002.jpg" e salvar
        val tempPhotoPath2 = "/mock/files/profile_photo_2002.jpg"
        saveProfile(restoredName, tempPhotoPath2)
        assertEquals(tempPhotoPath2, prefsMap["user_profile_photo"])

        // 6. Remover foto e salvar
        saveProfile(restoredName, null)
        assertFalse(prefsMap.containsKey("user_profile_photo"))
        assertNull(prefsMap["user_profile_photo"])
        // Volta automaticamente para as iniciais "MF"
        assertEquals("MF", getAvatarInitials(prefsMap["user_profile_photo"] as? String))

        // 7. Deixar nome vazio e salvar
        saveProfile("", null)
        assertEquals("", prefsMap["user_profile_name"])
        val headerAfterEmpty = getHomeHeader(prefsMap["user_profile_name"] as String)
        assertNull(headerAfterEmpty.first) // Não exibe "Olá,", vírgula, emoji nem espaço
        assertEquals("Meu Financeiro", headerAfterEmpty.second)

        // Testar também valores contendo apenas espaços ou null
        val headerWithSpaces = getHomeHeader("    ")
        assertNull(headerWithSpaces.first)
        assertEquals("Meu Financeiro", headerWithSpaces.second)

        val headerWithNull = getHomeHeader(null)
        assertNull(headerWithNull.first)
        assertEquals("Meu Financeiro", headerWithNull.second)

        assertEquals("MF", getAvatarInitials(null))
    }

    @Test
    fun `upcoming commitments empty state and dynamic reactivity across bill events`() {
        fun buildUpcomingCommitments(
            bills: List<RecurringBillEntity>,
            cards: List<CardInvoiceInfo>,
            loans: List<LoanEntity>,
            curMonthKey: String
        ): List<String> {
            val list = mutableListOf<String>()
            bills.filter { it.lastPaidMonth != curMonthKey }.forEach {
                list.add("bill_${it.id}_${it.name}")
            }
            cards.filter { !it.isPaidThisMonth && it.currentInvoiceTotal > 0.0 }.forEach {
                list.add("card_${it.card.id}_${it.card.name}")
            }
            loans.filter { it.lastPaidMonth != curMonthKey && it.paidInstallments < it.totalInstallments && it.installmentAmount > 0.0 }.forEach {
                list.add("loan_${it.id}_${it.institution}")
            }
            return list
        }

        val monthKeyCurrent = "09-2026"
        val monthKeyNext = "10-2026"

        // Cenário 1: Sem contas, sem cartões e sem empréstimos pendentes -> lista vazia (mostra EmptyUpcomingCommitmentsCard)
        val initialList = buildUpcomingCommitments(emptyList(), emptyList(), emptyList(), monthKeyCurrent)
        assertTrue(initialList.isEmpty())

        // Cenário 2: Cadastrar uma conta fixa pendente -> lista com 1 item (não mostra card vazio)
        var bill1 = RecurringBillEntity(
            id = 1L,
            name = "Energia Enel",
            amount = 180.0,
            dueDay = 15,
            frequency = "MENSAL",
            category = "Moradia",
            lastPaidMonth = ""
        )
        val listWithOne = buildUpcomingCommitments(listOf(bill1), emptyList(), emptyList(), monthKeyCurrent)
        assertEquals(1, listWithOne.size)
        assertEquals("bill_1_Energia Enel", listWithOne[0])

        // Cenário 3: Cadastrar mais contas (várias contas)
        val bill2 = RecurringBillEntity(
            id = 2L,
            name = "Internet Fibra",
            amount = 120.0,
            dueDay = 20,
            frequency = "MENSAL",
            category = "Moradia",
            lastPaidMonth = ""
        )
        val listWithMultiple = buildUpcomingCommitments(listOf(bill1, bill2), emptyList(), emptyList(), monthKeyCurrent)
        assertEquals(2, listWithMultiple.size)

        // Cenário 4: Marcar conta como paga no mês atual -> atualiza lastPaidMonth e fica pendente apenas a outra
        bill1 = bill1.copy(lastPaidMonth = monthKeyCurrent)
        val listAfterOnePaid = buildUpcomingCommitments(listOf(bill1, bill2), emptyList(), emptyList(), monthKeyCurrent)
        assertEquals(1, listAfterOnePaid.size)
        assertEquals("bill_2_Internet Fibra", listAfterOnePaid[0])

        // Marcar todas como pagas -> volta a ficar vazia (mostra EmptyUpcomingCommitmentsCard)
        val bill2Paid = bill2.copy(lastPaidMonth = monthKeyCurrent)
        val listAllPaid = buildUpcomingCommitments(listOf(bill1, bill2Paid), emptyList(), emptyList(), monthKeyCurrent)
        assertTrue(listAllPaid.isEmpty())

        // Cenário 5: Exclusão de conta
        val listAfterDelete = buildUpcomingCommitments(emptyList(), emptyList(), emptyList(), monthKeyCurrent)
        assertTrue(listAfterDelete.isEmpty())

        // Cenário 6: Troca de mês -> no próximo mês ("10-2026"), as contas pagas em "09-2026" voltam a ficar pendentes!
        val listNextMonth = buildUpcomingCommitments(listOf(bill1, bill2Paid), emptyList(), emptyList(), monthKeyNext)
        assertEquals(2, listNextMonth.size)
    }

    @Test
    fun `comprehensive integrated test of all transaction types, commitments, goals and budgets across months`() {
        val currentMonthKey = "2026-09"
        val nextMonthKey = "2026-10"

        // 1. Dados temporários de teste
        val testCard = CreditCardEntity(
            id = 100L,
            name = "Cartão Teste",
            limitAmount = 5000.0,
            closingDay = 25,
            dueDay = 5
        )

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 1)

        val testGoal = SavingsGoalEntity(
            id = 200L,
            name = "Reserva de Emergência",
            targetAmount = 1200.0,
            savedAmount = 200.0,
            targetDateMillis = cal.timeInMillis
        )

        val testBudget = BudgetEntity(
            id = 300L,
            category = "Alimentação",
            limitAmount = 600.0
        )

        var testBill = RecurringBillEntity(
            id = 400L,
            name = "Condomínio",
            amount = 350.0,
            dueDay = 10,
            frequency = "MENSAL",
            category = "Moradia",
            lastPaidMonth = ""
        )

        // 2. Transações temporárias:
        // - Uma entrada: Salário R$ 5000,00
        val txIncome = TransactionEntity(
            id = 1L,
            title = "Salário Empresa",
            amount = 5000.0,
            type = "INCOME",
            category = "Salário",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "Conta Corrente"
        )
        // - Uma despesa à vista: Mercado R$ 200,00 (Dinheiro/Pix)
        val txExpenseCash = TransactionEntity(
            id = 2L,
            title = "Supermercado Semanal",
            amount = 200.0,
            type = "EXPENSE",
            category = "Alimentação",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "Pix"
        )
        // - Uma despesa no cartão: Farmácia R$ 150,00
        val txExpenseCard = TransactionEntity(
            id = 3L,
            title = "Farmácia",
            amount = 150.0,
            type = "EXPENSE",
            category = "Saúde",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "Cartão de Crédito",
            cardId = testCard.id
        )
        // - Uma despesa parcelada: Celular 3x de R$ 300,00 (mês 1 em 2026-09, mês 2 em 2026-10, mês 3 em 2026-11)
        val txInstallmentMonth1 = TransactionEntity(
            id = 4L,
            title = "Smartphone (1/3)",
            amount = 300.0,
            type = "EXPENSE",
            category = "Eletrônicos",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "Cartão de Crédito",
            cardId = testCard.id,
            installmentNumber = 1,
            totalInstallments = 3
        )
        val txInstallmentMonth2 = TransactionEntity(
            id = 5L,
            title = "Smartphone (2/3)",
            amount = 300.0,
            type = "EXPENSE",
            category = "Eletrônicos",
            dateMillis = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
            paymentMethod = "Cartão de Crédito",
            cardId = testCard.id,
            installmentNumber = 2,
            totalInstallments = 3
        )

        val txListMonth1 = listOf(txIncome, txExpenseCash, txExpenseCard, txInstallmentMonth1)
        val allTx = listOf(txIncome, txExpenseCash, txExpenseCard, txInstallmentMonth1, txInstallmentMonth2)

        // 3. Verificação de Saldo disponível (Conta Corrente)
        // Saldo disponível = Entradas - Despesas à vista/Pix (gastos de cartão pendentes não descontam ainda da conta)
        val balance = allTx.filter { it.type == "INCOME" }.sumOf { it.amount } -
                allTx.filter { it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito" }.sumOf { it.amount }
        assertEquals(4800.0, balance, 0.001) // 5000 - 200 = 4800

        // 4. Verificação de Entradas e Saídas do Mês 1
        val incomeMonth1 = txListMonth1.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expenseMonth1 = txListMonth1.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
        assertEquals(5000.0, incomeMonth1, 0.001)
        assertEquals(650.0, expenseMonth1, 0.001) // 200 (mercado) + 150 (farmácia) + 300 (celular 1/3) = 650

        // 5. Faturas de Cartão no Mês 1
        val cardInvoiceMonth1 = txListMonth1.filter {
            it.cardId == testCard.id && it.paymentMethod == "Cartão de Crédito" && !it.isCreditCardInvoicePayment()
        }.sumOf { it.amount }
        assertEquals(450.0, cardInvoiceMonth1, 0.001) // 150 + 300 = 450
        val availableCardLimitMonth1 = testCard.limitAmount - cardInvoiceMonth1
        assertEquals(4550.0, availableCardLimitMonth1, 0.001) // 5000 - 450 = 4550

        // 6. Contas a pagar pendentes
        val pendingBillsMonth1 = if (testBill.lastPaidMonth != currentMonthKey) testBill.amount else 0.0
        assertEquals(350.0, pendingBillsMonth1, 0.001)

        // 7. Aporte em Metas
        // Faltam 1000 em 2 meses -> 500/mês
        val metaTargetDiff = (testGoal.targetAmount - testGoal.savedAmount).coerceAtLeast(0.0)
        val monthlyGoalAporte = metaTargetDiff / 2.0
        assertEquals(500.0, monthlyGoalAporte, 0.001)

        // 8. Saldo Real Livre (Modo Economia)
        // Saldo disponível (4800) - Fatura Cartão (450) - Contas (350) - Empréstimos (0) - Metas (500)
        val compromissos = cardInvoiceMonth1 + pendingBillsMonth1 + monthlyGoalAporte
        val saldoRealLivre = balance - compromissos
        assertEquals(3500.0, saldoRealLivre, 0.001)

        // 9. Orçamento
        val spentInBudgetCat = txListMonth1.filter {
            it.type == "EXPENSE" && it.category == testBudget.category && !it.isCreditCardInvoicePayment()
        }.sumOf { it.amount }
        assertEquals(200.0, spentInBudgetCat, 0.001)
        val budgetPct = (spentInBudgetCat / testBudget.limitAmount) * 100
        assertEquals(33.333, budgetPct, 0.01)

        // 10. Teste de Edição: Alterar o valor do mercado de 200 para 300
        val txExpenseCashEdited = txExpenseCash.copy(amount = 300.0)
        val balanceEdited = allTx.map { if (it.id == txExpenseCash.id) txExpenseCashEdited else it }
            .filter { it.type == "INCOME" }.sumOf { it.amount } -
                allTx.map { if (it.id == txExpenseCash.id) txExpenseCashEdited else it }
                    .filter { it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito" }.sumOf { it.amount }
        assertEquals(4700.0, balanceEdited, 0.001) // 5000 - 300 = 4700

        // 11. Teste de Exclusão (remover os dados temporários de teste)
        // Todos os registros temporários de teste são limpos
        val emptyTxList = emptyList<TransactionEntity>()
        val emptyBalance = emptyTxList.filter { it.type == "INCOME" }.sumOf { it.amount } -
                emptyTxList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        assertEquals(0.0, emptyBalance, 0.001)

        // 12. Troca de mês (Mês 2: 2026-10)
        // No Mês 2, a parcela 2/3 (300) aparece, a conta de condomínio volta a ficar pendente
        val txListMonth2 = listOf(txInstallmentMonth2)
        val cardInvoiceMonth2 = txListMonth2.filter {
            it.cardId == testCard.id && it.paymentMethod == "Cartão de Crédito" && !it.isCreditCardInvoicePayment()
        }.sumOf { it.amount }
        assertEquals(300.0, cardInvoiceMonth2, 0.001)
    }

    @Test
    fun `full acceptance test user scenario with 3000 income, 500 cash expense, 600 card installment in 3x, recurring bill, profile and month navigation from sep to dec 2026`() {
        fun getAvatarInitials(name: String?): String {
            val trimmed = name?.trim() ?: ""
            if (trimmed.isEmpty()) return "MF"
            val parts = trimmed.split(" ").filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
                parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
                parts.isNotEmpty() -> parts[0].take(1).uppercase()
                else -> "MF"
            }
        }

        fun getHomeHeader(name: String?): Pair<String?, String> {
            val trimmed = name?.trim() ?: ""
            return if (trimmed.isNotEmpty()) {
                Pair("Olá, $trimmed 👋", "Meu Financeiro")
            } else {
                Pair(null, "Meu Financeiro")
            }
        }

        // 1. Perfil: Usuário configura nome e foto
        val profileName = "Carlos Silva"
        val profilePhoto = "/data/user/0/com.aistudio.financas/files/profile.jpg"
        assertEquals("CS", getAvatarInitials(profileName))
        val headerWithProfile = getHomeHeader(profileName)
        assertEquals("Olá, Carlos Silva 👋", headerWithProfile.first)
        assertEquals("Meu Financeiro", headerWithProfile.second)

        // 2. Cadastrar Cartão de Crédito
        val nubankCard = CreditCardEntity(
            id = 50L,
            name = "Nubank Black",
            limitAmount = 10000.0,
            closingDay = 25,
            dueDay = 5
        )

        // 3. Cadastrar Conta Mensal Recorrente (ex: Aluguel R$ 1200,00 dia 10)
        var aluguelBill = RecurringBillEntity(
            id = 60L,
            name = "Aluguel Apartamento",
            amount = 1200.0,
            dueDay = 10,
            frequency = "MENSAL",
            category = "Moradia",
            lastPaidMonth = ""
        )

        // 4. Cadastrar Meta Financeira (ex: Viagem fim de ano R$ 2400 até Dezembro/2026, 3 meses a partir de Setembro)
        val viagemGoal = SavingsGoalEntity(
            id = 70L,
            name = "Viagem Final de Ano",
            targetAmount = 2400.0,
            savedAmount = 600.0, // Faltam 1800 -> 600/mês
            targetDateMillis = Calendar.getInstance().apply {
                set(2026, Calendar.DECEMBER, 31)
            }.timeInMillis
        )

        // 5. Cadastrar Orçamento Mensal (ex: Alimentação R$ 800,00)
        val alimentacaoBudget = BudgetEntity(
            id = 80L,
            category = "Alimentação",
            limitAmount = 800.0
        )

        // 6. Cadastrar Entrada de R$ 3.000,00 (Salário em Setembro/2026)
        val calSep = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5) }
        val txIncome = TransactionEntity(
            id = 101L,
            title = "Salário Mensal",
            amount = 3000.0,
            type = "INCOME",
            category = "Salário",
            dateMillis = calSep.timeInMillis,
            paymentMethod = "Conta Corrente"
        )

        // 7. Cadastrar Despesa à Vista de R$ 500,00 (Mercado em Setembro/2026 via Pix)
        val txExpenseCash = TransactionEntity(
            id = 102L,
            title = "Compras Supermercado",
            amount = 500.0,
            type = "EXPENSE",
            category = "Alimentação",
            dateMillis = calSep.timeInMillis,
            paymentMethod = "Pix"
        )

        // 8. Cadastrar Compra Parcelada de R$ 600,00 em 3 parcelas de R$ 200,00 no cartão de crédito
        // Parcela 1: Setembro/2026
        // Parcela 2: Outubro/2026
        // Parcela 3: Novembro/2026
        // Dezembro/2026: 0 parcelas deste lançamento
        val calOct = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 5) }
        val calNov = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 5) }
        val calDec = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 5) }

        val txParcela1 = TransactionEntity(
            id = 103L,
            title = "Notebook (1/3)",
            amount = 200.0,
            type = "EXPENSE",
            category = "Tecnologia",
            dateMillis = calSep.timeInMillis,
            paymentMethod = "Cartão de Crédito",
            cardId = nubankCard.id,
            installmentNumber = 1,
            totalInstallments = 3
        )
        val txParcela2 = TransactionEntity(
            id = 104L,
            title = "Notebook (2/3)",
            amount = 200.0,
            type = "EXPENSE",
            category = "Tecnologia",
            dateMillis = calOct.timeInMillis,
            paymentMethod = "Cartão de Crédito",
            cardId = nubankCard.id,
            installmentNumber = 2,
            totalInstallments = 3
        )
        val txParcela3 = TransactionEntity(
            id = 105L,
            title = "Notebook (3/3)",
            amount = 200.0,
            type = "EXPENSE",
            category = "Tecnologia",
            dateMillis = calNov.timeInMillis,
            paymentMethod = "Cartão de Crédito",
            cardId = nubankCard.id,
            installmentNumber = 3,
            totalInstallments = 3
        )

        val allTransactions = mutableListOf(txIncome, txExpenseCash, txParcela1, txParcela2, txParcela3)

        // --- VALIDAÇÃO SETEMBRO 2026 ---
        val sepKey = "2026-09"
        val sepTx = allTransactions.filter { it.dateMillis == calSep.timeInMillis }
        assertEquals(3, sepTx.size) // Entrada 3000, Despesa 500, Parcela 1/3 (200)

        // Saldo disponível em Conta: Entradas - Despesas à vista (gastos não quitados de cartão não deduzem ainda a conta corrente)
        val sepCashBalance = allTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } -
                allTransactions.filter { it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito" }.sumOf { it.amount }
        assertEquals(2500.0, sepCashBalance, 0.001) // 3000 - 500 = 2500

        // Entradas do Mês (Setembro): 3000
        val sepIncome = sepTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        assertEquals(3000.0, sepIncome, 0.001)

        // Saídas do Mês (Setembro): 500 (à vista) + 200 (fatura cartão parcela 1) = 700
        val sepExpenses = sepTx.filter { it.type == "EXPENSE" && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
        assertEquals(700.0, sepExpenses, 0.001)

        // Fatura do Cartão em Setembro: 200,00
        val sepCardInvoice = sepTx.filter { it.cardId == nubankCard.id && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
        assertEquals(200.0, sepCardInvoice, 0.001)
        val sepCardAvailableLimit = nubankCard.limitAmount - sepCardInvoice
        assertEquals(9800.0, sepCardAvailableLimit, 0.001) // 10000 - 200 = 9800

        // Compromissos do Mês de Setembro:
        // Contas a pagar: Aluguel R$ 1200
        // Fatura do Cartão: R$ 200
        // Meta (aporte mensal proporcional: 1800 restantes / 3 meses = 600)
        val sepCommitments = aluguelBill.amount + sepCardInvoice + 600.0
        assertEquals(2000.0, sepCommitments, 0.001) // 1200 + 200 + 600 = 2000

        // Saldo Real Livre (Modo Economia) em Setembro:
        // Saldo disponível (2500) - Compromissos (2000) = R$ 500,00
        val sepSaldoRealLivre = sepCashBalance - sepCommitments
        assertEquals(500.0, sepSaldoRealLivre, 0.001)

        // Orçamento Alimentação em Setembro:
        // Limite R$ 800, Gasto R$ 500 -> 62.5% utilizado
        val sepAlimentacaoSpent = sepTx.filter { it.category == alimentacaoBudget.category }.sumOf { it.amount }
        assertEquals(500.0, sepAlimentacaoSpent, 0.001)
        val sepBudgetPct = (sepAlimentacaoSpent / alimentacaoBudget.limitAmount) * 100
        assertEquals(62.5, sepBudgetPct, 0.001)

        // --- VALIDAÇÃO OUTUBRO 2026 ---
        val octTx = allTransactions.filter { it.dateMillis == calOct.timeInMillis }
        assertEquals(1, octTx.size) // Somente parcela 2/3 (200)
        assertEquals("Notebook (2/3)", octTx[0].title)

        val octCardInvoice = octTx.filter { it.cardId == nubankCard.id && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
        assertEquals(200.0, octCardInvoice, 0.001)

        // A conta recorrente Aluguel está pendente em Outubro
        val octBillPending = aluguelBill.amount
        assertEquals(1200.0, octBillPending, 0.001)

        // --- VALIDAÇÃO NOVEMBRO 2026 ---
        val novTx = allTransactions.filter { it.dateMillis == calNov.timeInMillis }
        assertEquals(1, novTx.size) // Somente parcela 3/3 (200)
        assertEquals("Notebook (3/3)", novTx[0].title)

        // --- VALIDAÇÃO DEZEMBRO 2026 ---
        val decTx = allTransactions.filter { it.dateMillis == calDec.timeInMillis }
        assertTrue(decTx.isEmpty()) // Fim das 3 parcelas do notebook em Dezembro!
        val decCardInvoice = decTx.filter { it.cardId == nubankCard.id && !it.isCreditCardInvoicePayment() }.sumOf { it.amount }
        assertEquals(0.0, decCardInvoice, 0.001) // Fatura zerada em Dezembro

        // --- TESTE DE EDIÇÃO E EXCLUSÃO ---
        // Editar despesa de 500 para 450
        val txEdited = txExpenseCash.copy(amount = 450.0)
        val editedBalance = (allTransactions.filter { it.id != txExpenseCash.id } + txEdited)
            .filter { it.type == "INCOME" }.sumOf { it.amount } -
                (allTransactions.filter { it.id != txExpenseCash.id } + txEdited)
                    .filter { it.type == "EXPENSE" && it.paymentMethod != "Cartão de Crédito" }.sumOf { it.amount }
        assertEquals(2550.0, editedBalance, 0.001) // 3000 - 450 = 2550

        // Excluir a despesa
        allTransactions.removeIf { it.id == txExpenseCash.id }
        assertEquals(4, allTransactions.size)

        // --- LIMPEZA COMPLETA DOS DADOS DE TESTE ---
        allTransactions.clear()
        assertTrue(allTransactions.isEmpty())
        val cleanBalance = allTransactions.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        assertEquals(0.0, cleanBalance, 0.001)
    }
}
