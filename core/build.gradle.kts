import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}

// LabyMod's addon annotation processor is required for main sources only. Keeping it away from
// plain JUnit sources also avoids treating tests as an addon entrypoint.
tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-proc:none")
}
