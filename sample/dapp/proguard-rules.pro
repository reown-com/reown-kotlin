-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

-keepattributes *Annotation*

-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    native <methods>;
    *;
}

-keep class uniffi.** { *; }

# Preserve all public and protected fields and methods
-keepclassmembers class ** {
    public *;
    protected *;
}

-dontwarn uniffi.**
-dontwarn com.sun.jna.**