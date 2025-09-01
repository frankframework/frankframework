package org.frankframework.credentialprovider;

import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class MockCredentialFactory extends HashMap<String, ICredentials> implements ICredentialProvider {

	private static MockCredentialFactory instance;

	public static MockCredentialFactory getInstance() {
		if (instance == null) {
			instance = new MockCredentialFactory();
		}
		return instance;
	}

	@Override
	public void initialize() {
		// NO OP
	}

	@Override
	public boolean hasCredentials(String alias) {
		return getInstance().containsKey(alias);
	}

	@Override
	public ICredentials getCredentials(String alias) throws NoSuchElementException {
		ICredentials credentials = getInstance().get(alias);
		if (credentials == null) {
			throw new NoSuchElementException("credentials not found");
		}
		credentials.getUsername(); // Validate validity, may throw NoSuchElementException
		return credentials;
	}

	public static void add(String alias, String username, String password) {
		getInstance().put(alias, new MockCredential(alias, username, password));
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		return getInstance().keySet();
	}

	@Override
	public String toString() {
		return "MockCredentialFactory@"+this.hashCode()+"#aliasses"+this.keySet();
	}

	@Getter
	@AllArgsConstructor
	private static class MockCredential implements ICredentials {
		private final String alias;
		private final String username;
		private final String password;
	}
}
