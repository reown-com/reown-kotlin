package com.reown.foundation.util

interface Logger {

    fun log(logMsg: String?)

    fun log(throwable: Throwable?)

    fun error(errorMsg: String?)

    fun error(throwable: Throwable?)
}