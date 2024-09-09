package com.reown.appkit.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.reown.appkit.R
import com.reown.appkit.ui.components.internal.AppKitComponent

class AppKitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var shouldOpenNetwork: Boolean
    private var closeModal: () -> Unit = {}

    fun setOnCloseModal(onCloseModal: () -> Unit) {
        closeModal = onCloseModal
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Web3ModalView, 0, 0)
        shouldOpenNetwork = typedArray.getBoolean(R.styleable.Web3ModalView_open_network_select, false)
        typedArray.recycle()

        context.setTheme(R.style.Web3ModalTheme)

        val mode = context.getThemeMode()
        val colors = context.getColorMap()

        LayoutInflater.from(context)
            .inflate(R.layout.view_web3modal, this, true)
            .findViewById<ComposeView>(R.id.root)
            .apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppKitTheme(
                        mode = mode,
                        lightColors = colors.getLightModeColors(),
                        darkColors = colors.getDarkModeColors()
                    ) {
                        AppKitComponent(
                            shouldOpenChooseNetwork = shouldOpenNetwork,
                            closeModal = closeModal
                        )
                    }
                }
            }
    }

}