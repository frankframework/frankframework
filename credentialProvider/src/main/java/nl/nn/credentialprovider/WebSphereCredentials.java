/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.credentialprovider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import nl.nn.credentialprovider.util.AppConstants;
import nl.nn.credentialprovider.util.ClassUtils;

/**
 * Provides user-id and password from the WebSphere authentication-alias repository.
 * A default username and password can be set, too.
 *
 * Note:
 * In WSAD the aliases are named just as you type them.
 * In WebSphere 5 and 6, and in RAD7/RSA7 aliases are prefixed with the name of the server.
 * It is therefore sensible to use a environment setting to find the name of the alias.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.2
 */
public class WebSphereCredentials extends Credentials implements CallbackHandler {

	private boolean initialized;
	private boolean aliasFound;

	public WebSphereCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		super(alias, defaultUsernameSupplier, defaultPasswordSupplier);
	}

	public String invokeCharArrayGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		char[] arr = (char[])ClassUtils.invokeGetter(o,name);
		StringBuffer sb = new StringBuffer();
		for (int j=0; j<arr.length;j++) {
			sb.append(arr[j]);
		}
		return sb.toString();
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		log.info("Handling callbacks for alias ["+getAlias()+"]");
		for (int i=0; i<callbacks.length; i++) {
			Callback cb=callbacks[i];
//			log.info(i+") "+cb.getClass().getName()+" "+ToStringBuilder.reflectionToString(cb));
			Class<? extends Callback> cbc = cb.getClass();
			if (cbc.getName().endsWith("MappingPropertiesCallback")) { // Websphere 6
				try {
					Map<String, String> mappingProperties = new HashMap<>();
					mappingProperties.put("com.ibm.mapping.authDataAlias", getAlias());
					ClassUtils.invokeSetter(cb,"setProperties",mappingProperties,Map.class);
					log.fine("MappingPropertiesCallback.properties set to entry key [com.ibm.mapping.authDataAlias], value ["+getAlias()+"]");
					continue;
				} catch (Exception e) {
					log.log(Level.WARNING, "exception setting alias ["+getAlias()+"] on MappingPropertiesCallback", e);
				}
			}
			if (cbc.getName().endsWith("AuthDataAliasCallback")) { // Websphere 5
				try {
					log.info("setting alias of AuthDataAliasCallback to alias ["+getAlias()+"]");
					ClassUtils.invokeSetter(cb,"setAlias",getAlias());
					continue;
				} catch (Exception e) {
					log.log(Level.WARNING, "exception setting alias ["+getAlias()+"] on AuthDataAliasCallback", e);
				}
			}
			if (cb instanceof NameCallback) {
				NameCallback ncb = (NameCallback) cb;
				log.info("setting name of NameCallback to alias ["+getAlias()+"]");
				ncb.setName(getAlias());
				continue;
			}
			log.fine("ignoring callback of type ["+cb.getClass().getName()+"] for alias ["+getAlias()+"]");
		}
		log.info("Handled callbacks for alias ["+getAlias()+"]");
	}

	/*
	 * Dummy principal, to populate subject, to have at least a principal.
	 */
	private static class FrankPrincipal implements Principal{
		@Override
		public String getName() {
			return "Frank";
		}
	}

	@Override
	protected void getCredentialsFromAlias() {
		try {
			initialized = true;
			Set<Principal> principals = new HashSet<>();
			Set<Object> publicCredentials = new HashSet<>();
			Set<Object> privateCredentials = new HashSet<>();

			Principal p = new FrankPrincipal();
			principals.add(p);
			Subject initialSubject= new Subject(false, principals, publicCredentials, privateCredentials);
			String loginConfiguration = AppConstants.getInstance().getProperty("PrincipalMapping", "DefaultPrincipalMapping");
			LoginContext lc = new LoginContext(loginConfiguration, initialSubject, this);
			lc.login();
			Subject s = lc.getSubject();
			//showSet(s.getPrincipals(),"principals");
			//showSet(s.getPublicCredentials(),"PublicCredentials");
			//showSet(s.getPrivateCredentials(),"PrivateCredentials");
			//Object pwcred=Subject.doAsPrivileged(s,new PasswordGetter(s),AccessController.getContext());
			//Object pwcred=AccessController.doPrivileged(new PasswordGetter(s));

			Object pwcred = s.getPrivateCredentials().toArray()[0];

			setUsername(ClassUtils.invokeStringGetter(pwcred,"getUserName"));
			setPassword(invokeCharArrayGetter(pwcred,"getPassword"));
			aliasFound = true;
		} catch (Exception e) {
			NoSuchElementException nsee=new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]");
			nsee.initCause(e);
			throw nsee;
		}
	}

	public boolean isAliasFound() {
		if (!initialized) {
			getCredentialsFromAlias();
		}
		return aliasFound;
	}

}
