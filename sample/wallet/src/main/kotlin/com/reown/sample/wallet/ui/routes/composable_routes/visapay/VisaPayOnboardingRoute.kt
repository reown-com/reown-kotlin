package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

private const val TOTAL_FORM_STEPS = 4

@Composable
fun VisaPayOnboardingRoute(
    navController: NavController,
    viewModel: VisaPayOnboardingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val onBack: () -> Unit = { if (!viewModel.back()) navController.popBackStack() }
    BackHandler(onBack = onBack)

    // Drop focus when the step changes so the keyboard never carries over to the next screen.
    LaunchedEffect(state.step) { focusManager.clearFocus() }

    // Card is live: return to the Card tab, which now shows the card home.
    LaunchedEffect(state.activationStage) {
        if (state.activationStage == VisaPayActivationStage.DONE) {
            navController.popBackStack(Route.VisaPay.path, inclusive = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Every step fills the screen, so the transition must not clip to a stale, smaller size:
        // that clipping hid the pinned footer (and its Continue button) on some steps.
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(200)) togetherWith fadeOut(tween(200))) using SizeTransform(clip = false)
            },
            label = "visapay_step"
        ) { step ->
            Column(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    VisaPayStep.DETAILS, VisaPayStep.ADDRESS, VisaPayStep.IDENTITY, VisaPayStep.REVIEW ->
                        StepHeader(step = step.number ?: 0, onBack = onBack)

                    VisaPayStep.SETUP, VisaPayStep.KYC_FAILED -> BackOnlyHeader(onBack = onBack)
                    VisaPayStep.IN_REVIEW, VisaPayStep.APPROVED, VisaPayStep.ACTIVATING -> Unit
                }
                when (step) {
                    VisaPayStep.DETAILS -> DetailsStep(state, viewModel)
                    VisaPayStep.ADDRESS -> AddressStep(state, viewModel)
                    VisaPayStep.IDENTITY -> IdentityStep(state, viewModel)
                    VisaPayStep.REVIEW -> ReviewStep(state, viewModel)
                    VisaPayStep.IN_REVIEW -> InReviewStep()
                    VisaPayStep.KYC_FAILED -> KycFailedStep(
                        title = if (state.isServiceError) "Couldn't reach Visa Pay" else "We couldn't verify you yet",
                        reason = state.kycFailure.orEmpty(),
                        onTryAgain = viewModel::submitForReview,
                        onEdit = { viewModel.goTo(VisaPayStep.REVIEW) }
                    )
                    VisaPayStep.APPROVED -> ApprovedStep(state, onSetUp = { viewModel.goTo(VisaPayStep.SETUP) })
                    VisaPayStep.SETUP -> SetupStep(state, viewModel)
                    VisaPayStep.ACTIVATING -> ActivatingStep(
                        state = state,
                        onRetry = viewModel::activateCard,
                        onBack = { viewModel.goTo(VisaPayStep.SETUP) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int, onBack: () -> Unit) {
    val spacing = WCTheme.spacing
    Column(
        modifier = Modifier.padding(start = spacing.spacing3, end = spacing.spacing6, top = spacing.spacing2),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Text(
                text = "Step $step of $TOTAL_FORM_STEPS",
                style = WCTheme.typography.bodyMdMedium.copy(color = WCTheme.colors.textTertiary)
            )
        }
        LinearProgressIndicator(
            progress = step / TOTAL_FORM_STEPS.toFloat(),
            modifier = Modifier
                .padding(start = spacing.spacing3)
                .fillMaxWidth()
                .height(spacing.spacing1)
                .clip(WCTheme.borderRadius.shapeFull),
            color = WCTheme.colors.bgAccentPrimary,
            backgroundColor = WCTheme.colors.foregroundSecondary
        )
    }
}

@Composable
private fun BackOnlyHeader(onBack: () -> Unit) {
    Box(modifier = Modifier.padding(start = WCTheme.spacing.spacing3, top = WCTheme.spacing.spacing2)) {
        BackButton(onBack)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(WCTheme.spacing.spacing11 - WCTheme.spacing.spacing1)
            .clip(CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = "Back",
            tint = WCTheme.colors.textPrimary,
            modifier = Modifier.size(WCTheme.spacing.spacing4)
        )
    }
}

/** Scrollable form body with a pinned footer holding the primary action. */
@Composable
private fun ColumnScope.FormScaffold(
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = WCTheme.spacing
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6, vertical = spacing.spacing3),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing5)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayTitle(title)
            VisaPayBody(subtitle)
        }
        content()
    }
    Divider(color = WCTheme.colors.borderPrimary)
    Box(modifier = Modifier.padding(horizontal = spacing.spacing6, vertical = spacing.spacing4)) {
        footer()
    }
}

@Composable
private fun ColumnScope.DetailsStep(state: VisaPayOnboardingState, viewModel: VisaPayOnboardingViewModel) {
    FormScaffold(
        title = "Your personal details",
        subtitle = "Enter your name exactly as it appears on your ID.",
        footer = {
            VisaPayPrimaryButton("Continue", enabled = state.isDetailsValid) { viewModel.goTo(VisaPayStep.ADDRESS) }
        }
    ) {
        VisaPayTextField("Full legal name", state.fullName, viewModel::onFullNameChange, placeholder = "First and last name")
        VisaPayTextField(
            "Email", state.email, viewModel::onEmailChange,
            placeholder = "name@example.com", keyboardType = KeyboardType.Email
        )
        VisaPayTextField(
            "Date of birth", state.dateOfBirth, viewModel::onDateOfBirthChange,
            placeholder = "DD/MM/YYYY", keyboardType = KeyboardType.Number
        )
        VisaPaySelectField("Nationality", state.nationality, VisaPayMocks.countries, viewModel::onNationalityChange)
    }
}

@Composable
private fun ColumnScope.AddressStep(state: VisaPayOnboardingState, viewModel: VisaPayOnboardingViewModel) {
    FormScaffold(
        title = "Your home address",
        subtitle = "We send your physical card here. Use the address where you live now.",
        footer = {
            VisaPayPrimaryButton("Continue", enabled = state.isAddressValid) { viewModel.goTo(VisaPayStep.IDENTITY) }
        }
    ) {
        VisaPayTextField("Address line 1", state.street, viewModel::onStreetChange, placeholder = "Street and house number")
        VisaPayTextField("Address line 2 (optional)", state.street2, viewModel::onStreet2Change, placeholder = "Flat, floor or building")
        Row(horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing3)) {
            Box(modifier = Modifier.weight(1f)) { VisaPayTextField("City", state.city, viewModel::onCityChange) }
            Box(modifier = Modifier.weight(1f)) { VisaPayTextField("Postcode", state.postcode, viewModel::onPostcodeChange) }
        }
        VisaPaySelectField("Country", state.country, VisaPayMocks.countries, viewModel::onCountryChange)
    }
}

@Composable
private fun ColumnScope.IdentityStep(state: VisaPayOnboardingState, viewModel: VisaPayOnboardingViewModel) {
    val spacing = WCTheme.spacing
    FormScaffold(
        title = "Verify your identity",
        subtitle = "Use a valid document in good light. Keep all 4 corners in view and make sure the text is sharp.",
        footer = {
            VisaPayPrimaryButton("Continue", enabled = state.isIdentityValid) { viewModel.goTo(VisaPayStep.REVIEW) }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayLabel("Document type")
            VisaPayPillRow(
                items = VisaPayDocumentType.entries,
                selected = state.documentType,
                label = { it.label },
                onSelect = viewModel::onDocumentTypeChange
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
            state.requiredCaptures.forEach { capture ->
                val copy = captureCopy(capture, state.documentType)
                if (capture in state.captures) {
                    CapturedItem(capture, copy.doneLabel, onRetake = { viewModel.retake(capture) })
                } else {
                    VisaPayDashedAction(
                        icon = R.drawable.ic_camera,
                        title = copy.label,
                        subtitle = copy.hint,
                        onClick = { viewModel.capture(capture) }
                    )
                }
            }
        }
    }
}

private data class CaptureCopy(val label: String, val hint: String, val doneLabel: String)

private fun captureCopy(capture: VisaPayCapture, documentType: VisaPayDocumentType): CaptureCopy = when (capture) {
    VisaPayCapture.FRONT -> if (documentType.needsBack) {
        CaptureCopy("Photograph the front", "Tap to open camera", "Front added")
    } else {
        CaptureCopy("Photograph the photo page", "Tap to open camera", "Photo page added")
    }

    VisaPayCapture.BACK -> CaptureCopy("Photograph the back", "Tap to open camera", "Back added")
    VisaPayCapture.SELFIE -> CaptureCopy("Take a selfie", "We match it to your ID photo", "Selfie added")
}

@Composable
private fun CapturedItem(capture: VisaPayCapture, doneLabel: String, onRetake: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    VisaPaySurface {
        Row(
            modifier = Modifier.padding(vertical = spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
        ) {
            VisaPayIconTile(
                icon = if (capture == VisaPayCapture.SELFIE) R.drawable.ic_user else R.drawable.ic_id_document,
                circle = capture == VisaPayCapture.SELFIE
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.spacing05)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                    VisaPayCheckIcon()
                    Text(text = doneLabel, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
                }
                Text(text = "Looks clear and readable", style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
            }
            VisaPayTextButton("Retake", onRetake)
        }
    }
}

@Composable
private fun ColumnScope.ReviewStep(state: VisaPayOnboardingState, viewModel: VisaPayOnboardingViewModel) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    FormScaffold(
        title = "Check your details",
        subtitle = "Make sure everything matches your ID. Mismatches are the most common reason for delays.",
        footer = { VisaPayPrimaryButton("Submit for review", enabled = state.consent, onClick = viewModel::submitForReview) }
    ) {
        VisaPaySurface {
            ReviewRow(
                caption = "Personal details",
                title = state.fullName,
                subtitle = "Born ${state.dateOfBirth} · ${state.nationality}",
                onEdit = { viewModel.goTo(VisaPayStep.DETAILS) }
            )
            Divider(color = colors.borderPrimary)
            ReviewRow(
                caption = "Home address",
                title = state.addressLine,
                subtitle = state.cityLine,
                onEdit = { viewModel.goTo(VisaPayStep.ADDRESS) }
            )
            Divider(color = colors.borderPrimary)
            ReviewRow(
                caption = "Identity",
                title = state.documentType.label,
                subtitle = "Photos and selfie added",
                onEdit = { viewModel.goTo(VisaPayStep.IDENTITY) }
            )
        }
        Row(
            modifier = Modifier
                .clip(WCTheme.borderRadius.shapeMedium)
                .clickable { viewModel.onConsentChange(!state.consent) },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
        ) {
            Checkbox(
                checked = state.consent,
                onCheckedChange = viewModel::onConsentChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.bgAccentPrimary,
                    uncheckedColor = colors.borderSecondary,
                    checkmarkColor = colors.textInvert
                )
            )
            Text(
                text = "I confirm these details are correct and agree to identity and address checks with ${VisaPayMocks.KYC_PROVIDER}.",
                modifier = Modifier.padding(top = spacing.spacing3),
                style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = colors.textPrimary)
            )
        }
    }
}

@Composable
private fun ReviewRow(caption: String, title: String, subtitle: String, onEdit: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Row(modifier = Modifier.padding(vertical = spacing.spacing3), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
            Text(text = caption, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
            Text(text = title, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = subtitle, style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
        VisaPayTextButton("Edit", onEdit)
    }
}

@Composable
private fun InReviewStep() {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing13 + spacing.spacing8, bottom = spacing.spacing8),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing7)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing5)
        ) {
            VisaPayReviewDots()
            VisaPayBadge("In review", VisaPayBadgeTone.ACCENT)
            Text(
                text = "We're checking your details",
                textAlign = TextAlign.Center,
                style = WCTheme.typography.h5Regular.copy(color = colors.textPrimary)
            )
            VisaPayBody(
                text = "This usually takes a few minutes. You can leave the app, we'll send you a notification when it's done.",
                modifier = Modifier.padding(horizontal = spacing.spacing4)
            )
        }
        VisaPaySurface {
            TimelineRow(TimelineState.DONE, "Application received", "Just now")
            TimelineRow(TimelineState.ACTIVE, "Identity checks with the bank", "In progress")
            TimelineRow(TimelineState.TODO, "Issuing your Visa card", "Usually within a few seconds")
        }
    }
}

@Composable
private fun ActivatingStep(state: VisaPayOnboardingState, onRetry: () -> Unit, onBack: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val wallet = state.fundingWallet.trim()
    val walletShort = "${wallet.take(6)}...${wallet.takeLast(4)}"
    val allowance = "%,d ${VisaPayMocks.CARD_CURRENCY}".format(state.dailyLimitValue)
    val stage = state.activationStage

    fun stateOf(rowStage: VisaPayActivationStage): TimelineState = when {
        stage.ordinal > rowStage.ordinal -> TimelineState.DONE
        stage == rowStage -> TimelineState.ACTIVE
        else -> TimelineState.TODO
    }

    val title = when {
        state.activationError != null -> "Couldn't activate your card"
        stage == VisaPayActivationStage.PREPARING_PERMIT2 -> "Preparing your card"
        stage == VisaPayActivationStage.SIGNING_PERMIT2 -> "Approving card payments"
        else -> "Activating your card"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing13 + spacing.spacing8, bottom = spacing.spacing8),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing7)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing5)
        ) {
            VisaPayReviewDots()
            VisaPayBadge("Permit2", VisaPayBadgeTone.ACCENT)
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = WCTheme.typography.h5Regular.copy(color = colors.textPrimary)
            )
            VisaPayBody(
                text = "Your wallet is signing a Permit2 approval so card payments can be taken in " +
                    "${VisaPayMocks.CARD_CURRENCY}. This happens automatically, keep the app open.",
                modifier = Modifier.padding(horizontal = spacing.spacing4)
            )
        }
        VisaPaySurface {
            TimelineRow(
                stateOf(VisaPayActivationStage.PREPARING_PERMIT2),
                "Preparing spending approval",
                "${VisaPayMocks.CARD_CURRENCY} on ${state.network.label}"
            )
            TimelineRow(
                stateOf(VisaPayActivationStage.SIGNING_PERMIT2),
                "Signing Permit2 approval",
                "Up to $allowance a day from $walletShort"
            )
            TimelineRow(
                stateOf(VisaPayActivationStage.ACTIVATING_CARD),
                "Activating your card",
                "Daily limit, checkout email and funding"
            )
        }
        state.activationError?.let { error ->
            Text(text = error, style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = colors.textError))
            VisaPayPrimaryButton("Try again", onClick = onRetry)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VisaPayTextButton("Back to card setup", onBack)
            }
        }
    }
}

@Composable
private fun KycFailedStep(title: String, reason: String, onTryAgain: () -> Unit, onEdit: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing8, bottom = spacing.spacing6),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing5)
    ) {
        Text(text = title, style = WCTheme.typography.h5Regular.copy(color = colors.textPrimary))
        Text(text = reason, style = WCTheme.typography.bodyLgRegular.paragraph().copy(color = colors.textTertiary))
        VisaPayPrimaryButton("Try again", onClick = onTryAgain)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VisaPayTextButton("Check your details", onEdit)
        }
    }
}

@Composable
private fun ApprovedStep(state: VisaPayOnboardingState, onSetUp: () -> Unit) {
    val spacing = WCTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.spacing6)
            .padding(top = spacing.spacing13, bottom = spacing.spacing4),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing7)
    ) {
        VisaPayCardVisual {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing1)) {
                Text(
                    text = "•••• •••• •••• ${state.issuedCard?.last4 ?: VisaPayMocks.CARD_LAST_DIGITS}",
                    style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textInvert)
                )
                VisaPayCardFooter(left = state.fullName.ifBlank { "Cardholder name" }, right = "VISA")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
            VisaPayBadge("Approved", VisaPayBadgeTone.SUCCESS)
            Text(text = "You're approved", style = WCTheme.typography.h4Regular.copy(color = WCTheme.colors.textPrimary))
            VisaPayBody("Your card is ready. Set a daily limit and choose the wallet it spends from to start using it.")
        }
        Spacer(modifier = Modifier.weight(1f))
        VisaPayPrimaryButton("Set up your card", onClick = onSetUp)
    }
}

@Composable
private fun ColumnScope.SetupStep(
    state: VisaPayOnboardingState,
    viewModel: VisaPayOnboardingViewModel,
) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    val walletInput = state.fundingWallet.trim()
    FormScaffold(
        title = "Set up your card",
        subtitle = "Choose how much you can spend each day and where the money comes from. You can change this later.",
        footer = {
            VisaPayPrimaryButton(
                text = "Activate card",
                enabled = state.isSetupValid,
                onClick = viewModel::activateCard
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayLabel("Card currency")
            CurrencyRow()
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayTextField(
                label = "Daily spending limit",
                value = state.dailyLimit,
                onValueChange = viewModel::onDailyLimitChange,
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                trailing = {
                    Text(
                        text = VisaPayMocks.CARD_CURRENCY,
                        modifier = Modifier.padding(end = spacing.spacing2),
                        style = WCTheme.typography.bodyLgMedium.copy(color = colors.textTertiary)
                    )
                }
            )
            VisaPayPillRow(
                items = VisaPayMocks.dailyLimitPresets,
                selected = state.dailyLimitValue,
                label = { "%,d".format(it) },
                onSelect = viewModel::onDailyLimitPreset
            )
            Text(
                text = "Resets every day at midnight.",
                style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayTextField(
                label = "Funding wallet address",
                value = state.fundingWallet,
                onValueChange = viewModel::onFundingWalletChange,
                placeholder = "0x...",
                monospace = true,
                isError = walletInput.isNotEmpty() && !state.isWalletValid
            )
            when {
                walletInput.isEmpty() -> Unit
                state.isWalletValid -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
                ) {
                    VisaPayCheckIcon()
                    Text(
                        text = "Card payments will be taken in USDC from this wallet",
                        style = WCTheme.typography.bodyMdRegular.copy(color = colors.textSuccess)
                    )
                }

                else -> Text(
                    text = "Enter a valid address that starts with 0x and has 42 characters.",
                    style = WCTheme.typography.bodyMdRegular.copy(color = colors.textError)
                )
            }
            UseSampleWalletButton(address = viewModel.sampleWalletAddress, onClick = viewModel::useSampleWalletAddress)
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayTextField(
                label = "Email for online checkout",
                value = state.checkoutEmail,
                onValueChange = viewModel::onCheckoutEmailChange,
                placeholder = "name@example.com",
                keyboardType = KeyboardType.Email,
                isError = state.checkoutEmail.isNotEmpty() && !state.isCheckoutEmailValid
            )
            Text(
                text = "At an online checkout, choose Pay with Visa Pay and enter this email. We send a one-time code to confirm.",
                style = WCTheme.typography.bodyMdRegular.paragraph().copy(color = colors.textTertiary)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing2)) {
            VisaPayLabel("Network")
            VisaPayPillRow(
                items = VisaPayNetwork.entries,
                selected = state.network,
                label = { it.label },
                onSelect = viewModel::onNetworkChange
            )
            Text(
                text = "Make sure your USDC is on this network.",
                style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary)
            )
        }
    }
}

@Composable
private fun CurrencyRow() {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(colors.bgPrimary)
            .border(1.dp, colors.borderAccentPrimary, WCTheme.borderRadius.shapeLarge)
            .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
    ) {
        Box(
            modifier = Modifier.size(spacing.spacing9).clip(CircleShape).background(colors.bgAccentPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$", style = WCTheme.typography.bodyLgMedium.copy(color = colors.textInvert))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.spacing05)) {
            Text(text = VisaPayMocks.CARD_CURRENCY, style = WCTheme.typography.bodyLgMedium.copy(color = colors.textPrimary))
            Text(text = "USD Coin, 1 USDC = 1 USD", style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary))
        }
        VisaPayCheckIcon(colors.iconAccentPrimary)
    }
}

@Composable
private fun UseSampleWalletButton(address: String, onClick: () -> Unit) {
    val spacing = WCTheme.spacing
    val colors = WCTheme.colors
    VisaPaySurface(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(vertical = spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
        ) {
            VisaPayIconTile(R.drawable.ic_wallet)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing05)) {
                Text(text = "Use my Sample Wallet address", style = WCTheme.typography.bodyMdMedium.copy(color = colors.textPrimary))
                Text(
                    text = "${address.take(6)}...${address.takeLast(4)}",
                    style = WCTheme.typography.bodyMdRegular.copy(color = colors.textTertiary, fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
