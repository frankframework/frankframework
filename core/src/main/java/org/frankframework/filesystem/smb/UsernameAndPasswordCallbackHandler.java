/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.filesystem.smb;

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

	@Override
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
