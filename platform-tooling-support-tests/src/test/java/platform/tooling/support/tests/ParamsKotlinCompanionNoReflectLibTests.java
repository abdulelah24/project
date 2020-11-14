/*
 * Copyright 2015-2020 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import platform.tooling.support.Helper;
import platform.tooling.support.Request;

/**
 * @since 1.3
 */
class ParamsKotlinCompanionNoReflectLibTests {

	@Test
	void verifyParamsKotlinCompanionNoReflectLibProject() {
		var result = Request.builder() //
				.setTool(Request.maven()) //
				.setProject("params-kotlin-companion-no-reflectlib") //
				.addArguments("-Dmaven.repo=" + System.getProperty("maven.repo")) //
				.addArguments("--debug", "verify") //
				.setTimeout(Duration.ofMinutes(2)) //
				.setJavaHome(Helper.getJavaHome("8").orElseThrow(TestAbortedException::new)) //
				.build() //
				.run();

		assumeFalse(result.isTimedOut(), () -> "tool timed out: " + result);

		assertEquals(1, result.getExitCode());
		assertEquals("", result.getOutput("err"));

		assertTrue(result.getOutputLines("out").contains(
			"org.junit.platform.commons.JUnitException: No kotlin-reflect.jar in the classpath"));
		assertTrue(result.getOutputLines("out").contains(
			"Caused by: kotlin.jvm.KotlinReflectionNotSupportedError: Kotlin reflection implementation is not found at runtime. Make sure you have kotlin-reflect.jar in the classpath"));

		assertTrue(result.getOutputLines("out").stream().anyMatch(
			it -> it.startsWith("[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ")
					&& it.endsWith(" s <<< FAILURE! - in com.example.project.MethodSourceFromCompanionTest")));
		assertTrue(result.getOutputLines("out").stream().anyMatch(
			it -> it.startsWith("[ERROR] fromCompanion{String}  Time elapsed: ") && it.endsWith(" s  <<< ERROR!")));

		assertTrue(result.getOutputLines("out").stream().anyMatch(
			it -> it.startsWith("[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ")
					&& it.endsWith(" s - in com.example.project.MethodSourceFromJvmStaticTest")));

		assertTrue(result.getOutputLines("out").stream().anyMatch(
			it -> it.startsWith("[ERROR] Tests run: 2, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: ")
					&& it.endsWith(" s <<< FAILURE! - in com.example.project.TestInstancePerClassTest")));
		assertTrue(result.getOutputLines("out").stream().anyMatch(
			it -> it.startsWith("[ERROR] perClassFromCompanion{String}  Time elapsed: ")
					&& it.endsWith(" s  <<< ERROR!")));

		assertTrue(result.getOutputLines("out").contains("[INFO] BUILD FAILURE"));
		assertTrue(result.getOutputLines("out").contains("[ERROR] Tests run: 4, Failures: 0, Errors: 2, Skipped: 0"));
		assertThat(result.getOutput("out")).contains("Using Java version: 1.8");
	}
}
