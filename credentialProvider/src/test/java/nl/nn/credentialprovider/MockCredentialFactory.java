package nl.nn.credentialprovider;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

public class MockCredentialFactory extends HashMap<String,Credentials> implements ICredentialFactory {

	@Override
	public boolean hasCredentials(String alias) {
		return containsKey(alias);
	}

	@Override
	public Credentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		Credentials result = new Credentials(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		Credentials entry = get(alias);
		if (entry!=null) {
			if (entry.getUsername()!=null && !entry.getUsername().isEmpty()) result.setUsername(entry.getUsername());
			if (entry.getPassword()!=null && !entry.getPassword().isEmpty()) result.setPassword(entry.getPassword());
		}
		return result;
	}

	public void add(String alias, String username, String password) {
		put(alias, new Credentials(alias, ()->username, ()->password));
	}

	@Override
	public Collection<String> getConfiguredAliases() throws Exception {
		return keySet();
	}
}
