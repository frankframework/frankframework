package org.frankframework.testutil.threading;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RunInThreadRule implements TestRule {

	@Override
	public Statement apply(Statement base, Description description) {
		Statement result = base;
		IsolatedThread annotation = description.getAnnotation(IsolatedThread.class);
		if (annotation != null) {
			result = new RunTestInThread(base);
		}
		return result;
	}
}
