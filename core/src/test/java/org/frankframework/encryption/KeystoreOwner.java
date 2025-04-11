package org.frankframework.encryption;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

public class KeystoreOwner implements HasKeystore {

	private final @Getter ClassLoader configurationClassLoader = getClass().getClassLoader();
	private @Getter @Setter String keystore;
	private @Getter @Setter String keystoreAuthAlias;
	private @Getter @Setter String keystorePassword;
	private @Getter @Setter KeystoreType keystoreType=KeystoreType.PKCS12;
	private @Getter @Setter String keystoreAlias;
	private @Getter @Setter String keystoreAliasAuthAlias;
	private @Getter @Setter String keystoreAliasPassword;
	private @Getter @Setter String keyManagerAlgorithm=null;
	private @Getter @Setter String name=null;
	private @Getter @Setter ApplicationContext applicationContext=null;

	public KeystoreOwner(String keystore) {
		this.keystore = keystore;
	}
}
