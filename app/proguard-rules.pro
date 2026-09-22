# kotlinx.serialization: keep serializers for our own @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class org.transdroid.** {
    *** Companion;
}
-keepclasseswithmembers class org.transdroid.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# MaxMind DB reader (peer country lookups), kept whole as Transdroid 2 did. The Map-based lookup
# GeoIpDatabase uses needs no reflection, but the library is tiny and its decoder does reflect
# for annotated model classes - not worth the risk of shrinking it for a few KB.
-keep class com.maxmind.db.** { *; }
-dontwarn com.maxmind.db.**
