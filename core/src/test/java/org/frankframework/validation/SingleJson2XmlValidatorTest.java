package org.frankframework.validation;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Convenience Test Class for validators, useful when developing.
 *
 * @author Gerrit van Brakel
 */
@RunWith(value=JUnit4.class)
public class SingleJson2XmlValidatorTest extends Json2XmlValidatorTest {

	public static final int implementationIndex=0;

	public SingleJson2XmlValidatorTest() {
		super((Class<? extends AbstractXmlValidator>)(((Object[])(data().toArray()[implementationIndex]))[0]));
	}

}
