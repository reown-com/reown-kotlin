package com.walletconnect.web3.modal.ui.routes.connect.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walletconnect.web3.modal.ui.components.internal.commons.HorizontalSpacer
import com.walletconnect.web3.modal.ui.components.internal.commons.VerticalSpacer
import com.walletconnect.web3.modal.ui.components.internal.email.code.CodeInput
import com.walletconnect.web3.modal.ui.components.internal.email.code.rememberCodeInputState
import com.walletconnect.web3.modal.ui.previews.UiModePreview
import com.walletconnect.web3.modal.ui.theme.Web3ModalTheme

@Composable
internal fun ConfirmEmailRoute() {
    ConfirmEmail()
}

@Composable
private fun ConfirmEmail() {
    //TODO: Add loading state
    val codeInputState = rememberCodeInputState { code ->
        //TODO: Handle sending code
        println("kobe: $code")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VerticalSpacer(16.dp)
        Text(
            text = "Enter the code we sent to",
            style = Web3ModalTheme.typo.paragraph400,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "jakub@walletconnect.com", //TODO: Replace with email
            style = Web3ModalTheme.typo.paragraph400,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        VerticalSpacer(12.dp)
        Text(
            text = "The code expires in 20 minutes",
            style = Web3ModalTheme.typo.small400.copy(Web3ModalTheme.colors.foreground.color200),
            textAlign = TextAlign.Center
        )
        VerticalSpacer(16.dp)
        CodeInput(codeInputState)
        VerticalSpacer(12.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't receive it?",
                style = Web3ModalTheme.typo.paragraph400
            )
            HorizontalSpacer(width = 4.dp)
            Text(
                text = "Resend code",
                style = Web3ModalTheme.typo.small600.copy(color = Web3ModalTheme.colors.accent100)
            ) //TODO: Implement resend link
        }
        VerticalSpacer(20.dp)
    }
}

@Composable
@UiModePreview
private fun ConfirmEmailPreview() {
    ConfirmEmail()
}