package com.reown.appkit.domain.model

import com.reown.appkit.client.Modal

object InvalidSessionException: Throwable("Session topic is missing")
object NoChainSelectedException: Throwable("No chain specified. Either provide chainId parameter or select a chain first.")

internal fun Throwable.toModalError() = Modal.Model.Error(this)