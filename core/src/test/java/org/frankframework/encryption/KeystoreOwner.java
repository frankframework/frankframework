package org.frankframework.encryption;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

public class KeystoreOwner implements HasKeystore {

	private @Getter KeystoreConfiguration keystoreConfiguration = createKeystoreConfiguration();
	private final @Getter ClassLoader configurationClassLoader = getClass().getClassLoader();
	private @Getter @Setter String name = null;
	private @Getter @Setter ApplicationContext applicationContext = null;

	public KeystoreOwner(String keystore) {
		this.keystoreConfiguration.setKeystoreResource(keystore);
	}

	@Override
	public void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		// not implemented yet
	}
}
