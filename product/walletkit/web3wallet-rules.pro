-keep class com.reown.walletkit.client.Wallet$Model$Cacao$Signature { *; }
-keep class com.reown.walletkit.client.Wallet$Model$Cacao { *; }
-keep class com.reown.walletkit.client.Wallet$Model { *; }
-keep class com.reown.walletkit.client.Wallet { *; }

# Preserve all annotations (JNA and other libraries)
-keepattributes *Annotation*

# Keep all JNA-related classes and methods
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    native <methods>;
    *;
}

# Preserve the uniffi generated classes
-keep class uniffi.** { *; }

# Preserve all public and protected fields and methods
-keepclassmembers class ** {
    public *;
    protected *;
}

# Disable warnings for uniffi and JNA
-dontwarn uniffi.**
-dontwarn com.sun.jna.**


