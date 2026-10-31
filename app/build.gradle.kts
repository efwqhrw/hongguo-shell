plugins {
    id("com.android.application")
}

android {
    namespace = "com.hongguo.shell"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hongguo.shell"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 源码里含中文（隐藏用的文字表），强制按 UTF-8 编译，避免 Windows 下变乱码
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
