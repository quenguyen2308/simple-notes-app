# build-debug

Build a debug APK for simple-notes-app.

Run the following command and report the result:

```bash
cd "D:/ClaudeCode/claude-code/simple-notes-app" && JAVA_HOME="C:/Users/test user/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot" ANDROID_HOME="C:/Android/Sdk" PATH="$JAVA_HOME/bin:$PATH" ./gradlew assembleDebug 2>&1 | tail -20
```

If the build fails with `classes.dex: The process cannot access the file`, run:
```bash
rm -rf "D:/ClaudeCode/claude-code/simple-notes-app/app/build/intermediates/dex/debug"
```
then retry the build command above.

When successful, report the APK path:
`app/build/outputs/apk/debug/app-debug.apk`
