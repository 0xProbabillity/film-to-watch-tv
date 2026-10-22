# Retrofit, OkHttp and kotlinx.serialization ship their own R8 rules.
# Keep our @Serializable models' generated serializers (looked up by Retrofit's converter).
-keepclassmembers @kotlinx.serialization.Serializable class com.filmtowatch.tv.data.** {
    static **$Companion Companion;
    *** Companion;
}
-keep,includedescriptorclasses class com.filmtowatch.tv.data.**$$serializer { *; }

# Retrofit service interfaces are only reached through reflection
-keep,allowobfuscation interface com.filmtowatch.tv.data.*Api
