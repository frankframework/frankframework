/*
 * $Log: CredentialFactory.java,v $
 * Revision 1.1  2005-12-19 16:38:07  europe\L190409
 * introduction for credential factory, to enable authentication using authentication-alias
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Provides user-id and password from the WebSphere authentication-alias repository.
 * A default username and password can be set, too.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.2
 * @version Id
 */
public class CredentialFactory implements CallbackHandler {
	protected Logger log = Logger.getLogger(this.getClass());
	
	private String alias;
	private String username;
	private String password;
	private boolean gotCredentials=false;
	
	public CredentialFactory(String alias, String defaultUsername, String defaultPassword) {
		super();
		setAlias(alias);
		setUsername(defaultUsername);
		setPassword(defaultPassword);
	}
	
	public void invokeStringSetter(Object o, String name, String value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class argsTypes[] = { name.getClass() };
		Method setterMtd = o.getClass().getMethod(name, argsTypes ); 
		Object args[] = { value };
		setterMtd.invoke(o,args);
	}
	public Object invokeGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Method getterMtd = o.getClass().getMethod(name, null ); 
		return getterMtd.invoke(o,null);
	}
	public String invokeStringGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return (String)invokeGetter(o,name);
	}
	public String invokeCharArrayGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		char arr[] = (char[])invokeGetter(o,name);
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
//			if (cb instanceof WSManagedConnectionFactoryCallback) {
//				WSManagedConnectionFactoryCallback cb_1 = (WSManagedConnectionFactoryCallback) cb;
//			}
//			if (cb instanceof WSAuthDataAliasCallback) {
//				WSAuthDataAliasCallback cb_1 = (WSAuthDataAliasCallback) cb;
//			}
			Class cbc = cb.getClass();
//			Class itf[] = cbc.getInterfaces();
//			for (int j=0; j<itf.length; j++) {
//				log.info("interface "+j+": "+itf[j].getName());
//			}
			if (cbc.getName().endsWith("AuthDataAliasCallback")) {
				try {
					log.info("setting alias of AuthDataAliasCallback to alias ["+getAlias()+"]");
					invokeStringSetter(cb,"setAlias",getAlias());
				} catch (Exception e) {
					log.warn("exception setting alias ["+getAlias()+"]", e);
				}
			}
			if (cb instanceof NameCallback) {
				NameCallback ncb = (NameCallback) cb;
				log.info("setting name of NameCallback to alias ["+getAlias()+"]");
				ncb.setName(getAlias());
			}
//			if (cb instanceof ChoiceCallback) {
//				ChoiceCallback ccb = (ChoiceCallback) cb;
//				log.info("ChoiceCallback: "+ccb.getPrompt());
//			}
				
		}
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

				setUsername(invokeStringGetter(pwcred,"getUserName"));
				setPassword(invokeCharArrayGetter(pwcred,"getPassword"));
				gotCredentials=true;
			} catch (Exception e) {
				log.error("exception obtaining credentials",e);
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
