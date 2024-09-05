package com.reown.notify.client

object NotifyClient: NotifyInterface by NotifyProtocol.instance {
    interface Delegate: NotifyInterface.Delegate
}