# Native code calls back into Kotlin by name: Demucs.Progress.onProgress(F)Z, on whatever class
# implements it (lambdas included). R8 must not rename or strip it.
-keep class com.pocketstudio.separation.Demucs { *; }
-keep interface com.pocketstudio.separation.Demucs$Progress { *; }
-keepclassmembers class * implements com.pocketstudio.separation.Demucs$Progress {
    boolean onProgress(float);
}
