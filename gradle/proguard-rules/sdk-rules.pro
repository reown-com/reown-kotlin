-allowaccessmodification
-keeppackagenames doNotKeepAThing

-dontobfuscate
-dontshrink
-dontoptimize
-dontusemixedcaseclassnames
-dontwarn java.lang.invoke.StringConcatFactory

# Keep all JNA-related classes and methods
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    *;
}

# Disable warnings for JNA
-dontwarn com.sun.jna.**