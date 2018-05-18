### Steps to build a new .aar
 * Clone this repo
 * Open it in Android Studio
 * Update any source files as needed (current version is: https://github.com/zxing/zxing/releases/tag/BS-4.7.6):
   - Copy all files from `core`
   - From the `android` folder grab the src/.../android folder and paste that to the appropriate package
   - Same for `android-core`
   - Note that R.java is generated when building and is supposed to be in build/generated/source/r/debug/barcodescanner/xservices/nl/barcodescanner/R
   - Re-add the 'flip camera' feature and the 'portrait scan' feature I added before!
   - Make sure no `app_name` tag is active in the `res/values*/string.xml` files
 * (Finder/Explorer): Clean barcodescanner > build > outputs
 * Open the Gradle toolwindow
 * Run barcodescanner > Tasks > other > build
 * The (release) .aar will be generated in barcodescanner > build > outputs
 * Commit and push any changes made!

### The generated .aar is used in:
* [NativeScript BarcodeScanner Plugin](https://github.com/EddyVerbruggen/nativescript-barcodescanner/)
* [Cordova BarcodeScanner Plugin](https://github.com/Telerik-Verified-Plugins/BarcodeScanner/)
