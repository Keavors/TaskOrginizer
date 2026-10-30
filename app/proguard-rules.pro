# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.keavors.taskorganizer.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
