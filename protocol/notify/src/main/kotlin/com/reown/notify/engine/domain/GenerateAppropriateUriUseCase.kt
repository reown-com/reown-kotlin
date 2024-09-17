@file:JvmSynthetic

package com.reown.notify.engine.domain

import android.net.Uri

internal class GenerateAppropriateUriUseCase {

    operator fun invoke(uri: Uri, path: String): Uri =
        if (uri.path?.contains(path) == false) {
            uri.buildUpon().encodedPath(path).build()
        } else {
            uri
        }
}