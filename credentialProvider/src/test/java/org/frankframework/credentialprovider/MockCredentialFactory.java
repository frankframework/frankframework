package org.frankframework.credentialprovider;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("serial")
public class MockCredentialFactory extends HashMap<String, ISecret> implements ISecretProvider {

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
	public boolean hasSecret(CredentialAlias alias) {
		return getInstance().containsKey(alias.getName());
	}

	@Override
	public ISecret getSecret(CredentialAlias alias) throws NoSuchElementException {
		ISecret credentials = getInstance().get(alias.getName());
		if (credentials == null) {
			throw new NoSuchElementException("credentials not found");
		}
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
	private static class MockCredential implements ISecret {
		private final String alias;
		private final String username;
		private final String password;

		@Override
		public String getField(String fieldname) throws IOException {
			if ("username".equals(fieldname) && username != null) {
				return username;
			}
			return password;
		}
	}
}
