import java.util.Properties

plugins {
	kotlin("jvm") version "2.2.0"
	id("org.jetbrains.dokka-javadoc") version "2.0.0"
	`maven-publish`
	`java-library`
	signing
	application
}

group = "org.bread_experts_group"
version = "3.0.2"

repositories {
	mavenCentral()
	maven { url = uri("https://maven.breadexperts.group/") }
}

dependencies {
	implementation("org.bread_experts_group:bread_server_lib-code:2.51.9")
	implementation("org.bread_experts_group:static_microserver-code:3.1.6")
}

tasks.test {
	useJUnitPlatform()
}
application {
	mainClass = "org.bread_experts_group.maven_microserver.MavenMainKt"
	applicationDefaultJvmArgs = listOf(
		"-XX:+UseZGC", "-Xms256m", "-Xmx256m", "-XX:SoftMaxHeapSize=128m", "-server",
		"-XX:MaxDirectMemorySize=128m", "-XX:+AlwaysPreTouch", "-XX:+UseLargePages",
		"-XX:+DisableExplicitGC", "-XX:MaxTenuringThreshold=1", "-XX:MaxGCPauseMillis=20",
		"-Djdk.tls.rejectClientInitiatedRenegotiation=true"
	)
}

kotlin {
	jvmToolchain(21)
}
tasks.register<Jar>("dokkaJavadocJar") {
	dependsOn(tasks.dokkaGeneratePublicationJavadoc)
	from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
	archiveClassifier.set("javadoc")
}
val localProperties = Properties().apply {
	rootProject.file("local.properties").reader().use(::load)
}
publishing {
	publications {
		create<MavenPublication>("mavenKotlinDist") {
			artifact(tasks.distZip)
			artifact(tasks.distTar)
		}
		create<MavenPublication>("mavenKotlin") {
			artifactId = "$artifactId-code"
			from(components["kotlin"])
			artifact(tasks.kotlinSourcesJar)
			artifact(tasks["dokkaJavadocJar"])
			pom {
				name = "Maven file server"
				description = "Distribution of software for Bread Experts Group Maven file servers."
				url = "https://breadexperts.group"
				signing {
					sign(publishing.publications["mavenKotlin"])
					sign(configurations.archives.get())
				}
				licenses {
					license {
						name = "GNU General Public License v3.0"
						url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
					}
				}
				developers {
					developer {
						id = "mikoe"
						name = "Miko Elbrecht"
						email = "miko@breadexperts.group"
					}
				}
				scm {
					connection = "scm:git:git://github.com/Bread-Experts-Group/bread_server_lib.git"
					developerConnection = "scm:git:ssh://git@github.com:Bread-Experts-Group/maven_micro_server.git"
					url = "https://breadexperts.group"
				}
			}
		}
	}
	repositories {
		maven {
			url = uri("https://maven.breadexperts.group/")
			credentials {
				username = localProperties["mavenUser"] as String
				password = localProperties["mavenPassword"] as String
			}
		}
	}
}
signing {
	useGpgCmd()
	sign(publishing.publications["mavenKotlinDist"])
	sign(publishing.publications["mavenKotlin"])
}
tasks.javadoc {
	if (JavaVersion.current().isJava9Compatible) {
		(options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
	}
}