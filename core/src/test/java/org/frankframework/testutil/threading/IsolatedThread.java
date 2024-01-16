package org.frankframework.testutil.threading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes sure the Junit Test is run in a different thread.
 * Only works if
 * <pre>
 * @Rule public RunInThreadRule runInThread = new RunInThreadRule();
 * </pre>
 * is present!
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IsolatedThread {
}
