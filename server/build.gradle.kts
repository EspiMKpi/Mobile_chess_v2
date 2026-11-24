// :server - ứng dụng Java chạy trên PC (Swing ServerMainFrm, ServerSocket, JDBC MySQL).
plugins {
    application
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":common"))
    runtimeOnly(libs.mysql.connector.j)
    testImplementation(libs.junit)
    testImplementation(libs.mysql.connector.j)
}

application {
    mainClass.set("server.ServerRun")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnit()
}

// gradlew :server:run chạy với thư mục làm việc server/ -> đọc server/config/db.properties
tasks.named<JavaExec>("run") {
    workingDir = projectDir
}
