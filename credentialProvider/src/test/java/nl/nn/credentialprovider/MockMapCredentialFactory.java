package nl.nn.credentialprovider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import nl.nn.credentialprovider.util.AppConstants;

public class MockMapCredentialFactory extends MapCredentialFactory {

	public MockMapCredentialFactory() throws IOException {
		super();
	}

	@Override
	public String getPropertyBase() {
		return "mockCredentiaFactory";
	}

	@Override
	protected Map<String, String> getCredentialMap(AppConstants appConstants) throws MalformedURLException, IOException {
		Map<String,String>  map = new HashMap<>();
		map.put("account/username", "fakeUsername");
		map.put("account/password", "fakePassword");
		return map;
	}
	
}
