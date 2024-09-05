package com.reown.appkit.domain.model

import com.reown.appkit.client.Modal

object InvalidSessionException: Throwable("Session topic is missing")

internal fun Throwable.toModalError() = Modal.Model.Error(this)