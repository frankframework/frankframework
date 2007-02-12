/*
 * $Log: JNDIBase.java,v $
 * Revision 1.9  2007-02-12 13:58:11  europe\L190409
 * Logger from LogUtil
 *
 * Revision 1.8  2006/03/15 14:08:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.7  2006/03/15 10:34:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.6  2006/03/15 10:33:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added principal-attribute
 * corrected environment handling
 *
 * Revision 1.5  2005/01/13 08:09:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modifications for LDAP-pipe
 *
 * Revision 1.4  2004/03/26 10:42:55  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:59:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 * added setJmsRealm
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 * <br/>
 * @version Id
 * @author Johan Verrips IOS
 */
public class JNDIBase {
	public static final String version = "$RCSfile: JNDIBase.java,v $ $Revision: 1.9 $ $Date: 2007-02-12 13:58:11 $";
	protected Logger log = LogUtil.getLogger(this);

    // JNDI
    private String providerURL = null;
    private String initialContextFactoryName = null;
    private Context context = null;
    private String authentication = null;
	private String principal = null;
    private String credentials = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;




    public void closeContext() throws javax.naming.NamingException {
        if (null != context) {
        	log.debug("closing JNDI-context");
            context.close();
            context = null;
        }
    }
    
	protected Hashtable getJndiEnv() {
		Hashtable jndiEnv = new Hashtable();

		if (getInitialContextFactoryName() != null)
			jndiEnv.put(Context.INITIAL_CONTEXT_FACTORY, getInitialContextFactoryName());
		if (getProviderURL() != null)
			jndiEnv.put(Context.PROVIDER_URL, getProviderURL());
		if (getAuthentication() != null)
			jndiEnv.put(Context.SECURITY_AUTHENTICATION, getAuthentication());
		if (getPrincipal() != null)
			jndiEnv.put(Context.SECURITY_PRINCIPAL, getPrincipal());
		if (getCredentials() != null)
			jndiEnv.put(Context.SECURITY_CREDENTIALS, getCredentials());
		if (getUrlPkgPrefixes() != null)
			jndiEnv.put(Context.URL_PKG_PREFIXES, getUrlPkgPrefixes());
		if (getSecurityProtocol() != null)
			jndiEnv.put(Context.SECURITY_PROTOCOL, getSecurityProtocol());
		return jndiEnv;
	}
	
    /**
     *  Gets the Context<br/>
     *  When InitialContextFactory and ProviderURL are set, these are used
     *  to get the <code>Context</code>. Otherwise the the InitialContext is
     *  retrieved without parameters.<br/>
     *  <b>Notice:</b> you can set the parameters on the commandline with <br/>
     *  java -Djava.naming.factory.initial= xxx -Djava.naming.provider.url=xxx
     * <br/><br/>
     *
     * @return                                   The context value
     * @exception  javax.naming.NamingException  Description of the Exception
     */
    public Context getContext() throws NamingException {

        if (null == context) {
        	Hashtable jndiEnv = getJndiEnv();
        	if (jndiEnv.size()>0) {
				log.debug("creating initial JNDI-context using specified environment");
                context = (Context) new InitialContext(jndiEnv);
            } else {
				log.debug("creating initial JNDI-context");
                context = (Context) new InitialContext();
            }
        }
        return context;
    }
    
    public String getCredentials() {
        return credentials;
    }
    /**
     *  Gets the initialContextFactoryName
     *
     * @return    The initialContextFactoryName value
     */
    public String getInitialContextFactoryName() {
        return initialContextFactoryName;
    }
    /**
     *  Gets the providerURL
     *
     * @return    The providerURL value
     */
    public String getProviderURL() {
        return providerURL;
    }
    public String getSecurityProtocol() {
        return securityProtocol;
    }
    public java.lang.String getUrlPkgPrefixes() {
        return urlPkgPrefixes;
    }
    public void setAuthentication(java.lang.String newAuthentication) {
        authentication = newAuthentication;
    }
    public void setCredentials(java.lang.String newCredentials) {
        credentials = newCredentials;
    }
    /**
     *  Sets the initialContextFactoryName
     *
     * @param  value  The new initialContextFactoryName value
     */
    public void setInitialContextFactoryName(String value) {
        initialContextFactoryName = value;
    }
    /**
     *  Sets the providerURL
     *
     * @param  value  The new providerURL value
     */
    public void setProviderURL(String value) {
        providerURL = value;
    }
    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }
    /**
     * Setter for <code>Context.URL_PKG_PREFIXES</code><br/>
     * Creation date: (03-04-2003 8:50:36)
     * @param newUrlPkgPrefixes java.lang.String
     */
    public void setUrlPkgPrefixes(java.lang.String newUrlPkgPrefixes) {
        urlPkgPrefixes = newUrlPkgPrefixes;
    }
    public String toString() {
        ToStringBuilder ts = new ToStringBuilder(this);
        ts.append("context", context);
        ts.append("authentication", authentication);
        ts.append("credentials", credentials);
        ts.append("providerURL", providerURL);
        ts.append("urlPkgPrefixes", urlPkgPrefixes);
        ts.append("securityProtocol", securityProtocol);
        ts.append("initialContextFactoryName", initialContextFactoryName);

        return ts.toString();

    }

	/**
	 * loads JNDI (and other) properties from a JmsRealm
	 * @see JmsRealm
	 */ 
	public void setJmsRealm(String jmsRealmName){
		JmsRealm.copyRealm(this,jmsRealmName);
	}

	public String getAuthentication() {
		return authentication;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String string) {
		principal = string;
	}

}
