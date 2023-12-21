package org.frankframework.testutil.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Creates a {@link LocalFileServer} with share 'home' @ 'localhost' : 'automatically-calculated-port'.
 *
 * @author Niels Meijer
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Tag("filesystem")
@ExtendWith(JUnitFileServerExtension.class)
public @interface LocalFileSystemMock {

	String username() default "frankframework";
	String password() default "pass_123";
}
