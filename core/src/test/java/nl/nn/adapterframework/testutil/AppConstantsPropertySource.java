package nl.nn.adapterframework.testutil;

import java.util.Properties;

import nl.nn.adapterframework.lifecycle.CustomPropertySourcePostProcessor;
import nl.nn.adapterframework.util.AppConstants;

public class AppConstantsPropertySource extends CustomPropertySourcePostProcessor {

	@Override
	protected void convertProperties(Properties props) {
		props.putAll(AppConstants.getInstance());
	}

}
