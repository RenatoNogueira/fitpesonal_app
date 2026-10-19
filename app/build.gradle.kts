import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Firebase Analytics é opcional: sem app/google-services.json o app compila normalmente e a coleta fica desligada.
// Passo a passo em docs/ANALYTICS.md.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Chave da API OpenWeather: lida de local.properties (fora do controle de versão) ou da variável de ambiente.
// Sem chave, o app compila e o cartão de clima simplesmente não aparece.
// (Declarada depois do bloco plugins {}, que precisa ser o primeiro comando do script.)
val chaveOpenWeather: String = run {
    val props = Properties()
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { props.load(it) }
    (props.getProperty("OPENWEATHER_API_KEY") ?: System.getenv("OPENWEATHER_API_KEY") ?: "")
        .trim().replace("\"", "")
}

// Assinatura de release: lida de keystore.properties (fora do controle de versão) ou de variáveis de ambiente.
// Sem os dados, o build de release sai sem assinatura (útil para CI/verificação); nunca há senha no repositório.
val dadosAssinatura: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { f -> Properties().also { p -> f.inputStream().use(p::load) } }

fun segredoAssinatura(chave: String, ambiente: String): String? =
    (dadosAssinatura?.getProperty(chave) ?: System.getenv(ambiente))?.trim()?.takeIf { it.isNotEmpty() }

// Versão: -PversionCode=2 -PversionName=1.0.0 na linha de comando (ou em gradle.properties).
val codigoVersao: Int = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 10
val nomeVersao: String = (project.findProperty("versionName") as String?) ?: "1.0.0"

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "br.com.fitpersonal.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.fitpersonal.app"
        minSdk = 26
        targetSdk = 36
        versionCode = codigoVersao
        versionName = nomeVersao
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$chaveOpenWeather\"")
        // Carregador de dados fictícios (Perfil → Dados). Desligado por padrão; ligado só em debug/homologacao.
        buildConfigField("boolean", "DADOS_TESTE", "false")
    }

    signingConfigs {
        val arquivoChave = segredoAssinatura("storeFile", "RJE_STORE_FILE")
        val senhaChave = segredoAssinatura("storePassword", "RJE_STORE_PASSWORD")
        val alias = segredoAssinatura("keyAlias", "RJE_KEY_ALIAS")
        val senhaAlias = segredoAssinatura("keyPassword", "RJE_KEY_PASSWORD")
        if (arquivoChave != null && senhaChave != null && alias != null && senhaAlias != null) {
            create("release") {
                storeFile = rootProject.file(arquivoChave)
                storePassword = senhaChave
                keyAlias = alias
                keyPassword = senhaAlias
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DADOS_TESTE", "true")
        }
        // Igual ao release (minificado, não depurável), assinado com a chave de debug e COM os dados de teste:
        // serve para validar o comportamento de produção antes de publicar. Nunca publique este tipo.
        create("homologacao") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "DADOS_TESTE", "true")
            matchingFallbacks += listOf("release")
        }
        release {
            buildConfigField("boolean", "DADOS_TESTE", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":contrato"))
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // QR Code do Pix (só o núcleo, sem tela de leitura)
    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation("androidx.sqlite:sqlite:2.4.0")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Estatísticas de uso anônimas (opt-in do usuário). Sem google-services.json o SDK fica inerte.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")

    testImplementation("junit:junit:4.13.2")
    // org.json real na JVM (no android.jar de testes ele é um stub que não executa)
    testImplementation("org.json:json:20240303")

    // Testes instrumentados (src/androidTest): rodam em aparelho/emulador, com Keystore e SQLCipher reais
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
