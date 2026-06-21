# Ktor 通过插件和引擎在运行期装配 HTTP 管线；Release 压缩时保留其公开运行时类型。
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization 的 serializer 查找依赖注解、内部类和 Companion 元数据。
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.runninghub.**$$serializer { *; }
-keepclassmembers class com.runninghub.** { *** Companion; }
-keepclasseswithmembers class com.runninghub.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class kotlin.Metadata { *; }

# Koin 模块使用泛型和 lambda 装配依赖；保留框架运行时，业务类型仍允许 R8 按引用收缩。
-keep class org.koin.** { *; }

# Coil / Compose 相关库自带规则会覆盖大部分场景；这里仅保留日志和平台服务加载时常见的动态入口警告。
-dontwarn org.slf4j.**
