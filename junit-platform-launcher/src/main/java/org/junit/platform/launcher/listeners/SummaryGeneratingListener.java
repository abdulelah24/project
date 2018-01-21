/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static java.util.stream.Stream.concat;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.PreconditionViolationException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Simple {@link TestExecutionListener} that generates a
 * {@linkplain TestExecutionSummary summary} of the test execution.
 *
 * @since 1.0
 * @see #getSummary()
 */
@API(status = MAINTAINED, since = "1.0")
public class SummaryGeneratingListener implements TestExecutionListener {

	private TestPlan testPlan;
	private MutableTestExecutionSummary summary;

	/**
	 * Get the summary generated by this listener.
	 */
	public TestExecutionSummary getSummary() {
		return this.summary;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.testPlan = testPlan;
		this.summary = new MutableTestExecutionSummary(testPlan);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		this.summary.timeFinished = System.currentTimeMillis();
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		if (testIdentifier.isContainer()) {
			this.summary.containersFound.incrementAndGet();
		}
		if (testIdentifier.isTest()) {
			this.summary.testsFound.incrementAndGet();
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		// @formatter:off
		long skippedContainers = concat(Stream.of(testIdentifier), testPlan.getDescendants(testIdentifier).stream())
				.filter(TestIdentifier::isContainer)
				.count();
		long skippedTests = concat(Stream.of(testIdentifier), testPlan.getDescendants(testIdentifier).stream())
				.filter(TestIdentifier::isTest)
				.count();
		// @formatter:on
		this.summary.containersSkipped.addAndGet(skippedContainers);
		this.summary.testsSkipped.addAndGet(skippedTests);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isContainer()) {
			this.summary.containersStarted.incrementAndGet();
		}
		if (testIdentifier.isTest()) {
			this.summary.testsStarted.incrementAndGet();
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {

		switch (testExecutionResult.getStatus()) {

			case SUCCESSFUL: {
				if (testIdentifier.isContainer()) {
					this.summary.containersSucceeded.incrementAndGet();
				}
				if (testIdentifier.isTest()) {
					this.summary.testsSucceeded.incrementAndGet();
				}
				return;
			}

			case ABORTED: {
				if (testIdentifier.isContainer()) {
					this.summary.containersAborted.incrementAndGet();
				}
				if (testIdentifier.isTest()) {
					this.summary.testsAborted.incrementAndGet();
				}
				return;
			}

			case FAILED: {
				if (testIdentifier.isContainer()) {
					this.summary.containersFailed.incrementAndGet();
				}
				if (testIdentifier.isTest()) {
					this.summary.testsFailed.incrementAndGet();
				}
				testExecutionResult.getThrowable().ifPresent(
					throwable -> this.summary.addFailure(testIdentifier, throwable));
				return;
			}
		}

		throw new PreconditionViolationException("Unsupported execution status:" + testExecutionResult.getStatus());
	}

}
