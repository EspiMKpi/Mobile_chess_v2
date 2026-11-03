// :common - thư viện Java THUẦN dùng chung cho :server (PC) và :app (Android).
// Không dùng Swing/AWT, JDBC hay android.* trong module này.
plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

dependencies {
    testImplementation(libs.junit)
}
