package com.reown.sample.wallet.ui.routes.dialog_routes.payment

import android.content.Intent
import android.graphics.Bitmap
import com.reown.sample.wallet.BuildConfig
import android.net.Uri
import android.util.Log
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R
import com.reown.sample.wallet.payment.PaymentReviewFormatter
import com.reown.sample.wallet.payment.PaymentTransactionUtil
import com.reown.sample.wallet.payment.PaymentUtil
import com.reown.sample.wallet.payment.TransactionFeeEstimate
import com.reown.sample.wallet.ui.routes.Route
import com.reown.walletkit.client.Wallet
import org.json.JSONObject
import java.math.BigDecimal
import com.reown.sample.wallet.ui.common.Shimmer
import com.reown.sample.wallet.ui.common.WalletConnectLoader
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PaymentRoute(
    navController: NavHostController,
    paymentLink: String,
    onPaymentSuccess: () -> Unit = {},
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(paymentLink) {
        viewModel.setPaymentLink(paymentLink)
    }

    AnimatedContent(
        targetState = uiState,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "paymentState"
    ) { state ->
        // Paint the sheet background (the sheet itself is transparent) so it fills
        // behind the system nav bar, then inset content above it to avoid overlap.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WCTheme.colors.bgPrimary)
                .navigationBarsPadding()
        ) {
        when (state) {
            is PaymentUiState.WebViewDataCollection -> {
                WebViewDataCollectionContent(
                    url = state.url,
                    paymentInfo = state.paymentInfo,
                    onComplete = { viewModel.onICWebViewComplete() },
                    onError = { error ->
                        viewModel.onICWebViewError(error)
                    },
                    onBack = {
                        viewModel.goBackToOptions()
                    },
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.Loading -> {
                LoadingContent()
            }
            is PaymentUiState.Options -> {
                PaymentOptionsContent(
                    paymentInfo = state.paymentInfo,
                    options = state.options,
                    selectedOptionId = state.selectedOptionId,
                    gasEstimates = state.gasEstimates,
                    estimatingOptionIds = state.estimatingOptionIds,
                    onOptionSelected = { optionId ->
                        viewModel.onOptionSelected(optionId)
                    },
                    onWhyInfoRequired = { viewModel.showWhyInfoRequired() },
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.Summary -> {
                SummaryContent(
                    paymentInfo = state.paymentInfo,
                    selectedOption = state.selectedOption,
                    requiresApproval = state.requiresApproval,
                    approvalGasEstimate = state.approvalGasEstimate,
                    isEstimatingApprovalGas = state.isEstimatingApprovalGas,
                    canChangeOption = state.canChangeOption,
                    onConfirm = { viewModel.confirmFromSummary() },
                    onChangeOption = { viewModel.goBackToOptions() },
                    onGasFeeInfo = { viewModel.showGasFeeInfo() },
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.WhyInfoRequired -> {
                WhyInfoRequiredContent(
                    onBack = { viewModel.dismissWhyInfoRequired() },
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.GasFeeInfo -> {
                GasFeeInfoContent(
                    selectedOption = state.selectedOption,
                    approvalGasEstimate = state.approvalGasEstimate,
                    onBack = { viewModel.dismissGasFeeInfo() },
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.Processing -> {
                val loading = getLoadingContent(state.step, state.setupTokenSymbol)
                ProcessingContent(
                    message = loading.message,
                    note = loading.note,
                )
            }
            is PaymentUiState.Success -> {
                SuccessContent(
                    paymentInfo = state.paymentInfo,
                    resultInfo = state.resultInfo,
                    onDone = {
                        viewModel.cancel()
                        onPaymentSuccess()
                        dismissPaymentDialog(navController)
                    }
                )
            }
            is PaymentUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    errorType = state.errorType,
                    onClose = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                    },
                    onScanNewQrCode = {
                        viewModel.cancel()
                        dismissPaymentDialog(navController)
                        navController.navigate(Route.ScannerOptions.path)
                    }
                )
            }
        }
        }
    }
}

private fun dismissPaymentDialog(navController: NavHostController) {
    if (!navController.popBackStack(Route.Wallets.path, inclusive = false)) {
        navController.popBackStack()
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(WCTheme.colors.bgPrimary)
            .padding(horizontal = WCTheme.spacing.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WalletConnectLoader(size = 120.dp)
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
        Text(
            text = getLoadingContent(LoadingStep.PREPARING).message,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("pay-loading-message")
        )
    }
}

@Composable
private fun PaymentOptionsContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    options: List<Wallet.Model.PaymentOption>,
    selectedOptionId: String?,
    gasEstimates: Map<String, TransactionFeeEstimate?>,
    estimatingOptionIds: Set<String>,
    onOptionSelected: (String) -> Unit,
    onWhyInfoRequired: () -> Unit,
    onClose: () -> Unit
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .heightIn(max = maxSheetHeight)
            .verticalScroll(rememberScrollState())
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Header: spacer (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(38.dp))
            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        // Top header: SelectToken icon + headline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pay-select-option-header"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_token),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(58.dp)
            )

            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

            Text(
                text = "Choose the asset you want to pay with",
                style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary)
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
        ) {
            options.forEachIndexed { index, option ->
                val hasCollectData = option.collectData?.url != null
                // Stable, network+token-keyed testTag for deterministic selection
                // (e.g. `pay-option-usdt-polygon`), additive to the order-dependent
                // `pay-option-$index`. Lets a test pick a specific asset+network when
                // several options share a token symbol across networks.
                val display = option.amount.display
                // Build from asset+network when available; fall back to the option's
                // unique id so the tag never collapses to a bare "pay-option-" (which
                // would collide across rows with missing display data). Locale.ROOT
                // keeps the tag identical regardless of device locale.
                val stableTagParts = listOfNotNull(display?.assetSymbol, display?.networkName)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val stableTagSuffix = if (stableTagParts.isEmpty()) {
                    option.id
                } else {
                    stableTagParts.joinToString("-")
                }
                val stableTag = "pay-option-" + stableTagSuffix
                    .lowercase(Locale.ROOT)
                    .replace(Regex("\\s+"), "-")
                Box(modifier = Modifier.testTag(stableTag)) {
                    PaymentOptionRow(
                        option = option,
                        feeEstimate = gasEstimates[option.id],
                        isEstimatingFee = estimatingOptionIds.contains(option.id),
                        onClick = { onOptionSelected(option.id) },
                        testTag = "pay-option-$index",
                        trailing = if (hasCollectData) {
                            {
                                BorderedIconButton(
                                    iconRes = R.drawable.ic_info,
                                    contentDescription = "Why info needed",
                                    onClick = onWhyInfoRequired,
                                    modifier = Modifier.testTag("pay-option-info-required")
                                )
                            }
                        } else null,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))

        // Bottom merchant footer
        MerchantFooter(paymentInfo = paymentInfo)
    }
}

@Composable
private fun MerchantFooter(paymentInfo: Wallet.Model.PaymentInfo?) {
    if (paymentInfo == null) return
    val merchantName = paymentInfo.merchant.name
    val amount = PaymentReviewFormatter.formatDisplayAmount(
        value = paymentInfo.amount.value,
        decimals = paymentInfo.amount.display?.decimals ?: 2,
        currencyCode = paymentInfo.amount.display?.assetSymbol ?: paymentInfo.amount.unit,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pay-merchant-info"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Pay $amount to $merchantName",
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary)
        )
        Spacer(modifier = Modifier.width(WCTheme.spacing.spacing2))
        MerchantIcon(paymentInfo = paymentInfo, size = 20.dp)
    }
}

@Composable
private fun PaymentOptionRow(
    option: Wallet.Model.PaymentOption,
    feeEstimate: TransactionFeeEstimate?,
    isEstimatingFee: Boolean,
    testTag: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val networkName = option.amount.display?.networkName?.lowercase() ?: "unknown"
    val requiresApproval = PaymentUtil.requiresApproval(option.actions)

    val containerModifier = Modifier
        .fillMaxWidth()
        .height(72.dp)
        .clip(WCTheme.borderRadius.shapeLarge)
        .background(WCTheme.colors.foregroundPrimary)
        .padding(horizontal = WCTheme.spacing.spacing4)

    Row(
        modifier = containerModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clickable main content. clearAndSetSemantics collapses this subtree into a single node
        // (testTag + networkName text + Button role) so `copyTextFrom`/`tapOn` resolve cleanly.
        // It is scoped to the main content only — a `trailing` marker keeps its own testTag and
        // stays discoverable (e.g. the KYC `pay-option-info-required` badge).
        val mainModifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .weight(1f)
            .fillMaxHeight()
            .clearAndSetSemantics {
                this.testTag = testTag
                text = AnnotatedString(networkName)
                if (onClick != null) role = Role.Button
            }

        Row(
            modifier = mainModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaymentAssetIcon(
                display = option.amount.display,
                tokenIconSize = 32.dp,
                networkIconSize = 18.dp,
                networkIconBorderWidth = 1.dp,
                networkIconBorderColor = WCTheme.colors.foregroundPrimary,
                useExternalNetworkBorder = true
            )
            Spacer(modifier = Modifier.width(WCTheme.spacing.spacing3))

            val display = option.amount.display
            val tokenAmount = formatTokenAmount(
                value = option.amount.value,
                decimals = display?.decimals ?: 18,
                symbol = display?.assetSymbol ?: "Token"
            )

            Text(
                text = tokenAmount,
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textPrimary)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing4)
        ) {
            when {
                feeEstimate != null && requiresApproval -> OptionGasEstimate(feeEstimate.display)
                isEstimatingFee && requiresApproval -> Shimmer(width = 70.dp, height = 16.dp)
            }
            if (trailing != null) trailing()
        }
    }
}

@Composable
private fun PaymentAssetIcon(
    display: Wallet.Model.PaymentAmountDisplay?,
    tokenIconSize: Dp,
    networkIconSize: Dp,
    networkIconBorderWidth: Dp = 2.dp,
    networkIconBorderColor: Color = Color.White,
    useExternalNetworkBorder: Boolean = false
) {
    val tokenIconUrl = display?.iconUrl?.takeIf { it.isNotBlank() }
    val networkIconUrl = display?.networkIconUrl?.takeIf { it.isNotBlank() }
    val symbol = display?.assetSymbol ?: "?"

    when {
        tokenIconUrl != null -> TokenIconWithNetwork(
            tokenIconUrl = tokenIconUrl,
            networkIconUrl = networkIconUrl,
            tokenIconSize = tokenIconSize,
            networkIconSize = networkIconSize,
            networkIconBorderWidth = networkIconBorderWidth,
            networkIconBorderColor = networkIconBorderColor,
            useExternalNetworkBorder = useExternalNetworkBorder
        )
        networkIconUrl != null -> AsyncImage(
            model = networkIconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(tokenIconSize)
                .clip(CircleShape)
        )
        else -> Box(
            modifier = Modifier
                .size(tokenIconSize)
                .clip(CircleShape)
                .background(WCTheme.colors.foregroundTertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(1).uppercase(Locale.ROOT),
                style = WCTheme.typography.bodyLgMedium.copy(color = WCTheme.colors.textPrimary)
            )
        }
    }
}

@Composable
private fun TokenIconWithNetwork(
    tokenIconUrl: String,
    networkIconUrl: String?,
    tokenIconSize: Dp,
    networkIconSize: Dp,
    networkIconBorderWidth: Dp = 1.dp,
    networkIconBorderColor: Color = Color.White,
    useExternalNetworkBorder: Boolean = false
) {
    // Badge overhangs the token's right/bottom edges to match Figma.
    val overhang = 2.dp
    Box(modifier = Modifier.size(tokenIconSize)) {
        AsyncImage(
            model = tokenIconUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
        networkIconUrl?.let { networkUrl ->
            if (useExternalNetworkBorder && networkIconBorderWidth > 0.dp) {
                val badgeSize = networkIconSize + (networkIconBorderWidth * 2)
                val badgeOffset = tokenIconSize - badgeSize + overhang
                Box(
                    modifier = Modifier
                        .size(badgeSize)
                        .offset(x = badgeOffset, y = badgeOffset)
                        .clip(CircleShape)
                        .background(networkIconBorderColor)
                        .padding(networkIconBorderWidth)
                ) {
                    AsyncImage(
                        model = networkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            } else {
                val badgeOffset = tokenIconSize - networkIconSize + overhang
                AsyncImage(
                    model = networkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(networkIconSize)
                        .offset(x = badgeOffset, y = badgeOffset)
                        .clip(CircleShape)
                        .border(networkIconBorderWidth, networkIconBorderColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun MerchantIcon(paymentInfo: Wallet.Model.PaymentInfo?, size: Dp) {
    paymentInfo?.merchant?.iconUrl?.let { iconUrl ->
        AsyncImage(
            model = iconUrl,
            contentDescription = "Merchant icon",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(Color.Black)
        )
    } ?: run {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = paymentInfo?.merchant?.name?.take(1)?.uppercase() ?: "P",
                style = WCTheme.typography.h4Regular.copy(color = Color.White)
            )
        }
    }
}

@Composable
private fun PaymentTitle(paymentInfo: Wallet.Model.PaymentInfo?) {
    val merchantName = paymentInfo?.merchant?.name ?: "Merchant"
    val displayAmount = paymentInfo?.let {
        formatDisplayAmount(
            value = it.amount.value,
            decimals = it.amount.display?.decimals ?: 2,
            symbol = it.amount.display?.assetSymbol ?: it.amount.unit
        )
    } ?: ""

    Text(
        text = "Pay $displayAmount to $merchantName",
        style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SummaryContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    selectedOption: Wallet.Model.PaymentOption,
    requiresApproval: Boolean,
    approvalGasEstimate: TransactionFeeEstimate?,
    isEstimatingApprovalGas: Boolean,
    canChangeOption: Boolean,
    onConfirm: () -> Unit,
    onChangeOption: () -> Unit,
    onGasFeeInfo: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pay-merchant-info"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantIcon(paymentInfo = paymentInfo, size = 64.dp)
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
            PaymentTitle(paymentInfo = paymentInfo)
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))

        val networkName = selectedOption.amount.display?.networkName?.lowercase() ?: "unknown"
        PaymentOptionRow(
            option = selectedOption,
            feeEstimate = approvalGasEstimate,
            isEstimatingFee = isEstimatingApprovalGas,
            testTag = "pay-review-token-$networkName",
            trailing = if (canChangeOption) {
                {
                    BorderedIconButton(
                        iconRes = R.drawable.ic_pencil,
                        contentDescription = "Change token",
                        onClick = onChangeOption,
                        iconSize = 18.dp,
                    )
                }
            } else null,
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing5))

        val payLabel = PaymentReviewFormatter.payButtonLabel(paymentInfo, approvalGasEstimate)

        PrimaryActionButton(
            primaryText = "Pay ${payLabel.total}",
            secondaryText = if (payLabel.includesGasFee) " (includes network fee)" else null,
            onClick = onConfirm,
            modifier = Modifier.testTag("pay-button-pay")
        )

        if (requiresApproval) {
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing3))
            Text(
                text = "Why does ${(selectedOption.amount.display?.assetSymbol ?: "this token").uppercase(Locale.ROOT)} need a network fee?",
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary),
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGasFeeInfo() }
            )
        }
    }
}

@Composable
private fun GasFeeInfoContent(
    selectedOption: Wallet.Model.PaymentOption,
    approvalGasEstimate: TransactionFeeEstimate?,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val tokenSymbol = selectedOption.amount.display?.assetSymbol?.uppercase(Locale.ROOT) ?: "THIS TOKEN"
    val gasText = approvalGasEstimate?.display ?: "Network fee set by wallet"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalIconButton(
                iconRes = R.drawable.ic_arrow_left,
                contentDescription = "Back",
                onClick = onBack,
                showBorder = false,
                modifier = Modifier.testTag("pay-button-back")
            )

            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PaymentAssetIcon(
                display = selectedOption.amount.display,
                tokenIconSize = 48.dp,
                networkIconSize = 18.dp,
                networkIconBorderWidth = 2.dp,
                networkIconBorderColor = WCTheme.colors.bgPrimary,
            )

            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

            Text(
                text = "Why does $tokenSymbol need a network fee?",
                style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

            Text(
                text = "The network fee covers a one-time setup so your wallet can pay with $tokenSymbol.",
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "You only pay it once. Future $tokenSymbol payments from this wallet skip this step.",
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.foregroundPrimary)
                .padding(horizontal = WCTheme.spacing.spacing4, vertical = WCTheme.spacing.spacing3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_gas_pump),
                contentDescription = null,
                tint = WCTheme.colors.iconDefault,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(WCTheme.spacing.spacing2))
            Text(
                text = "Network fee: $gasText",
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary)
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        PrimaryActionButton(
            text = "Got it",
            onClick = onBack
        )
    }
}

@Composable
private fun WhyInfoRequiredContent(
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
    ) {
        // Header: back arrow (left) + X close (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalIconButton(
                iconRes = R.drawable.ic_arrow_left,
                contentDescription = "Back",
                onClick = onBack,
                showBorder = false,
                modifier = Modifier.testTag("pay-button-back")
            )

            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Why we collect personal details",
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary)
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = "We collect a few basic details to meet compliance requirements for WalletConnect Pay.\n\nWe only ask once per wallet on this network. You won\u2019t see this again unless your details change.",
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryActionButton(
            text = "Got it",
            onClick = onBack
        )
    }
}

/**
 * Format display amount with the appropriate currency symbol.
 */
private fun formatDisplayAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val safeDecimals = decimals.coerceIn(0, 18)
        val divisor = BigDecimal.TEN.pow(safeDecimals)
        val formattedValue = rawValue.divide(divisor, 2, RoundingMode.HALF_UP)
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val formatted = numberFormat.format(formattedValue)
        val currencySymbol = getCurrencySymbol(symbol)
        "$currencySymbol$formatted"
    } catch (e: Exception) {
        val currencySymbol = getCurrencySymbol(symbol)
        "$currencySymbol$value"
    }
}

/**
 * Get currency symbol for a given currency code.
 */
private fun getCurrencySymbol(currencyCode: String): String {
    return when (currencyCode.uppercase()) {
        "USD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        "JPY" -> "\u00A5"
        "CNY" -> "\u00A5"
        "KRW" -> "\u20A9"
        "INR" -> "\u20B9"
        "RUB" -> "\u20BD"
        "BRL" -> "R$"
        "CHF" -> "CHF "
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> "$currencyCode "
    }
}

/**
 * Format token amount with symbol.
 */
private fun formatTokenAmount(value: String, decimals: Int, symbol: String): String {
    return try {
        val rawValue = BigDecimal(value)
        val safeDecimals = decimals.coerceIn(0, 18)
        val divisor = BigDecimal.TEN.pow(safeDecimals)
        val tokenValue = rawValue.divide(divisor, safeDecimals, RoundingMode.HALF_UP)
        if (tokenValue.signum() == 0) return "0 $symbol"

        // Start at 2 decimals; if the value rounds to 0 there, grow the scale until
        // a non-zero digit appears (capped at the token's natural precision).
        var scale = 2
        while (scale < safeDecimals &&
            tokenValue.setScale(scale, RoundingMode.HALF_UP).signum() == 0
        ) {
            scale++
        }
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = scale
        }
        val formatted = numberFormat.format(tokenValue)
        "$formatted $symbol"
    } catch (e: Exception) {
        "$value $symbol"
    }
}

@Composable
private fun ProcessingContent(
    message: String,
    note: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(WCTheme.colors.bgPrimary)
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WalletConnectLoader(size = 120.dp)

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = message,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("pay-loading-message")
        )

        note?.let {
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))
            Text(
                text = it,
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary),
                textAlign = TextAlign.Center,
                // Secondary line shown only while setting up a token for the first time
                // (e.g. the USDT Permit2 approve step). Lets pay_usdt_polygon observe the
                // approve step by id instead of matching a copy string.
                modifier = Modifier.testTag("pay-loading-setup-note")
            )
        }
    }
}

@Composable
private fun SuccessContent(
    paymentInfo: Wallet.Model.PaymentInfo?,
    resultInfo: Wallet.Model.PaymentResultInfo? = null,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .testTag("pay-result-container")
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        // Green checkmark circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WCTheme.colors.iconSuccess)
                .testTag("pay-result-success-icon"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_check),
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing6))

        // Success summary with payment details, falling back to the default title.
        val successSummary = paymentInfo?.let {
            val displayAmount = formatDisplayAmount(
                value = it.amount.value,
                decimals = it.amount.display?.decimals ?: 2,
                symbol = it.amount.display?.assetSymbol ?: it.amount.unit
            )
            "You've paid $displayAmount to ${it.merchant.name}"
        }
        val content = getResultContent(
            isSuccess = true,
            errorType = null,
            successSummary = successSummary,
        )

        Text(
            text = content.title,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        PrimaryActionButton(
            text = content.actionLabel,
            onClick = onDone,
            modifier = Modifier.testTag("pay-button-result-action-success")
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    errorType: PaymentErrorType,
    onClose: () -> Unit,
    onScanNewQrCode: () -> Unit = {}
) {
    val content = getResultContent(
        isSuccess = false,
        errorType = errorType,
        rawErrorMessage = message,
    )
    val title = content.title
    val subtitle = content.description
    val errorIconTag = content.iconTestId

    val errorActionTag = when (errorType) {
        PaymentErrorType.INSUFFICIENT_FUNDS -> "pay-button-result-action-insufficient_funds"
        PaymentErrorType.EXPIRED -> "pay-button-result-action-expired"
        PaymentErrorType.CANCELLED -> "pay-button-result-action-cancelled"
        PaymentErrorType.NOT_FOUND, PaymentErrorType.GENERIC -> "pay-button-result-action-generic"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WCTheme.colors.bgPrimary)
            .testTag("pay-result-container")
            .padding(WCTheme.spacing.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button (top-right)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing7))

        // Warning icon
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_warning_circle),
            contentDescription = "Warning",
            modifier = Modifier
                .size(40.dp)
                .testTag(errorIconTag),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))

        Text(
            text = title,
            style = WCTheme.typography.h6Regular.copy(color = WCTheme.colors.textPrimary),
            textAlign = TextAlign.Center
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(WCTheme.spacing.spacing2))
            Text(
                text = subtitle,
                style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textTertiary),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(WCTheme.spacing.spacing8))

        PrimaryActionButton(
            text = content.actionLabel,
            onClick = when (content.actionKind) {
                ResultActionKind.SCAN_QR -> onScanNewQrCode
                ResultActionKind.CLOSE -> onClose
            },
            modifier = Modifier.testTag(errorActionTag)
        )
    }
}

// ==================== Shared Modal Components ====================

@Composable
private fun ModalCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .border(
                width = 1.dp,
                color = WCTheme.colors.borderSecondary,
                shape = RoundedCornerShape(WCTheme.borderRadius.radius3)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x_close),
            contentDescription = "Close",
            modifier = Modifier.size(20.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun ModalIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    showBorder: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .then(
                if (showBorder) Modifier.border(
                    width = 1.dp,
                    color = WCTheme.colors.borderSecondary,
                    shape = RoundedCornerShape(WCTheme.borderRadius.radius3)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun InlineIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.bgPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun OptionGasEstimate(displayValue: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "+$displayValue ",
            style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textSecondary)
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_gas_pump),
            contentDescription = null,
            tint = WCTheme.colors.iconDefault,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BorderedIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(WCTheme.borderRadius.radius3))
            .border(
                width = 1.dp,
                color = WCTheme.colors.borderSecondary,
                shape = RoundedCornerShape(WCTheme.borderRadius.radius3)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = WCTheme.colors.textPrimary
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    PrimaryActionButton(
        primaryText = text,
        secondaryText = null,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun PrimaryActionButton(
    primaryText: String,
    secondaryText: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(
                if (enabled) WCTheme.colors.bgAccentPrimary
                else WCTheme.colors.bgAccentPrimary.copy(alpha = 0.6f)
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            )
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = primaryText,
                style = WCTheme.typography.bodyLgRegular.copy(color = Color.White)
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = WCTheme.typography.bodyMdRegular.copy(color = Color.White)
                )
            }
        }
    }
}

// ==================== WebView Information Capture Components ====================

/**
 * JavaScript interface for WebView → Wallet communication.
 * WebView calls: window.AndroidWallet.onDataCollectionComplete(jsonString)
 */
class AndroidWalletJsInterface(
    private val onComplete: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onDataCollectionComplete(jsonData: String) {
        Log.d("AndroidWalletJS", "=== WebView -> Wallet Communication ===")
        try {
            val json = JSONObject(jsonData)
            val type = json.optString("type")
            val success = json.optBoolean("success", false)
            Log.d("AndroidWalletJS", "Parsed message - type: $type, success: $success")

            when {
                type == "IC_COMPLETE" && success -> {
                    Log.d("AndroidWalletJS", "SUCCESS: Information capture completed successfully")
                    onComplete()
                }
                type == "IC_ERROR" -> {
                    val error = json.optString("error", "Unknown error")
                    Log.e("AndroidWalletJS", "ERROR: Information capture failed - $error")
                    onError(error)
                }
                else -> {
                    Log.w("AndroidWalletJS", "WARNING: Unknown message type received: $type")
                    onError("Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidWalletJS", "ERROR: Failed to parse WebView message", e)
            onError("Failed to parse message: ${e.message}")
        }
        Log.d("AndroidWalletJS", "=== End WebView Communication ===")
    }
}

@Composable
private fun WebViewDataCollectionContent(
    url: String,
    paymentInfo: Wallet.Model.PaymentInfo?,
    onComplete: () -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnError by rememberUpdatedState(onError)
    val webViewBackground = WCTheme.colors.bgPrimary
    val webViewBackgroundArgb = webViewBackground.toArgb()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(webViewBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Error display
            loadError?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        style = WCTheme.typography.bodyLgRegular.copy(color = WCTheme.colors.textError),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(WCTheme.spacing.spacing4))
                    PrimaryActionButton(
                        text = "Retry",
                        onClick = { loadError = null }
                    )
                }
                return@Column
            }

            // WebView wrapped in FrameLayout to fix Compose rendering issues
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    // Wrap WebView in FrameLayout to isolate from Compose rendering
                    android.widget.FrameLayout(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )

                        val webView = WebView(ctx).apply {
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            // Enable WebView debugging in debug builds only - inspect via chrome://inspect on desktop
                            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                            // Match the Compose container background so the WebView blends in.
                            setBackgroundColor(webViewBackgroundArgb)

                            // Fix for text input issues in Compose - disable nested scrolling
                            isNestedScrollingEnabled = false
                            overScrollMode = android.view.View.OVER_SCROLL_NEVER

                            // Ensure proper focus handling
                            isFocusable = true
                            isFocusableInTouchMode = true

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }

                            // Add JS interface for IC completion
                            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            addJavascriptInterface(
                                AndroidWalletJsInterface(
                                    onComplete = {
                                        Log.d("PaymentWebView", "IC_COMPLETE received - WebView data collection successful")
                                        mainHandler.post {
                                            Log.d("PaymentWebView", "Proceeding to payment options")
                                            currentOnComplete()
                                        }
                                    },
                                    onError = { error ->
                                        Log.e("PaymentWebView", "IC_ERROR received - WebView error: $error")
                                        mainHandler.post {
                                            Log.e("PaymentWebView", "Showing error to user: $error")
                                            currentOnError(error)
                                        }
                                    }
                                ),
                                "AndroidWallet"
                            )

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    val originalHost = Uri.parse(url).host

                                    // If the URL is from a different host, open in external browser
                                    if (Uri.parse(requestUrl).host != originalHost) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            Log.e("PaymentWebView", "Failed to open external URL: $requestUrl", e)
                                        }
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = error?.description?.toString() ?: "Failed to load"
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.cancel()
                                    loadError = "SSL certificate error"
                                }
                            }

                            Log.d("PaymentWebView", "Loading URL: $url")
                            loadUrl(url)
                        }

                        addView(webView)
                    }
                },
                onRelease = { frameLayout ->
                    (frameLayout.getChildAt(0) as? WebView)?.apply {
                        removeJavascriptInterface("AndroidWallet")
                        stopLoading()
                        webViewClient = WebViewClient()
                        loadUrl("about:blank")
                        (parent as? android.view.ViewGroup)?.removeView(this)
                        destroy()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Centered loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WCTheme.colors.bgPrimary),
                contentAlignment = Alignment.Center
            ) {
                WalletConnectLoader(size = 120.dp)
            }
        }

        // Floating header overlay with back + close buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = WCTheme.spacing.spacing4, vertical = WCTheme.spacing.spacing2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModalIconButton(
                iconRes = R.drawable.ic_arrow_left,
                contentDescription = "Back",
                onClick = onBack,
                showBorder = false,
                modifier = Modifier.testTag("pay-button-back")
            )

            ModalCloseButton(onClick = onClose, modifier = Modifier.testTag("pay-button-close"))
        }
    }
}
