# Keep readable stack traces in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose, AndroidX and DataStore ship their own consumer rules; org.json is part of the platform.
