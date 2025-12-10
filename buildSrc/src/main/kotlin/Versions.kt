import org.gradle.api.JavaVersion

const val KEY_PUBLISH_VERSION = "PUBLISH_VERSION"
const val KEY_PUBLISH_ARTIFACT_ID = "PUBLISH_ARTIFACT_ID"
const val KEY_PUBLISH_GROUP = "PUBLISH_GROUP"
const val KEY_SDK_NAME = "SDK_NAME"

const val DEFAULT_PUBLISH_GROUP = "com.reown"

//Latest versions
const val BOM_VERSION = "1.4.11"
const val FOUNDATION_VERSION = "1.4.11"
const val CORE_VERSION = "1.4.11"
const val SIGN_VERSION = "1.4.11"
const val NOTIFY_VERSION = "1.4.11"
const val WALLETKIT_VERSION = "1.4.11"
const val APPKIT_VERSION = "1.4.11"
const val MODAL_CORE_VERSION = "1.4.11"
const val POS_VERSION = "1.0.0"

//Artifact ids
const val ANDROID_BOM = "android-bom"
const val FOUNDATION = "foundation"
const val ANDROID_CORE = "android-core"
const val SIGN = "sign"
const val NOTIFY = "notify"
const val WALLETKIT = "walletkit"
const val APPKIT = "appkit"
const val MODAL_CORE = "modal-core"
const val POS = "pos"

val jvmVersion = JavaVersion.VERSION_11
const val MIN_SDK: Int = 23
const val TARGET_SDK: Int = 35
const val COMPILE_SDK: Int = TARGET_SDK
val SAMPLE_VERSION_CODE = BOM_VERSION.replace(".", "").toInt()
const val SAMPLE_VERSION_NAME = BOM_VERSION