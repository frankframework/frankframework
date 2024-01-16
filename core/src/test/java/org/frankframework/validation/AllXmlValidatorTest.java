package org.frankframework.validation;


import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class AllXmlValidatorTest extends AbstractXmlValidatorTestBase {

	public AllXmlValidatorTest(Class<AbstractXmlValidator> implementation) {
		super(implementation);
	}

}
