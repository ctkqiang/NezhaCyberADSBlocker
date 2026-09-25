import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

ksp {
    // 每一版表结构都导出为 JSON 并纳入版本控制：将来改动表结构时，迁移写得对不对可以直接拿
    // 历史 schema 对照复核，而不必凭记忆重建上一版长什么样。
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "xin.ctkqiang.nezha_cyber.ads_block"
    compileSdk = 36

    defaultConfig {
        applicationId = "xin.ctkqiang.nezha_cyber.ads_block"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * Release 签名配置。
     *
     * 密钥库路径与口令只从 `local.properties`（本机）或环境变量（CI）读取，绝不硬编码进构建脚本：
     * 工程规则第 0.3 节（不收集用户数据）同样适用于开发者凭据——把口令写进仓库等于公开。
     *
     * 未配置时 release 构建回退到 debug 签名，便于本地验证；但发布前必须配置正式密钥库，
     * 否则 Play 上传与部分系统安装器会拒绝。
     */
    signingConfigs {
        create("release") {
            val properties =
                Properties()
                    .apply {
                        val localProperties = rootProject.file("local.properties")
                        if (localProperties.exists()) {
                            localProperties.inputStream().use { load(it) }
                        }
                    }
            val keystorePath =
                properties
                    .getProperty("key.store")
                    ?: System.getenv("NEZHA_KEYSTORE")
            storeFile = keystorePath?.let { rootProject.file(it) }
            storePassword =
                properties
                    .getProperty("key.storePassword")
                    ?: System.getenv("NEZHA_KEYSTORE_PASSWORD")
            keyAlias =
                properties
                    .getProperty("key.alias")
                    ?: System.getenv("NEZHA_KEYSTORE_ALIAS")
            keyPassword =
                properties
                    .getProperty("key.keyPassword")
                    ?: System.getenv("NEZHA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // 设置页需要显示版本号；用生成的 BuildConfig 而不是去读包信息，
        // 既避免 API 33 前后 getPackageInfo 的重载差异，也不必为此在界面里取 Context。
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
