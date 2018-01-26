/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.junit.jupiter.api.EnabledIf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.platform.commons.util.PreconditionViolationException;

class EnabledIfConditionTests {

	@Test
	void findJavaScriptEngine() {
		assertAll("Names", //
			() -> findJavaScriptEngine("Nashorn"), //
			() -> findJavaScriptEngine("nashorn"), //
			() -> findJavaScriptEngine("javascript"), //
			() -> findJavaScriptEngine("ecmascript") //
		);

		assertAll("File extension", //
			() -> findJavaScriptEngine("js") //
		);

		assertAll("MIME types", //
			() -> findJavaScriptEngine("application/javascript"), //
			() -> findJavaScriptEngine("application/ecmascript"), //
			() -> findJavaScriptEngine("text/javascript"), //
			() -> findJavaScriptEngine("text/ecmascript") //
		);

		assertThrows(PreconditionViolationException.class, () -> findJavaScriptEngine("?!"));
	}

	private void findJavaScriptEngine(String string) {
		EnabledIfCondition condition = new EnabledIfCondition();
		ScriptEngine engine = condition.findScriptEngine(string);
		assertNotNull(engine);
	}

	@Test
	void trivialJavaScript() {
		String script = "true";
		EnabledIf annotation = mockEnabledIfAnnotation(script);
		String actual = new EnabledIfCondition().createScript(annotation, "ECMAScript");
		assertSame(script, actual);
	}

	@Test
	void trivialGroovyScript() {
		String script = "true";
		EnabledIf annotation = mockEnabledIfAnnotation(script);
		String actual = new EnabledIfCondition().createScript(annotation, "Groovy");
		assertSame(script, actual);
	}

	@Test
	void trivialNonJavaScript() {
		EnabledIf annotation = mockEnabledIfAnnotation("one", "two");
		assertLinesMatchCreatedScript(Arrays.asList("one", "two"), annotation, "unknown language");
	}

	@Test
	void createJavaScriptMultipleLines() {
		EnabledIf annotation = mockEnabledIfAnnotation("m1()", "m2()");
		assertLinesMatchCreatedScript(Arrays.asList("m1()", "m2()"), annotation, "ECMAScript");
	}

	@Test
	void createReasonWithDefaultMessage() {
		EnabledIf annotation = mockEnabledIfAnnotation("?");
		String actual = new EnabledIfCondition().createReason(annotation, "?", "!");
		assertEquals("Script `?` evaluated to: !", actual);
	}

	@Test
	void createReasonWithCustomMessage() {
		EnabledIf annotation = mockEnabledIfAnnotation("?");
		when(annotation.reason()).thenReturn("result={result} script={script}");
		String actual = new EnabledIfCondition().createReason(annotation, "?", "!");
		assertEquals("result=! script=?", actual);
	}

	@Test
	@EnabledIf(value = "true", reason = "{annotation}")
	void createReasonWithAnnotationPlaceholder() throws Exception {
		EnabledIf annotation = getClass() //
				.getDeclaredMethod("createReasonWithAnnotationPlaceholder") //
				.getDeclaredAnnotation(EnabledIf.class);
		ConditionEvaluationResult result = new EnabledIfCondition().evaluate(annotation, this::mockBinder);
		assertFalse(result.isDisabled());
		String expected = "@org.junit.jupiter.api.EnabledIf(reason=\"{annotation}\", value={\"true\"})";
		String actual = result.getReason().orElseThrow(() -> new AssertionError("causeless"));
		assertEquals(expected, actual);
	}

	private void mockBinder(Bindings bindings) {
		bindings.put("jupiterTags", Collections.emptySet());
		bindings.put("jupiterUniqueId", "Mock for UniqueId");
		bindings.put("jupiterDisplayName", "Mock for DisplayName");
		bindings.put("jupiterConfigurationParameter", Collections.emptyMap());
	}

	private void assertLinesMatchCreatedScript(List<String> expectedLines, EnabledIf annotation, String language) {
		EnabledIfCondition condition = new EnabledIfCondition();
		String actual = condition.createScript(annotation, language);
		assertLinesMatch(expectedLines, Arrays.asList(actual.split("\\R")));
	}

	private EnabledIf mockEnabledIfAnnotation(String... value) {
		EnabledIf annotation = mock(EnabledIf.class);
		when(annotation.value()).thenReturn(value);
		try {
			when(annotation.reason()).thenReturn(
				(String) EnabledIf.class.getDeclaredMethod("reason").getDefaultValue());
		}
		catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
		return annotation;
	}
}
