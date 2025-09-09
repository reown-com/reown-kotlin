package com.reown.sign.client

interface SignInterface {
    interface WalletDelegate {
        fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal, verifyContext: Sign.Model.VerifyContext)
        val onSessionAuthenticate: ((Sign.Model.SessionAuthenticate, Sign.Model.VerifyContext) -> Unit)? get() = null
        fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest, verifyContext: Sign.Model.VerifyContext)
        fun onSessionDelete(deletedSession: Sign.Model.DeletedSession)
        fun onSessionExtend(session: Sign.Model.Session)

        //Responses
        fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse)
        fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse)

        //Utils
        fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
            //override me
        }

        fun onRequestExpired(request: Sign.Model.ExpiredRequest) {
            //override me
        }

        fun onConnectionStateChange(state: Sign.Model.ConnectionState)
        fun onError(error: Sign.Model.Error)
    }

    interface DappDelegate {
        fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession)
        fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession)
        fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession)

        @Deprecated(

            message = "onSessionEvent is deprecated. Use onEvent instead. Using both will result in duplicate events.",
            replaceWith = ReplaceWith(expression = "onEvent(event)")
        )
        fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent)
        fun onSessionEvent(sessionEvent: Sign.Model.Event) {}
        fun onSessionExtend(session: Sign.Model.Session)
        fun onSessionDelete(deletedSession: Sign.Model.DeletedSession)

        //Responses
        fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse)
        fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Sign.Model.SessionAuthenticateResponse) {}

        // Utils
        fun onProposalExpired(proposal: Sign.Model.ExpiredProposal)
        fun onRequestExpired(request: Sign.Model.ExpiredRequest)
        fun onConnectionStateChange(state: Sign.Model.ConnectionState)
        fun onError(error: Sign.Model.Error)
    }

    fun initialize(init: Sign.Params.Init, onSuccess: () -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun setWalletDelegate(delegate: WalletDelegate)
    fun setDappDelegate(delegate: DappDelegate)

    @Deprecated(
        "This method is deprecated. The requiredNamespaces parameter is no longer supported as all namespaces are now treated as optional to improve connection compatibility. Use connect(connectParams: Sign.Params.ConnectParams, onSuccess: (String) -> Unit, onError: (Sign.Model.Error) -> Unit) instead.",
        replaceWith = ReplaceWith("connect(connect, onSuccess, onError)")
    )
    fun connect(
        connect: Sign.Params.Connect,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit,
    )

    fun connect(
        connectParams: Sign.Params.ConnectParams,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit,
    )

    fun pair(uri: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)

    fun authenticate(
        authenticate: Sign.Params.Authenticate,
        walletAppLink: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Sign.Model.Error) -> Unit
    )

    fun dispatchEnvelope(urlWithEnvelope: String, onError: (Sign.Model.Error) -> Unit)
    fun approveSession(approve: Sign.Params.Approve, onSuccess: (Sign.Params.Approve) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun rejectSession(reject: Sign.Params.Reject, onSuccess: (Sign.Params.Reject) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun approveAuthenticate(
        approve: Sign.Params.ApproveAuthenticate,
        onSuccess: (Sign.Params.ApproveAuthenticate) -> Unit,
        onError: (Sign.Model.Error) -> Unit
    )

    fun rejectAuthenticate(
        reject: Sign.Params.RejectAuthenticate,
        onSuccess: (Sign.Params.RejectAuthenticate) -> Unit,
        onError: (Sign.Model.Error) -> Unit
    )

    fun formatAuthMessage(formatMessage: Sign.Params.FormatMessage): String

    fun request(request: Sign.Params.Request, onSuccess: (Sign.Model.SentRequest) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun respond(response: Sign.Params.Response, onSuccess: (Sign.Params.Response) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun update(update: Sign.Params.Update, onSuccess: (Sign.Params.Update) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun extend(extend: Sign.Params.Extend, onSuccess: (Sign.Params.Extend) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun emit(emit: Sign.Params.Emit, onSuccess: (Sign.Params.Emit) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun ping(ping: Sign.Params.Ping, sessionPing: Sign.Listeners.SessionPing? = null)
    fun disconnect(disconnect: Sign.Params.Disconnect, onSuccess: (Sign.Params.Disconnect) -> Unit = {}, onError: (Sign.Model.Error) -> Unit)
    fun decryptMessage(params: Sign.Params.DecryptMessage, onSuccess: (Sign.Model.Message) -> Unit, onError: (Sign.Model.Error) -> Unit)

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getListOfActiveSessions(): List<Sign.Model.Session>

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getActiveSessionByTopic(topic: String): Sign.Model.Session?

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getPendingSessionRequests(topic: String): List<Sign.Model.SessionRequest>

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getSessionProposals(): List<Sign.Model.SessionProposal>

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getVerifyContext(id: Long): Sign.Model.VerifyContext?

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getPendingAuthenticateRequests(): List<Sign.Model.SessionAuthenticate>

    /**
     * Caution: This function is blocking and runs on the current thread.
     * It is advised that this function be called from background operation
     */
    fun getListOfVerifyContexts(): List<Sign.Model.VerifyContext>
}