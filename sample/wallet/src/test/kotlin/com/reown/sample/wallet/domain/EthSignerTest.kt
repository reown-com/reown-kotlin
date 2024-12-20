package com.reown.sample.wallet.domain

import org.junit.Test

class EthSignerTest {

    @Test
    fun testSignHash1() {
       val signature =  EthSigner.signHash(
            hashToSign = "0xaceca6583d6e6afa780127d5125edf94b157b2685ff81116339d0ca6752937a4",
            privateKey = "2727fcadfbc14134d6cfd50c2cb70e85870e16b32ca790822ea3d75e6e8b72a3"
        )

        assert(signature == "0xf7d4d35037d8cf5c757f0761fe84e312ba40a9493e868b5d3066b84a8dfc1bbb13dafc39e0dd6ef6a44cf17007580ef3ec8051097bfdab3996070971ae5cb13a1b")
    }
    @Test
    fun testSignHash2() {
        val signature =  EthSigner.signHash(
            hashToSign = "0xc3a9774d06728aaf600f0813355d7624eacb7a71f32359f1670efa0112613e1b",
            privateKey = "d2c26fe51164acb58719f0da5977df0061b7cc755695805d6af482cb37f02128"
        )

        assert(signature == "0xbdd007ae8b9e23e989873e80bc7aad05fb84fc7ad47a347b3002fdf0bae7bb8f7707fc6f7c241a41233c54e0c7c89ca75ad606eb95d6c18b7cbd09eba2361df81c")
    }
}