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
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

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
//			log.debug("contents of callback ["+ToStringBuilder.reflectionToString(cb)+"]");
//			Class itf[] = cbc.getInterfaces();
//			for (int j=0; j<itf.length; j++) {
//				log.info("interface "+j+": "+itf[j].getName());
//			}
//			Method methods[] = cbc.getMethods();
//			for (int j=0; j<methods.length; j++) {
//				log.info("method "+j+": "+methods[j].getName()+", "+methods[j].toString());
//			}
//			if (cb instanceof ChoiceCallback) {
//				ChoiceCallback ccb = (ChoiceCallback) cb;
//				log.info("ChoiceCallback: "+ccb.getPrompt());
//			}

		}
		log.info("Handled callbacks for alias ["+getAlias()+"]");
	}

	private class LoginCallbackHandler implements CallbackHandler {
		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			//log.info("callback: "+ToStringBuilder.reflectionToString(callbacks));
			for (int i=0; i<callbacks.length; i++) {
				Callback cb=callbacks[i];
				if (cb instanceof NameCallback) {
					NameCallback ncb = (NameCallback) cb;
					log.info("setting name of NameCallback to ["+getUsername()+"]");
					ncb.setName(getUsername());
				} else if (cb instanceof PasswordCallback) {
					PasswordCallback pcb = (PasswordCallback) cb;
					log.info("setting password of PasswordCallback");
					String password=getPassword();
					if (password==null) {
						pcb.setPassword(null);
					} else {
						pcb.setPassword(password.toCharArray());
					}
				} else {
					log.fine("ignoring callback of type ["+cb.getClass().getName()+"]");
				}
//				if (cb instanceof ChoiceCallback) {
//					ChoiceCallback ccb = (ChoiceCallback) cb;
//					log.info("ChoiceCallback: "+ccb.getPrompt());
//				}

			}
		}
	}

	/**
	 * return a loginContext, obtained by logging in using the obtained credentials
	 */
	public LoginContext getLoginContext() throws LoginException {
		String loginConfig="ClientContainer";
		getCredentialsFromAlias();
		log.fine("logging in using context["+loginConfig+"]");
		LoginContext lc = new LoginContext(loginConfig, new LoginCallbackHandler());
		lc.login();
		return lc;
	}

//	private class PasswordGetter implements PrivilegedAction {
//
//		Subject s;
//
//		public PasswordGetter(Subject s) {
//			this.s=s;
//		}
//
//		public Object run() {
//			//log.debug("Subject: "+s.toString());
//			//			log.info("Subject: "+ToStringBuilder.reflectionToString(s));
//			//			showSet(s.getPrincipals(),"principals");
//			//			showSet(s.getPublicCredentials(),"PublicCredentials");
//						Set pcs = s.getPrivateCredentials();
//						return pcs.toArray()[0];
//			//			log.info("Pwcred:"+pwcred.toString()+" "+ToStringBuilder.reflectionToString(pwcred));
//		};
//
//	}

//	private void showSet(Set set, String string) {
//		String msg=string+"("+set.getClass().getName()+"): ";
//		for(Iterator it=set.iterator();it.hasNext();){
//			Object item=it.next();
//			msg+=item.getClass().getName()+" "+item+"; ";
//		}
//		log.debug(msg);
//	}

	/*
	 * Dummy principal, to populate subject, to have at least a principal.
	 */
	private class FrankPrincipal implements Principal{
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
