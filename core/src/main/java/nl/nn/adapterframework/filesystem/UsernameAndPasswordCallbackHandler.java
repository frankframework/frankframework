package nl.nn.adapterframework.filesystem;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class UsernameAndPasswordCallbackHandler implements CallbackHandler {

	private final String user;
	private final String password;

	public UsernameAndPasswordCallbackHandler(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback) {
				NameCallback nc = (NameCallback) callback;
				nc.setName(user);
			} else if (callback instanceof PasswordCallback) {
				PasswordCallback pc = (PasswordCallback) callback;
				pc.setPassword(password.toCharArray());
			} else {
				throw new UnsupportedCallbackException(callback, "Unknown Callback");
			}
		}
	}
}
