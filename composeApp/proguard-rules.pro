# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.runninghub.shared.data.model.**$$serializer { *; }
-keepclassmembers class com.runninghub.shared.data.model.** { *** Companion; }
-keepclasseswithmembers class com.runninghub.shared.data.model.** { kotlinx.serialization.KSerializer serializer(...); }

# SQLDelight
-keep class com.runninghub.shared.db.** { *; }

# Koin
-keep class org.koin.** { *; }
