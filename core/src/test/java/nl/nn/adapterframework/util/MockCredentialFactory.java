package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import nl.nn.credentialprovider.MapCredentialFactory;
import nl.nn.credentialprovider.util.AppConstants;

public class MockCredentialFactory extends MapCredentialFactory {

	public MockCredentialFactory() throws IOException {
		super();
	}

	public final String CREDENTIALS_PROPERTIES = "mockCredentials.properties";
	
	@Override
	public String getPropertyBase() {
		return null;
	}

	@Override
	protected Map<String, String> getCredentialMap(AppConstants appConstants) throws MalformedURLException, IOException {
		URL url = ClassUtils.getResourceURL(CREDENTIALS_PROPERTIES);
		Properties properties = new Properties();
		try (InputStream is = url.openStream()) {
			properties.load(is);
		}
		Map<String,String> map = new LinkedHashMap<>();
		properties.forEach((k,v) -> map.put((String)k, (String)v));
		return map;
	}

}
