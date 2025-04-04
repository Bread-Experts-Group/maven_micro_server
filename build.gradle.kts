plugins {
	kotlin("jvm") version "2.1.10"
	`maven-publish`
}

group = "bread_experts_group"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(kotlin("test"))
	implementation(files("C:\\Users\\Adenosine3Phosphate\\Desktop\\Projects\\bread_server_lib\\build\\libs\\bread_server_lib-1.0-SNAPSHOT.jar"))
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			pom {
				name = "My Library"
				description = "A concise description of my library"
				url = "http://www.example.com/library"
				properties = mapOf(
					"myProp" to "value",
					"prop.with.dots" to "anotherValue"
				)
				licenses {
					license {
						name = "The Apache License, Version 2.0"
						url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
					}
				}
				developers {
					developer {
						id = "johnd"
						name = "John Doe"
						email = "john.doe@example.com"
					}
				}
				scm {
					connection = "scm:git:git://example.com/my-library.git"
					developerConnection = "scm:git:ssh://example.com/my-library.git"
					url = "http://example.com/my-library/"
				}
			}
		}
	}
}
