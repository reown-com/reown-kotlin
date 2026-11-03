package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

/** "Card" tab: shows the KYC welcome screen until a card is activated, then the card home. */
@Composable
fun VisaPayRoute(navController: NavController) {
    val card by VisaPayMockRepository.card.collectAsState()
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
            VisaPayCardHome(card = activeCard, onReset = VisaPayMockRepository::reset)
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
private fun VisaPayCardHome(card: VisaPayCard, onReset: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val firstName = card.cardholderName.trim().substringBefore(" ").ifBlank { "there" }
    val limitLabel = "%,d ${VisaPayMocks.CARD_CURRENCY}".format(card.dailyLimit)
    val walletShort = "${card.fundingWallet.take(6)}...${card.fundingWallet.takeLast(4)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing8, bottom = spacing.spacing6),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing6)
    ) {
        Text(text = "Hi, $firstName", style = WCTheme.typography.h4Regular.copy(color = colors.textPrimary))
        VisaPayCardVisual {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                Text(
                    text = "Left to spend today",
                    style = WCTheme.typography.bodyMdRegular.copy(color = colors.textInvert.copy(alpha = 0.8f))
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = limitLabel,
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
            HomeRow("Funding wallet") { HomeValue("$walletShort · ${card.network.label}", monospace = true) }
            Divider(color = colors.borderPrimary)
            HomeRow("Identity") { VisaPayBadge("Verified", VisaPayBadgeTone.NEUTRAL) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            Text(text = "Recent activity", style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            VisaPayBody("No transactions yet. Your first card payment will show here.")
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
