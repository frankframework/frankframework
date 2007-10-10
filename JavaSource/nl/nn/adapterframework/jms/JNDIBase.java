/*
 * $Log: JNDIBase.java,v $
 * Revision 1.13  2007-10-10 08:24:25  europe\L190409
 * added jmsRealmName, to be able to report it
 *
 * Revision 1.12  2007/07/10 07:20:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add some checks
 *
 * Revision 1.11  2007/05/08 16:27:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix username/password
 *
 * Revision 1.10  2007/05/08 16:07:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add jndiAuthAlias attribute
 *
 * Revision 1.9  2007/02/12 13:58:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jms.JNDIBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProviderURL(String) providerURL}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitialContextFactoryName(String) initialContextFactoryName}</td><td>class to use as initial context factory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthentication(String) authentication}</td><td>maps to the field Context.SECURITY_AUTHENTICATION</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrincipal(String) principal}</td><td>username to connect to context, maps to Context.SECURITY_PRINCIPAL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCredentials(String) credentials}</td><td>username to connect to context, maps to Context.SECURITY_CREDENTIALS</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJndiAuthAlias(String) jndiAuthAlias}</td><td>Authentication alias, may be used to override principal and credential-settings</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrlPkgPrefixes(String) urlPkgPrefixes}</td><td>maps to the field Context.URL_PKG_PREFIXES</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSecurityProtocol(String) securityProtocol}</td><td>maps to the field Context.SECURITY_PROTOCOL</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <br/>
 * @version Id
 * @author Johan Verrips IOS
 */
public class JNDIBase {
	public static final String version = "$RCSfile: JNDIBase.java,v $ $Revision: 1.13 $ $Date: 2007-10-10 08:24:25 $";
	protected Logger log = LogUtil.getLogger(this);

    // JNDI
    private String providerURL = null;
    private String initialContextFactoryName = null;
    private String authentication = null;
	private String principal = null;
    private String credentials = null;
	private String jndiAuthAlias = null;
    private String jmsRealmName = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;

	private Context context = null;



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
		if (getPrincipal() != null || getCredentials() != null || getJndiAuthAlias()!=null) {
			CredentialFactory jndiCf = new CredentialFactory(getJndiAuthAlias(), getPrincipal(), getCredentials());
			if (StringUtils.isNotEmpty(jndiCf.getUsername()))
				jndiEnv.put(Context.SECURITY_PRINCIPAL, jndiCf.getUsername());
			if (StringUtils.isNotEmpty(jndiCf.getPassword()))
				jndiEnv.put(Context.SECURITY_CREDENTIALS, jndiCf.getPassword());
		}
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
	public void setJmsRealm(String jmsRealmName) {
		try {
			JmsRealm.copyRealm(this,jmsRealmName);
            this.jmsRealmName = jmsRealmName;
		} catch (ConfigurationException e) {
			log.warn("cannot copy data from realm",e);
		}
	}
    
    public String getJmsRealName() {
        return this.jmsRealmName;
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

	public void setJndiAuthAlias(String string) {
		jndiAuthAlias = string;
	}
	public String getJndiAuthAlias() {
		return jndiAuthAlias;
	}

}
