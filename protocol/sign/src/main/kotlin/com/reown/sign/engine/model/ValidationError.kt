package com.reown.sign.engine.model

import com.reown.sign.common.exceptions.*

internal sealed class ValidationError(val message: String) {

    //Namespaces validation
    object EmptyNamespaces : ValidationError(EMPTY_NAMESPACES_MESSAGE)
    object UnsupportedNamespaceKey : ValidationError(NAMESPACE_KEYS_INVALID_FORMAT)
    class UnsupportedChains(_message: String) : ValidationError(_message)

    //Rejected errors
    object UserRejected : ValidationError(NAMESPACE_KEYS_MISSING_MESSAGE)
    class UserRejectedChains(_message: String) : ValidationError(_message)
    object UserRejectedMethods : ValidationError(NAMESPACE_METHODS_MISSING_MESSAGE)
    object UserRejectedEvents : ValidationError(NAMESPACE_EVENTS_MISSING_MESSAGE)

    //Authorization errors
    object UnauthorizedMethod : ValidationError(UNAUTHORIZED_METHOD_MESSAGE)
    object UnauthorizedEvent : ValidationError(UNAUTHORIZED_EVENT_MESSAGE)

    //Validation errors
    object InvalidSessionRequest : ValidationError(INVALID_REQUEST_MESSAGE)
    object InvalidEvent : ValidationError(INVALID_EVENT_MESSAGE)
    object InvalidExtendRequest : ValidationError(INVALID_EXTEND_TIME)

    //Properties errors
    object InvalidSessionProperties : ValidationError(INVALID_SESSION_PROPERTIES)
}
