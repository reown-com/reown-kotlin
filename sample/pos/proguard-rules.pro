-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# msgpack-core uses sun.nio.ch.DirectBuffer for optimized buffer access on JVM,
# which is not available on Android
-dontwarn sun.nio.ch.**