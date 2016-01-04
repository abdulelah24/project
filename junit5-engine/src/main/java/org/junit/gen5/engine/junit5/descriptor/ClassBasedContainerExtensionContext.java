/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.descriptor;

import java.lang.reflect.AnnotatedElement;

import org.junit.gen5.api.extension.ContainerExtensionContext;
import org.junit.gen5.api.extension.ExtensionContext;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.junit5.execution.JUnit5EngineExecutionContext;

final class ClassBasedContainerExtensionContext extends AbstractExtensionContext implements ContainerExtensionContext {

	public ClassBasedContainerExtensionContext(ExtensionContext parent, JUnit5EngineExecutionContext context,
			ClassTestDescriptor testDescriptor) {
		super(parent, context, testDescriptor);
	}

	@Override
	public Class<?> getTestClass() {
		return ((ClassTestDescriptor) getTestDescriptor()).getTestClass();
	}

	@Override
	public String getDisplayName() {
		return getTestDescriptor().getDisplayName();
	}

	@Override
	public AnnotatedElement getElement() {
		return getTestClass();
	}

}
