package com.reown.android.pairing.model.mapper

import com.reown.android.Core
import com.reown.android.internal.common.model.AppMetaData
import com.reown.android.internal.common.model.Expiry
import com.reown.android.internal.common.model.Pairing
import com.reown.android.internal.common.model.Redirect
import com.reown.android.pairing.engine.model.EngineDO
import com.reown.foundation.common.model.Topic
import com.reown.utils.Empty

@JvmSynthetic
@Deprecated("This mapper has been deprecated. It will be removed soon.")
internal fun EngineDO.PairingDelete.toCore(): Core.Model.DeletedPairing =
    Core.Model.DeletedPairing(topic, reason)

@JvmSynthetic
@Deprecated("This mapper has been deprecated. It will be removed soon.")
internal fun Pairing.toCore(): Core.Model.Pairing =
    Core.Model.Pairing(
        topic.value,
        expiry.seconds,
        peerAppMetaData?.toCore(),
        relayProtocol,
        relayData,
        uri,
        isActive = true,
        methods ?: String.Empty
    )

@JvmSynthetic
fun Core.Model.Pairing.toPairing(): Pairing =
    Pairing(
        Topic(topic),
        Expiry(expiry),
        peerAppMetaData?.toAppMetaData(),
        relayProtocol,
        relayData,
        uri,
        methods = registeredMethods
    )

@JvmSynthetic
internal fun Core.Model.AppMetaData.toAppMetaData() = AppMetaData(name = name, description = description, url = url, icons = icons, redirect = Redirect(redirect))

@JvmSynthetic
internal fun AppMetaData?.toCore() = Core.Model.AppMetaData(this?.name ?: String.Empty, this?.description ?: String.Empty, this?.url ?: String.Empty, this?.icons ?: emptyList(), this?.redirect?.native)