# BenStudio Test only (-PbenTest): the robot's classes keep their names, so that its crashes read plainly and
# scripts/bentest/check-apk.sh (dexdump) finds it; the app's own code is shrunk and optimised as in the real release.
-keep class com.pocketstudio.bentest.** { *; }
-keep class com.pocketstudio.app.journey.** { *; }
