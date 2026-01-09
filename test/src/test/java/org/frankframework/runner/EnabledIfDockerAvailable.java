package org.frankframework.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JUnit Condition that enables the annotated test or test class only if Docker is available. Used when using singleton testcontainers which
 * cannot use {@code @Testcontainers(disabledWithoutDocker = true)}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
