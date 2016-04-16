For NativeScipt 1.3+ we require .aar libraries, so the former zxing barcodescanner LibraryProject we used (copied from the Cordova barcodescanner repo) didn't work anymore.

Steps to build a new .aar:
 * Clone this repo
 * Open it in Android Studio
 * Update any source files as needed (current version is: https://github.com/zxing/zxing/releases/tag/BS-4.7.5):
 ** Copy all files from `core`
 ** From the `android` folder grab the src/.../android folder and paste that to the appropriate package
 ** Same for `android-core`
 ** Note that R.java is generated when building and is supposed to be in build/generated/source/r/debug/barcodescanner/xservices/nl/barcodescanner/R
 ** Re-add the 'flip camera' feature and the 'portrait scan' feature I added before!
 * Open the Gradle toolwindow
 * Run barcodescanner > Tasks > build > build
 * The (release) .aar will be generated in barcodescanner > build > outputs
 * Commit and push any changes made