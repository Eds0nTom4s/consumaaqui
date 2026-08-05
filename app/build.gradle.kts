plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

fun quotedBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val debugDiscoveryBaseUrl = providers.gradleProperty("DISCOVERY_DEBUG_BASE_URL").orElse("")
val stagingDiscoveryBaseUrl = providers.gradleProperty("DISCOVERY_STAGING_BASE_URL").orElse("")
val releaseDiscoveryBaseUrl = providers.gradleProperty("DISCOVERY_RELEASE_BASE_URL").orElse("")

android {
    namespace = "ao.consuma.aqui"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ao.consuma.aqui"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "ao.consuma.aqui.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "ENVIRONMENT", "\"DEBUG\"")
            buildConfigField("String", "DISCOVERY_BASE_URL", quotedBuildConfig(debugDiscoveryBaseUrl.get()))
            buildConfigField("String", "DISCOVERY_SOURCE", "\"MOCK\"")
            buildConfigField("boolean", "DISCOVERY_SOURCE_SELECTABLE", "true")
        }
        create("staging") {
            initWith(getByName("debug"))
            buildConfigField("String", "ENVIRONMENT", "\"STAGING\"")
            buildConfigField("String", "DISCOVERY_BASE_URL", quotedBuildConfig(stagingDiscoveryBaseUrl.get()))
            buildConfigField("String", "DISCOVERY_SOURCE", "\"REMOTE\"")
            buildConfigField("boolean", "DISCOVERY_SOURCE_SELECTABLE", "false")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ENVIRONMENT", "\"RELEASE\"")
            buildConfigField("String", "DISCOVERY_BASE_URL", quotedBuildConfig(releaseDiscoveryBaseUrl.get()))
            buildConfigField("String", "DISCOVERY_SOURCE", "\"REMOTE\"")
            buildConfigField("boolean", "DISCOVERY_SOURCE_SELECTABLE", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
