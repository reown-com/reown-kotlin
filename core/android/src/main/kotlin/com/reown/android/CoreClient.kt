package com.reown.android

object CoreClient : CoreInterface by CoreProtocol.instance {

    interface CoreDelegate : CoreInterface.Delegate

}