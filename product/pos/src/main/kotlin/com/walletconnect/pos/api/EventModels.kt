package com.walletconnect.pos.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.walletconnect.pos.Pos

@JsonClass(generateAdapter = true)
internal data class IngestEventRequest(
    @param:Json(name = "event_id") val eventId: String,
    @param:Json(name = "payment_id") val paymentId: String,
    @param:Json(name = "actor") val actor: String = "POS",
    @param:Json(name = "event_type") val eventType: String,
    @param:Json(name = "ts") val timestamp: String,
    @param:Json(name = "version") val version: Int = 1,
    @param:Json(name = "source_service") val sourceService: String = "POS-ANDROID",
    @param:Json(name = "sdk_name") val sdkName: String,
    @param:Json(name = "sdk_version") val sdkVersion: String,
    @param:Json(name = "sdk_platform") val sdkPlatform: String = "android",
    @param:Json(name = "merchant_id") val merchantId: String,
    @param:Json(name = "payload") val payload: EventPayload? = null
)

@JsonClass(generateAdapter = true)
internal data class EventPayload(
    // Payment context fields (for non-error events)
    @param:Json(name = "payment_url") val paymentUrl: String? = null,
    @param:Json(name = "amount") val amount: PaymentAmountPayload? = null,
    @param:Json(name = "reference_id") val referenceId: String? = null,
    @param:Json(name = "display_amount") val displayAmount: Double? = null,
    @param:Json(name = "currency") val currency: String? = null,
    // Error fields (for payment_failed events)
    @param:Json(name = "error_category") val errorCategory: String? = null,
    @param:Json(name = "error_code") val errorCode: String? = null,
    @param:Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
internal data class PaymentAmountPayload(
    @param:Json(name = "unit") val unit: String,
    @param:Json(name = "value_minor") val valueMinor: Long
)

internal data class PaymentContext(
    val paymentUrl: String,
    val unit: String,
    val valueMinor: Long,
    val referenceId: String?
) {
    fun toEventPayload(): EventPayload {
        val currency =  if (unit.contains("/")) {
            unit.substringAfter("/")
        } else {
            unit
        }

        val displayAmount = valueMinor / 100.0
        return EventPayload(
            paymentUrl = paymentUrl,
            amount = PaymentAmountPayload(unit = unit, valueMinor = valueMinor),
            referenceId = referenceId,
            displayAmount = displayAmount,
            currency = currency
        )
    }

    fun toErrorEventPayload(error: Pos.PaymentEvent.PaymentError): EventPayload {
        val basePayload = toEventPayload()
        val errorMapping = error.toErrorFields()
        return basePayload.copy(
            errorCategory = errorMapping.category,
            errorCode = errorMapping.code,
            errorMessage = errorMapping.message
        )
    }
}

internal data class ErrorFields(
    val category: String,
    val code: String,
    val message: String
)

internal object EventType {
    const val WC_PAY_SELECTED = "wc_pay_selected"
    const val PAYMENT_CREATED = "payment_created"
    const val PAYMENT_REQUESTED = "payment_requested"
    const val PAYMENT_PROCESSING = "payment_processing"
    const val PAYMENT_COMPLETED = "payment_completed"
    const val PAYMENT_FAILED = "payment_failed"
}

internal object ErrorCategory {
    const val API = "api"
    const val PAYMENT = "payment"
    const val VALIDATION = "validation"
    const val UNKNOWN = "unknown"
}

internal object ErrorCode {
    const val CREATE_PAYMENT_FAILED = "CREATE_PAYMENT_FAILED"
    const val PAYMENT_FAILED = "PAYMENT_FAILED"
    const val PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND"
    const val PAYMENT_EXPIRED = "PAYMENT_EXPIRED"
    const val INVALID_PAYMENT_REQUEST = "INVALID_PAYMENT_REQUEST"
    const val UNDEFINED_ERROR = "UNDEFINED_ERROR"
}

internal fun Pos.PaymentEvent.PaymentError.toErrorFields(): ErrorFields {
    return when (this) {
        is Pos.PaymentEvent.PaymentError.CreatePaymentFailed -> ErrorFields(
            category = ErrorCategory.API,
            code = ErrorCode.CREATE_PAYMENT_FAILED,
            message = message
        )
        is Pos.PaymentEvent.PaymentError.PaymentFailed -> ErrorFields(
            category = ErrorCategory.PAYMENT,
            code = ErrorCode.PAYMENT_FAILED,
            message = message
        )
        is Pos.PaymentEvent.PaymentError.PaymentNotFound -> ErrorFields(
            category = ErrorCategory.API,
            code = ErrorCode.PAYMENT_NOT_FOUND,
            message = message
        )
        is Pos.PaymentEvent.PaymentError.PaymentExpired -> ErrorFields(
            category = ErrorCategory.PAYMENT,
            code = ErrorCode.PAYMENT_EXPIRED,
            message = message
        )
        is Pos.PaymentEvent.PaymentError.InvalidPaymentRequest -> ErrorFields(
            category = ErrorCategory.VALIDATION,
            code = ErrorCode.INVALID_PAYMENT_REQUEST,
            message = message
        )
        is Pos.PaymentEvent.PaymentError.Undefined -> ErrorFields(
            category = ErrorCategory.UNKNOWN,
            code = ErrorCode.UNDEFINED_ERROR,
            message = message
        )
    }
}

internal fun Pos.PaymentEvent.PaymentError.toErrorPayload(): EventPayload {
    val errorFields = toErrorFields()
    return EventPayload(
        errorCategory = errorFields.category,
        errorCode = errorFields.code,
        errorMessage = errorFields.message
    )
}
