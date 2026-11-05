package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route
import kotlinx.coroutines.launch

/** "Card" tab: shows the KYC welcome screen until a card is activated, then the card home. */
@Composable
fun VisaPayRoute(navController: NavController) {
    val card by VisaPayRepository.card.collectAsState()
    val payments by VisaPayRepository.payments.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .statusBarsPadding()
    ) {
        val activeCard = card
        if (activeCard == null) {
            VisaPayWelcome(onStart = { navController.navigate(Route.VisaPayOnboarding.path) })
        } else {
            VisaPayCardHome(
                card = activeCard,
                payments = payments,
                onScan = { navController.navigate(Route.ScanUri.path) },
                onReset = VisaPayRepository::reset
            )
        }
    }
}

private data class WelcomeStep(val title: String, val subtitle: String)

private val welcomeSteps = listOf(
    WelcomeStep("Personal details", "Your legal name, date of birth and nationality"),
    WelcomeStep("Home address", "Where we send your physical card"),
    WelcomeStep("Photo ID and selfie", "A passport, ID card or driving licence"),
    WelcomeStep("Review and submit", "We usually reply within a few minutes"),
)

@Composable
private fun VisaPayWelcome(onStart: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.spacing6)
                .padding(top = spacing.spacing10, bottom = spacing.spacing6),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing7)
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.spacing13)
                    .clip(WCTheme.borderRadius.shapeLarge)
                    .background(colors.bgAccentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_walletconnect_brandmark),
                    contentDescription = null,
                    modifier = Modifier.size(width = spacing.spacing9, height = spacing.spacing6),
                    colorFilter = ColorFilter.tint(colors.textInvert)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
                Text(text = "Get your card in a few minutes", style = WCTheme.typography.h4Regular.copy(color = colors.textPrimary))
                VisaPayBody(
                    "Before we can issue your card, we need to confirm who you are. This is required by law and helps keep your account safe."
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing4)) {
                welcomeSteps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
                        Box(
                            modifier = Modifier
                                .size(spacing.spacing7)
                                .clip(CircleShape)
                                .background(colors.foregroundAccentPrimary10Solid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = WCTheme.typography.bodyMdMedium.copy(color = colors.textAccentPrimary)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                            Text(text = step.title, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
                            Text(text = step.subtitle, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = spacing.spacing6, vertical = spacing.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3)
        ) {
            VisaPayPrimaryButton("Start verification", onClick = onStart)
            Text(text = "Takes about 5 minutes", style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
    }
}

@Composable
private fun VisaPayCardHome(
    card: VisaPayCard,
    payments: List<VisaPayPayment>,
    onScan: () -> Unit,
    onReset: () -> Unit,
) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val firstName = card.cardholderName.trim().substringBefore(" ").ifBlank { "there" }
    val limitLabel = formatUsdc(card.dailyLimit.toDouble())
    val walletShort = "${card.fundingWallet.take(6)}...${card.fundingWallet.takeLast(4)}"

    // Pull what is left today and recent authorizations from vpay-server whenever the card home opens.
    LaunchedEffect(card.id) { runCatching { VisaPayRepository.refreshActivity() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing8, bottom = spacing.spacing6),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing6)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hi, $firstName",
                modifier = Modifier.weight(1f),
                style = WCTheme.typography.h4Regular.copy(color = colors.textPrimary)
            )
            ScanButton(onClick = onScan)
        }
        VisaPayCardVisual {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                Text(
                    text = "Left to spend today",
                    style = WCTheme.typography.bodyMdRegular.copy(color = colors.textInvert.copy(alpha = 0.8f))
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatUsdc(card.leftToSpend),
                        modifier = Modifier.weight(1f),
                        style = WCTheme.typography.h4Regular.copy(color = colors.textInvert)
                    )
                    Text(text = "VISA", style = WCTheme.typography.h6Medium.copy(color = colors.textInvert))
                }
            }
        }
        VisaPaySurface {
            HomeRow("Card") { VisaPayBadge("Active", VisaPayBadgeTone.SUCCESS) }
            Divider(color = colors.borderPrimary)
            HomeRow("Card number") { HomeValue("•••• ${card.lastDigits}") }
            Divider(color = colors.borderPrimary)
            HomeRow("Daily limit") { HomeValue(limitLabel) }
            Divider(color = colors.borderPrimary)
            HomeRow("Card balance") { HomeValue("${formatUsdc(card.balance.available)} available") }
            Divider(color = colors.borderPrimary)
            HomeRow("Funding wallet") { HomeValue("$walletShort · ${card.network.label}", monospace = true) }
            Divider(color = colors.borderPrimary)
            HomeRow("Identity") { VisaPayBadge("Verified", VisaPayBadgeTone.NEUTRAL) }
        }
        AddFundsRow()
        PayOnlineSection(card)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            Text(text = "Recent activity", style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            if (payments.isEmpty()) {
                VisaPayBody("No transactions yet. Scan a merchant's QR code to pay with your card.")
            } else {
                VisaPaySurface {
                    payments.forEachIndexed { index, payment ->
                        if (index > 0) Divider(color = colors.borderPrimary)
                        PaymentRow(payment)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VisaPayTextButton("Reset card demo", onReset)
        }
    }
}

@Composable
private fun HomeRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WCTheme.spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textPrimary)
        )
        value()
    }
}

@Composable
private fun HomeValue(text: String, monospace: Boolean = false) {
    val style = WCTheme.typography.bodyMdMedium.copy(color = WCTheme.colors.textPrimary)
    Text(text = text, style = if (monospace) style.copy(fontFamily = FontFamily.Monospace) else style)
}

@Composable
private fun PaymentRow(payment: VisaPayPayment) {
    val colors = WCTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WCTheme.spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing05)) {
            Text(text = payment.merchant, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = payment.category, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing05)) {
            Text(
                text = if (payment.isApproved) "-${formatUsdc(payment.amount)}" else formatUsdc(payment.amount),
                style = WCTheme.typography.bodyMdMedium.copy(color = if (payment.isApproved) colors.textPrimary else colors.textTertiary)
            )
            if (!payment.isApproved) {
                Text(
                    text = declineReason(payment.responseCode),
                    style = WCTheme.typography.bodySmRegular.copy(color = colors.textError)
                )
            }
        }
    }
}

/** Demo stand-in for a deposit: payments need the amount within the card's available balance. */
@Composable
private fun AddFundsRow() {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing1)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VisaPayTextButton(if (isLoading) "Adding funds…" else "Add ${formatUsdc(DEMO_TOP_UP_AMOUNT)}") {
                if (isLoading) return@VisaPayTextButton
                isLoading = true
                error = null
                scope.launch {
                    runCatching { VisaPayRepository.addFunds(DEMO_TOP_UP_AMOUNT) }.onFailure { error = it.message }
                    isLoading = false
                }
            }
        }
        error?.let { Text(text = it, style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = WCTheme.colors.textError)) }
    }
}

private const val DEMO_TOP_UP_AMOUNT = 100.0

/**
 * The two ways to pay online at the demo store (visa-pay-store): "Pay with Visa Pay" finds the card by the
 * registered email, and the card form takes the card number, expiry and one-time code.
 */
@Composable
private fun PayOnlineSection(card: VisaPayCard) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
        Text(text = "Pay online", style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
        CheckoutEmailSection(card.checkoutEmail)
        CardDetailsSection()
    }
}

/** Email a shop finds the card by. Changing it updates the Visa Pay enrollment on vpay-server. */
@Composable
private fun CheckoutEmailSection(registeredEmail: String) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var input by remember(registeredEmail) { mutableStateOf(registeredEmail) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
        Text(text = "With your email", style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary))
        if (!isEditing) {
            VisaPaySurface {
                HomeRow("Checkout email") { HomeValue(registeredEmail) }
            }
            Text(
                text = "Choose Pay with Visa Pay at checkout and enter this email.",
                style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
            )
            VisaPayTextButton("Change email") { isEditing = true }
        } else {
            VisaPayTextField(
                label = "Checkout email",
                value = input,
                onValueChange = { input = it },
                placeholder = "name@example.com",
                keyboardType = KeyboardType.Email,
            )
            VisaPayPrimaryButton(if (isSaving) "Saving…" else "Save email", enabled = !isSaving && input.contains("@")) {
                isSaving = true
                error = null
                scope.launch {
                    runCatching { VisaPayRepository.updateCheckoutEmail(input) }
                        .onSuccess { isEditing = false }
                        .onFailure { error = it.message }
                    isSaving = false
                }
            }
            VisaPayTextButton("Cancel") {
                isEditing = false
                input = registeredEmail
                error = null
            }
        }
        error?.let { Text(text = it, style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = colors.textError)) }
    }
}

/**
 * Card number, expiry and one-time code for paying online, fetched from vpay-server on demand.
 * Each fetch gives a new code and invalidates the previous one, so it is only loaded when asked for.
 */
@Composable
private fun CardDetailsSection() {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var details by remember { mutableStateOf<VisaPayCardDetails?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        isLoading = true
        error = null
        scope.launch {
            runCatching { VisaPayRepository.cardDetails() }
                .onSuccess { details = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
        Text(text = "With your card details", style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary))
        val shown = details
        if (shown == null) {
            VisaPayBody("Show your card number, expiry and a one-time security code to pay at an online checkout.")
            VisaPayPrimaryButton(if (isLoading) "Loading…" else "Show card details", enabled = !isLoading, onClick = ::load)
        } else {
            VisaPaySurface {
                HomeRow("Card number") {
                    Text(
                        text = shown.number.chunked(4).joinToString(" "),
                        modifier = Modifier.clickable { clipboard.setText(AnnotatedString(shown.number)) },
                        style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                    )
                }
                Divider(color = colors.borderPrimary)
                HomeRow("Expiry") { HomeValue(shown.expiry, monospace = true) }
                Divider(color = colors.borderPrimary)
                HomeRow("Security code") { HomeValue(shown.cvv, monospace = true) }
            }
            Text(
                text = "The security code works once. Tap the number to copy it.",
                style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
                VisaPayTextButton("New code", ::load)
                VisaPayTextButton("Hide") { details = null }
            }
        }
        error?.let { Text(text = it, style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = colors.textError)) }
    }
}

/** Same look as the scan button in the Wallets header; opens the QR scanner to pay with the card. */
@Composable
private fun ScanButton(onClick: () -> Unit) {
    val colors = WCTheme.colors
    Box(
        modifier = Modifier
            .size(WCTheme.spacing.spacing10)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(colors.bgInvert)
            .clickable(onClick = onClick)
            .testTag("button-visapay-scan"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_qr_code),
            contentDescription = "Scan to pay",
            modifier = Modifier.size(WCTheme.spacing.spacing5),
            tint = colors.bgPrimary
        )
    }
}
