/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.0
 */
class ParameterizedTestExtension implements TestTemplateInvocationContextProvider {

	private static final Logger logger = LoggerFactory.getLogger(ParameterizedTestExtension.class);

	static final String METHOD_CONTEXT_KEY = "context";
	static final String ARGUMENT_MAX_LENGTH_KEY = "junit.jupiter.params.displayname.argument.maxlength";
	static final String DEFAULT_DISPLAY_NAME = "{default_display_name}";
	static final String DISPLAY_NAME_PATTERN_KEY = "junit.jupiter.params.displayname.default";
	static final String ARGUMENT_COUNT_VALIDATION_KEY = "junit.jupiter.params.argumentCountValidation";

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		if (!context.getTestMethod().isPresent()) {
			return false;
		}

		Method templateMethod = context.getTestMethod().get();
		Optional<ParameterizedTest> annotation = findAnnotation(templateMethod, ParameterizedTest.class);
		if (!annotation.isPresent()) {
			return false;
		}

		ParameterizedTestMethodContext methodContext = new ParameterizedTestMethodContext(templateMethod,
			annotation.get());

		Preconditions.condition(methodContext.hasPotentiallyValidSignature(),
			() -> String.format(
				"@ParameterizedTest method [%s] declares formal parameters in an invalid order: "
						+ "argument aggregators must be declared after any indexed arguments "
						+ "and before any arguments resolved by another ParameterResolver.",
				templateMethod.toGenericString()));

		getStoreInMethodNamespace(context).put(METHOD_CONTEXT_KEY, methodContext);

		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
			ExtensionContext extensionContext) {

		ParameterizedTestMethodContext methodContext = getMethodContext(extensionContext);
		ParameterizedTestNameFormatter formatter = createNameFormatter(extensionContext, methodContext);
		AtomicLong invocationCount = new AtomicLong(0);

		// @formatter:off
		return findRepeatableAnnotations(methodContext.method, ArgumentsSource.class)
				.stream()
				.map(ArgumentsSource::value)
				.map(clazz -> ParameterizedTestSpiInstantiator.instantiate(ArgumentsProvider.class, clazz, extensionContext))
				.map(provider -> AnnotationConsumerInitializer.initialize(methodContext.method, provider))
				.flatMap(provider -> arguments(provider, extensionContext))
				.map(arguments -> {
					validateArgumentCount(extensionContext, arguments);
					invocationCount.incrementAndGet();
					return createInvocationContext(formatter, methodContext, arguments, invocationCount.intValue());
				})
				.onClose(() ->
						Preconditions.condition(invocationCount.get() > 0 || !methodContext.annotation.requireArguments(),
								"Configuration error: You must configure at least one set of arguments for this @ParameterizedTest"));
		// @formatter:on
	}

	@Override
	public boolean mayReturnZeroTestTemplateInvocationContexts(ExtensionContext extensionContext) {
		ParameterizedTestMethodContext methodContext = getMethodContext(extensionContext);
		return !methodContext.annotation.requireArguments();
	}

	private ParameterizedTestMethodContext getMethodContext(ExtensionContext extensionContext) {
		return getStoreInMethodNamespace(extensionContext)//
				.get(METHOD_CONTEXT_KEY, ParameterizedTestMethodContext.class);
	}

	private ExtensionContext.Store getStoreInMethodNamespace(ExtensionContext context) {
		return context.getStore(Namespace.create(ParameterizedTestExtension.class, context.getRequiredTestMethod()));
	}

	private ExtensionContext.Store getStoreInExtensionNamespace(ExtensionContext context) {
		return context.getStore(Namespace.create(ParameterizedTestExtension.class));
	}

	private void validateArgumentCount(ExtensionContext extensionContext, Arguments arguments) {
		ArgumentCountValidationMode argumentCountValidationMode = getArgumentCountValidationMode(extensionContext);
		switch (argumentCountValidationMode) {
			case DEFAULT:
			case NONE:
				return;
			case STRICT:
				int testParamCount = extensionContext.getRequiredTestMethod().getParameterCount();
				int argumentsCount = arguments.get().length;
				Preconditions.condition(testParamCount == argumentsCount, () -> String.format(
					"Configuration error: the @ParameterizedTest has %s argument(s) but there were %s argument(s) provided.%nNote: the provided arguments are %s",
					testParamCount, argumentsCount, Arrays.toString(arguments.get())));
				break;
			default:
				throw new ExtensionConfigurationException(
					"Unsupported argument count validation mode: " + argumentCountValidationMode);
		}
	}

	private ArgumentCountValidationMode getArgumentCountValidationMode(ExtensionContext extensionContext) {
		ParameterizedTest parameterizedTest = findAnnotation(//
			extensionContext.getRequiredTestMethod(), ParameterizedTest.class//
		).orElseThrow(NoSuchElementException::new);
		if (parameterizedTest.argumentCountValidation() != ArgumentCountValidationMode.DEFAULT) {
			return parameterizedTest.argumentCountValidation();
		}
		else {
			return getArgumentCountValidationModeConfiguration(extensionContext);
		}
	}

	private ArgumentCountValidationMode getArgumentCountValidationModeConfiguration(ExtensionContext extensionContext) {
		String key = ARGUMENT_COUNT_VALIDATION_KEY;
		ArgumentCountValidationMode fallback = ArgumentCountValidationMode.NONE;
		ExtensionContext.Store store = getStoreInExtensionNamespace(extensionContext);
		return store.getOrComputeIfAbsent(key, __ -> {
			Optional<String> optionalConfigValue = extensionContext.getConfigurationParameter(key);
			if (optionalConfigValue.isPresent()) {
				String configValue = optionalConfigValue.get();
				Optional<ArgumentCountValidationMode> enumValue = Arrays.stream(
					ArgumentCountValidationMode.values()).filter(
						mode -> mode.name().equalsIgnoreCase(configValue)).findFirst();
				if (enumValue.isPresent()) {
					logger.config(() -> String.format(
						"Using ArgumentCountValidationMode '%s' set via the '%s' configuration parameter.",
						enumValue.get().name(), key));
					return enumValue.get();
				}
				else {
					logger.warn(() -> String.format(
						"Invalid ArgumentCountValidationMode '%s' set via the '%s' configuration parameter. "
								+ "Falling back to the %s default value.",
						configValue, key, fallback.name()));
					return fallback;
				}
			}
			else {
				return fallback;
			}
		}, ArgumentCountValidationMode.class);
	}

	private TestTemplateInvocationContext createInvocationContext(ParameterizedTestNameFormatter formatter,
			ParameterizedTestMethodContext methodContext, Arguments arguments, int invocationIndex) {

		return new ParameterizedTestInvocationContext(formatter, methodContext, arguments, invocationIndex);
	}

	private ParameterizedTestNameFormatter createNameFormatter(ExtensionContext extensionContext,
			ParameterizedTestMethodContext methodContext) {

		String name = methodContext.annotation.name();
		String pattern = name.equals(DEFAULT_DISPLAY_NAME)
				? extensionContext.getConfigurationParameter(DISPLAY_NAME_PATTERN_KEY) //
						.orElse(ParameterizedTest.DEFAULT_DISPLAY_NAME)
				: name;
		pattern = Preconditions.notBlank(pattern.trim(),
			() -> String.format(
				"Configuration error: @ParameterizedTest on method [%s] must be declared with a non-empty name.",
				methodContext.method));

		int argumentMaxLength = extensionContext.getConfigurationParameter(ARGUMENT_MAX_LENGTH_KEY, Integer::parseInt) //
				.orElse(512);

		return new ParameterizedTestNameFormatter(pattern, extensionContext.getDisplayName(), methodContext,
			argumentMaxLength);
	}

	protected static Stream<? extends Arguments> arguments(ArgumentsProvider provider, ExtensionContext context) {
		try {
			return provider.provideArguments(context);
		}
		catch (Exception e) {
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}

}
