/*
 * $Log: CredentialFactory.java,v $
 * Revision 1.7  2009-09-08 14:35:27  L190409
 * improved signalling of misconfiguration
 *
 * Revision 1.6  2009/08/13 09:19:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made compatible with WAS 6
 *
 * Revision 1.5  2007/02/12 14:09:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.4  2006/03/20 13:51:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow null-password
 *
 * Revision 1.3  2006/03/15 14:04:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getLoginContext(), that performs actual login
 *
 * Revision 1.2  2006/01/19 12:23:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging and javadoc
 * added fallback to resolving userid and password from appConstants
 *
 * Revision 1.1  2005/12/19 16:38:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction for credential factory, to enable authentication using authentication-alias
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

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
 * @version Id
 */
public class CredentialFactory implements CallbackHandler {
	protected Logger log = LogUtil.getLogger(this);
	
	private String alias;
	private String username;
	private String password;
	private boolean gotCredentials=false;
	private boolean useFallback=false;
	
	public CredentialFactory(String alias, String defaultUsername, String defaultPassword) {
		super();
		setAlias(alias);
		setUsername(defaultUsername);
		setPassword(defaultPassword);
	}
	
	public String invokeCharArrayGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		char arr[] = (char[])ClassUtils.invokeGetter(o,name);
		StringBuffer sb=new StringBuffer();
		for (int j=0; j<arr.length;j++) {
			sb.append(arr[j]);
		}
		return sb.toString();
	}

	
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		log.info("Handling callbacks for alias ["+getAlias()+"]");
		for (int i=0; i<callbacks.length; i++) {
			Callback cb=callbacks[i];
//			log.info(i+") "+cb.getClass().getName()+" "+ToStringBuilder.reflectionToString(cb));
			Class cbc = cb.getClass();
			if (cbc.getName().endsWith("MappingPropertiesCallback")) { // Websphere 6
				try {
					Map mappingProperties=new HashMap();
					mappingProperties.put("com.ibm.mapping.authDataAlias", getAlias());
					ClassUtils.invokeSetter(cb,"setProperties",mappingProperties,Map.class);
					log.debug("MappingPropertiesCallback.properties set to entry key [com.ibm.mapping.authDataAlias], value ["+getAlias()+"]");
					continue;
				} catch (Exception e) {
					log.warn("exception setting alias ["+getAlias()+"] on MappingPropertiesCallback", e);
				}
			}
			if (cbc.getName().endsWith("AuthDataAliasCallback")) { // Websphere 5
				try {
					log.info("setting alias of AuthDataAliasCallback to alias ["+getAlias()+"]");
					ClassUtils.invokeSetter(cb,"setAlias",getAlias());
					continue;
				} catch (Exception e) {
					log.warn("exception setting alias ["+getAlias()+"] on AuthDataAliasCallback", e);
				}
			} 
			if (cb instanceof NameCallback) {
				NameCallback ncb = (NameCallback) cb;
				log.info("setting name of NameCallback to alias ["+getAlias()+"]");
				ncb.setName(getAlias());
				continue;
			} 
			log.debug("ignoring callback of type ["+cb.getClass().getName()+"] for alias ["+getAlias()+"]");
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

	private class loginCallbackHandler implements CallbackHandler {

		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			log.info("callback: "+ToStringBuilder.reflectionToString(callbacks));
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
					log.debug("ignoring callback of type ["+cb.getClass().getName()+"] contents ["+ToStringBuilder.reflectionToString(cb)+"]");
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
		log.debug("logging in using context["+loginConfig+"]");
		LoginContext lc = new LoginContext(loginConfig,new loginCallbackHandler());
		lc.login();
		return lc;
	}


	protected void getCredentialsFromAlias() {
		if (!gotCredentials && StringUtils.isNotEmpty(getAlias())) {
			try {
				String loginConfiguration = AppConstants.getInstance().getProperty("PrincipalMapping","DefaultPrincipalMapping");
				LoginContext lc = new LoginContext(loginConfiguration, this);
				lc.login();
				Subject s = lc.getSubject();
				//log.debug("Subject: "+s.toString());
	//			log.info("Subject: "+ToStringBuilder.reflectionToString(s));
	//			showSet(s.getPrincipals(),"principals");
	//			showSet(s.getPublicCredentials(),"PublicCredentials");
				Set pcs = s.getPrivateCredentials();
				Object pwcred = pcs.toArray()[0];
	//			log.info("Pwcred:"+pwcred.toString()+" "+ToStringBuilder.reflectionToString(pwcred)); 

				setUsername(ClassUtils.invokeStringGetter(pwcred,"getUserName"));
				setPassword(invokeCharArrayGetter(pwcred,"getPassword"));
				gotCredentials=true;
			} catch (Exception e) {
				if (!useFallback) {
					NoSuchElementException nsee=new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]"); 
					nsee.initCause(e);
					throw nsee;
				}
				log.error("exception obtaining credentials for alias ["+getAlias()+"]",e);

				String usernameProp="alias."+getAlias()+".username";
				String passwordProp="alias."+getAlias()+".password";
				log.info("trying to solve Authentication Alias from application properties ["+usernameProp+"] and ["+passwordProp+"]");
				setUsername(AppConstants.getInstance().getProperty(usernameProp,username));
				setPassword(AppConstants.getInstance().getProperty(passwordProp,password));
			}
		}
	}

	public String toString() {
		return getClass().getName()+": alias="+getAlias()+"; username="+username;
	}
	
	public void setAlias(String string) {
		alias = string;
		gotCredentials=false;
	}
	public String getAlias() {
		return alias;
	}

	public void setUsername(String string) {
		username = string;
	}
	public String getUsername() {
		getCredentialsFromAlias();
		return username;
	}

	public void setPassword(String string) {
		password = string;
	}
	public String getPassword() {
		getCredentialsFromAlias();
		return password;
	}



}
