package com.reown.sample.wallet

import com.reown.kotlin.ffi.yttrium.add
import org.junit.Test

class YttriumTest {

    @Test
    fun addTest() {
        val result = add(1u, 2u)
        println("Result: $result")
        assert(result == 3u)
    }
}