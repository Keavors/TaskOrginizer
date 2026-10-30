# JNI entry points are looked up by name from native code.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.keavors.taskorganizer.speech.WhisperNative { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.keavors.taskorganizer.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
