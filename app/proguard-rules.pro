-dontobfuscate

-keep class org.transdroid.core.gui.log.ErrorLogEntry { *; }
-dontwarn javax.persistence.**

# GeoIP (maxmind-db), used for peer country lookups.
-keep class com.maxmind.db.** { *; }
-dontwarn com.maxmind.db.**
