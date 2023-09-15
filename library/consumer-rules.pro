#noinspection ShrinkerUnresolvedReference

# 这里的规则将会被包含进 AAR 包中
# 1. 可以存放该模块的混淆规则，防止使用方无法找到对应的方法
# 2. 可以存放影响该模块功能的第三方库的混淆规则，例如反射、序列化等

# kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class com.mason.saas.net.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.mason.saas.net.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.mason.saas.net.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}

# an-net
-keep class * implements okhttp3.Interceptor { *; }
-keep class com.mason.net.BaseService { *; }
-keep class com.mason.net.ApiException { *; }
-keep class * extends com.mason.net.ApiException { *; }
-keep class com.mason.net.BaseApiCode { *; }
-keep interface com.mason.net.INetLog { *; }

-keepclasseswithmembers class com.mason.net.Net {
    com.mason.net.Net INSTANCE;
    void setLog(com.mason.net.INetLog);
    com.mason.net.INetLog getLog();
}
