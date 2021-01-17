/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testsuites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.testcases.Simple;

public class NestedSuite {

	@Suite
	@SelectClasses(Simple.class)
	static public class Jupiter {

	}

	@Suite
	@SelectClasses(org.junit.platform.suite.engine.testcases.Tagged.class)
	static public class Tagged {

	}

}
