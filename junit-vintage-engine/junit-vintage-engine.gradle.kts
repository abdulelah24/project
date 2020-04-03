import aQute.bnd.gradle.BundleTaskConvention;

plugins {
	`java-library-conventions`
	`junit4-compatibility`
}

apply(from = "$rootDir/gradle/testing.gradle.kts")

description = "JUnit Vintage Engine"

dependencies {
	internal(platform(project(":dependencies")))

	api(platform(project(":junit-bom")))
	api("org.apiguardian:apiguardian-api")
	api(project(":junit-platform-engine"))
	api("junit:junit")

	testImplementation(project(":junit-platform-launcher"))
	testImplementation(project(":junit-platform-runner"))
	testImplementation(project(":junit-platform-testkit"))
}

val String.version: String get() = rootProject.extra["$this.version"] as String

tasks.jar {
	withConvention(BundleTaskConvention::class) {
		val junit4Min = "junit4Min".version
		bnd("""
			# Import JUnit4 packages with a version
			Import-Package: \
				!org.apiguardian.api,\
				junit.runner;version="[$junit4Min,5)",\
				org.junit;version="[$junit4Min,5)",\
				org.junit.experimental.categories;version="[$junit4Min,5)",\
				org.junit.internal.builders;version="[$junit4Min,5)",\
				org.junit.platform.commons.logging;status=INTERNAL,\
				org.junit.runner.*;version="[$junit4Min,5)",\
				org.junit.runners.model;version="[$junit4Min,5)",\
				*
		""")
	}
}
