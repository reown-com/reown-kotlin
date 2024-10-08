package com.reown.android.relay

import com.reown.android.Core
import com.reown.foundation.network.RelayInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RelayConnectionInterface : RelayInterface {
    val wssConnectionState: StateFlow<WSSConnectionState>
    val isNetworkAvailable: StateFlow<Boolean?>
    val onResubscribe: Flow<Any?>

    @Deprecated("This has become deprecate in favor of the onError returning Core.Model.Error", ReplaceWith("this.connect(onErrorModel)"))
    fun connect(onErrorModel: (Core.Model.Error) -> Unit = {}, onError: (String) -> Unit)
    fun connect(onError: (Core.Model.Error) -> Unit)
    @Deprecated("This has become deprecate in favor of the onError returning Core.Model.Error", ReplaceWith("this.disconnect(onErrorModel)"))
    fun disconnect(onErrorModel: (Core.Model.Error) -> Unit = {}, onError: (String) -> Unit)
    fun disconnect(onError: (Core.Model.Error) -> Unit)

    fun restart(onError: (Core.Model.Error) -> Unit)
}