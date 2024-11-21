package com.reown.sample.wallet.ui.routes.dialog_routes.session_request.request

import com.reown.sample.wallet.ui.common.peer.PeerContextUI
import com.reown.sample.wallet.ui.common.peer.PeerUI

sealed class SessionRequestUI {
    object Initial : SessionRequestUI()

    data class Content(
        val peerUI: PeerUI,
        val topic: String,
        val requestId: Long,
        val param: String,
        val chain: String?,
        val method: String,
        val peerContextUI: PeerContextUI
    ) : SessionRequestUI()
}