package com.reown.walletkit.client

@RequiresOptIn(
    message = "This API is experimental and may change in a future release.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ChainAbstractionExperimentalApi

@RequiresOptIn(
    message = "This API is experimental and may change in a future release.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class SmartAccountExperimentalApi