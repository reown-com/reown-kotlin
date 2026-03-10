-keep class com.reown.android.** { *; }
-keep interface com.walletconnect.** { *; }
-keep interface com.reown.** { *; }
-keep class kotlinx.coroutines.** { *; }

-dontwarn kotlinx.coroutines.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**

-dontwarn groovy.lang.GroovyShell
