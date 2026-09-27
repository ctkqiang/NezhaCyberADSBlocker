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

/** 版本号的回退值：未注入环境变量时使用，与首次发布的取值保持一致。 */
val fallbackVersionCode = 1

val fallbackVersionName = "0.1.0"

/**
 * 由发版工作流从 git tag 注入的版本号。
 *
 * 工作流把 `v1.2.3` 解析成 `NEZHA_VERSION_NAME=1.2.3` 与 `NEZHA_VERSION_CODE=10203`
 * （major×10000 + minor×100 + patch）后传入。本地构建不设这两个变量，取值与之前完全一致。
 */
val injectedVersionName = System.getenv("NEZHA_VERSION_NAME")?.takeIf { it.isNotBlank() }

val injectedVersionCode = System.getenv("NEZHA_VERSION_CODE")?.toIntOrNull()

android {
    namespace = "xin.ctkqiang.nezha_cyber.ads_block"
    compileSdk = 36

    defaultConfig {
        applicationId = "xin.ctkqiang.nezha_cyber.ads_block"
        minSdk = 26
        targetSdk = 36
        versionCode = injectedVersionCode ?: fallbackVersionCode
        versionName = injectedVersionName ?: fallbackVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * 能力维度：按「能否被任意来源侧载安装」切分。
     *
     * Google Play Protect 对**互联网来源侧载**（浏览器、聊天工具、文件管理器下载的）APK 有一份
     * 自动拦截清单，声明其中任一权限即被直接拒绝安装，`BIND_NOTIFICATION_LISTENER_SERVICE`
     * 正在清单内。本应用的通知拦截必须声明该权限，于是整包被一并拦下。
     *
     * 关键点：这份拦截**只看 manifest 声明，与 APK 内存在哪些类无关**。因此这里只分两个变体、
     * 由清单决定差异，不改动任何源代码——standard 变体不声明该 service，full 变体保留。
     * 过滤引擎、规则资产与十个小组件在两个变体里共用同一份实现，避免核心逻辑承担分叉风险。
     */
    flavorDimensions += "capability"

    productFlavors {
        // 侧载友好变体：不含通知监听组件，不会命中 Play Protect 的拦截清单。
        create("standard") {
            dimension = "capability"
            buildConfigField("boolean", "NOTIFICATION_INTERCEPTION", "false")
        }

        // 完整变体：保留通知拦截。从浏览器等来源安装时需先关闭「Play 保护机制」，否则同样被拦。
        create("full") {
            dimension = "capability"
            buildConfigField("boolean", "NOTIFICATION_INTERCEPTION", "true")
        }
    }

    /**
     * Release 签名配置。
     *
     * 密钥库路径与口令从 `local.properties`（本机）或环境变量（CI）读取，调用方因此不必改动脚本。
     *
     * **本项目的发布密钥是刻意公开的**：`app/nezha-release.jks` 随仓库提交，口令明文写在
     * `.github/workflows/release.yml` 的 `env` 里，任何人都能复现出签名一致的安装包。代价同样明确
     * ——任何人都能用它签出冒充本应用的 APK，且签名无法吊销。这是权衡后的选择，不是疏忽：可复现性
     * 与可验证性在这里优先于签名独占。
     *
     * 未配置时 release 构建回退到 debug 签名，便于本地验证；正式发版必须提供密钥库，否则部分系统
     * 安装器会拒绝安装。
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

/**
 * 本地安装入口：`./gradlew installDebug`。
 *
 * 加了 `capability` 维度之后「debug」不再是一个变体而是两个，Gradle 于是拒绝为 `installDebug`
 * 猜目标——它按驼峰拆词匹配，`installStandardDebug`、`installFullDebug` 与两个 androidTest 变体
 * 全都命中，四个候选无法裁决。这里补回一个精确同名的任务，精确匹配优先于模式匹配，
 * 命令因此重新可用。
 *
 * 指向 full 而不是 standard：不加限定词时应当拿到功能最全的那个包，这与加维度之前
 * `installDebug` 装的是「当时唯一那个 debug 包」是同一个语义。调试包走 ADB 安装，而 ADB
 * 不属于「互联网来源侧载」，Play Protect 不会拦它，因此 full 在本地安装没有额外风险。
 *
 * 要装另一个变体必须写明：`./gradlew installStandardDebug`。刻意不做成二选一自动判断——
 * 两个变体的差别是通知监听的权限声明，装错了会得到「功能怎么没了」的困惑，
 * 而安装日志里出现的 APK 名（app-full-debug.apk / app-standard-debug.apk）会如实说明装的是哪个。
 */
tasks.register("installDebug") {
    group = "install"
    description = "安装 fullDebug 调试包，等价于 installFullDebug。要装 standard 请用 installStandardDebug"
    dependsOn("installFullDebug")
}
