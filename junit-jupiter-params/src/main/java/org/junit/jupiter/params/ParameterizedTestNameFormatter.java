/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.util.stream.Collectors.joining;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.StringUtils;

/**
 * @since 5.0
 */
class ParameterizedTestNameFormatter {

	private final String namePattern;

	ParameterizedTestNameFormatter(String namePattern) {
		this.namePattern = namePattern;
	}

	String format(int invocationIndex, Arguments arguments) {
		String pattern = prepareMessageFormatPattern(invocationIndex, arguments);
		Object[] humanReadableArguments = makeReadable(arguments);
		return formatSafely(pattern, humanReadableArguments);
	}

	private String prepareMessageFormatPattern(int invocationIndex, Arguments arguments) {
		// todo: shall it be split into three methods for clarity?
		String result = namePattern.replace("{index}", String.valueOf(invocationIndex));
		result = result.replace("{arguments.description}", arguments.getDescription().orElse(""));
		if (result.contains("{arguments}")) {
			// @formatter:off
			String replacement = IntStream.range(0, arguments.size())
					.mapToObj(index -> "{" + index + "}")
					.collect(joining(", "));
			// @formatter:on
			result = result.replace("{arguments}", replacement);
		}
		return result;
	}

	// todo: Rename to extractReadable? Or leave as it is and pass an array of objects?
	private Object[] makeReadable(Arguments arguments) {
		// Note: humanReadableArguments must be an Object[] in order to
		// avoid varargs issues with non-Eclipse compilers.
		// @formatter:off
		Object[] humanReadableArguments = //
			Arrays.stream(arguments.get())
					.map(StringUtils::nullSafeToString)
					.toArray(String[]::new);
		// @formatter:on
		return humanReadableArguments;
	}

	private String formatSafely(String pattern, Object[] arguments) {
		try {
			return MessageFormat.format(pattern, arguments);
		}
		catch (IllegalArgumentException ex) {
			String message = "The naming pattern defined for the parameterized tests is invalid. "
					+ "The nested exception contains more details.";
			throw new JUnitException(message, ex);
		}
	}

}
