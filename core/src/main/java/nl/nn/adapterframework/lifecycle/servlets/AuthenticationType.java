package nl.nn.adapterframework.lifecycle.servlets;

import lombok.Getter;

public enum AuthenticationType {
	AD(ActiveDirectoryAuthenticator.class),
	JEE(NoOpAuthenticator.class),
	ANONYMOUS(NoOpAuthenticator.class);
	//LdapAuthenticationProvider

	private final @Getter IAuthenticator authenticator;
	private AuthenticationType(Class<? extends IAuthenticator> clazz) {
		try {
			authenticator = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
