For NativeScipt 1.3+ we require .aar libraries, so the former zxing barcodescanner LibraryProject we used (copied from the Cordova barcodescanner repo) didn't work anymore.

Steps to build a new .aar:
 * Clone this repo
 * Open it in Android Studio
 * Update any source files as needed
 * Open the Gradle toolwindow
 * Run barcodescanner > Tasks > build > build
 * The (release) .aar will be generated in barcodescanner > build > outputs
 * Commit and push any changes made