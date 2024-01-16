package org.frankframework.validation;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Convenience Test Class for validators, useful when developing.
 *
 * @author Gerrit van Brakel
 */
@RunWith(value=JUnit4.class)
public class SingleXmlValidatorTest extends AbstractXmlValidatorTestBase {

	public static final int implementationIndex=0;

	public SingleXmlValidatorTest() {
		super((Class<? extends AbstractXmlValidator>)(((Object[])(data().toArray()[implementationIndex]))[0]));
	}

}
