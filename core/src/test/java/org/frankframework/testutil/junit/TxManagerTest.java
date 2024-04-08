package org.frankframework.testutil.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(Lifecycle.PER_CLASS)

//Only adds these two additional ArgumentSources
@Nested
@DatabaseTest
@NarayanaArgumentSource
public @interface TxManagerTest {

}
