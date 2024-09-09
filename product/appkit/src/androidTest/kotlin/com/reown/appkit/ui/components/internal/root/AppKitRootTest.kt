package com.reown.appkit.ui.components.internal.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.reown.appkit.ui.components.internal.commons.ContentDescription
import com.reown.appkit.ui.components.internal.commons.TestTags
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.rememberTestNavController
import com.reown.appkit.ui.theme.ProvideAppKitThemeComposition
import org.junit.Rule
import org.junit.Test

class AppKitRootTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun web3ModalRoot_shouldShowTitleFromEnumRoute() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }
        }

        composeTestRule.onNodeWithTag(TestTags.TITLE).assertExists()
        composeTestRule.onNodeWithText(Route.CONNECT_YOUR_WALLET.title!!).assertExists()
    }

    @Test
    fun web3ModalRoot_shouldShowTitleFromNavArg() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }

            LaunchedEffect(Unit) {
                controller.navigate(Route.REDIRECT.path + "&" + "title")
            }
        }

        composeTestRule.onNodeWithTag("Title").assertExists()
        composeTestRule.onNodeWithText("title").assertExists()
    }

    @Test
    fun web3ModalRoot_shouldShowWithoutTitle() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }

            LaunchedEffect(Unit) {
                controller.navigate("A")
            }
        }

        composeTestRule.onNodeWithTag("Title").assertDoesNotExist()
    }

    @Test
    fun web3ModalRoot_QuestionMarkIcon() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(ContentDescription.QUESTION_MARK.description)
    }

    @Test
    fun web3ModalRoot_BackIcon() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(ContentDescription.BACK_ARROW.description)
    }

    @Test
    fun web3ModalRoot_closeIsCalled() {
        composeTestRule.setContent {
            val controller = rememberTestNavController()
            ProvideAppKitThemeComposition {
                AppKitRoot(navController = controller, closeModal = { }) {
                    Box {}
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(ContentDescription.BACK_ARROW.description)
    }
}