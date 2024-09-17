package com.reown.appkit.ui.components.internal

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.reown.appkit.ui.components.internal.commons.BackArrowIcon
import com.reown.appkit.ui.components.internal.commons.ContentDescription
import com.reown.appkit.ui.components.internal.commons.TestTags
import com.reown.appkit.ui.theme.ProvideAppKitThemeComposition
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class AppKitTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun web3ModalTopBar_titleShouldBeShown() {
        composeTestRule.setContent {
            ProvideAppKitThemeComposition {
                AppKitTopBar(title = "Title", startIcon = { BackArrowIcon { } }, onCloseIconClick = {})
            }
        }

        composeTestRule.onNodeWithTag(TestTags.TITLE).assertExists()
        composeTestRule.onNodeWithText("Title").assertExists()
    }

    @Test
    fun web3ModalTopBar_onCloseCallbackIsTriggered() {
        var isClicked = false

        composeTestRule.setContent {
            ProvideAppKitThemeComposition {
                AppKitTopBar(title = "Title", startIcon = { BackArrowIcon { } }, onCloseIconClick = { isClicked = true})
            }

        }
        composeTestRule.onNodeWithContentDescription(ContentDescription.CLOSE.description).performClick()

        assertEquals(isClicked, true)
    }
}