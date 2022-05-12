package nl.nn.adapterframework.util;

import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class TransformerPoolNamespaceUnawarenessTest78 extends TransformerPoolNamespaceUnawarenessTest {

	@Before
	public void setup() {
		xpath_0_and_2_result = XPATH_0_AND_2_RESULT_7_8;
		stylesheet_auto_unaware_result = STYLESHEET_AUTO_UNAWARE_RESULT_7_8;
		xslt1_unaware_result = XSLT1_UNAWARE_RESULT_7_8;
	}
	
	@Override
	public TransformerPool getTransformerPool(String xpath, String stylesheet, int xsltVersion) throws ConfigurationException { 
		return TransformerPool.configureTransformer0("transformerpool test", null, null, xpath, stylesheet, OutputType.TEXT, false, null, xsltVersion);
	}

}
