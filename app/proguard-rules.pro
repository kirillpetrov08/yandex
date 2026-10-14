# Yandex Mobile Ads SDK: адаптеры медиации и рефлексия внутри SDK.
-keep class com.yandex.mobile.ads.** { *; }
-keep class com.yandex.metrica.** { *; }
-dontwarn com.yandex.mobile.ads.**
-dontwarn com.yandex.metrica.**

# Сохраняем имена для объектов, которые SDK создаёт по рефлексии
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Kotlin-корутины
-dontwarn kotlinx.coroutines.**
