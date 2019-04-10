
/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

module org.junit.platform.testkit {
	requires transitive org.assertj.core;
	requires transitive org.junit.platform.launcher;
	requires transitive org.opentest4j;

	// exports org.junit.platform.testkit; empty package
	exports org.junit.platform.testkit.engine;
}
