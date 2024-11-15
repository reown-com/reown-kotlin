import org.gradle.api.JavaVersion

const val KEY_PUBLISH_VERSION = "PUBLISH_VERSION"
const val KEY_PUBLISH_ARTIFACT_ID = "PUBLISH_ARTIFACT_ID"
const val KEY_SDK_NAME = "SDK_NAME"

//Latest versions
const val BOM_VERSION = "1.0.1"
const val FOUNDATION_VERSION = "1.0.1"
const val CORE_VERSION = "1.0.1"
const val SIGN_VERSION = "1.0.1"
const val NOTIFY_VERSION = "1.0.1"
const val WALLETKIT_VERSION = "1.0.1"
const val APPKIT_VERSION = "1.0.1"
const val MODAL_CORE_VERSION = "1.0.1"
const val YTTRIUM_VERSION = "0.2.8"

//Artifact ids
const val ANDROID_BOM = "android-bom"
const val FOUNDATION = "foundation"
const val ANDROID_CORE = "android-core"
const val SIGN = "sign"
const val NOTIFY = "notify"
const val WALLETKIT = "walletkit"
const val APPKIT = "appkit"
const val MODAL_CORE = "modal-core"

val jvmVersion = JavaVersion.VERSION_11
const val MIN_SDK: Int = 23
const val TARGET_SDK: Int = 34
const val COMPILE_SDK: Int = TARGET_SDK
val SAMPLE_VERSION_CODE = BOM_VERSION.replace(".", "").toInt()
const val SAMPLE_VERSION_NAME = BOM_VERSION