/*
 * $Log: JNDIBase.java,v $
 * Revision 1.3  2004-03-23 17:59:02  L190409
 * cosmetic changes
 * added setJmsRealm
 *
 */
package nl.nn.adapterframework.jms;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 * <br/>
 * <p>$Id: JNDIBase.java,v 1.3 2004-03-23 17:59:02 L190409 Exp $</p>
 * @author Johan Verrips IOS
 */
public class JNDIBase {
	public static final String version="$Id: JNDIBase.java,v 1.3 2004-03-23 17:59:02 L190409 Exp $";

    // JNDI
    private String providerURL = null;
    private String initialContextFactoryName = null;
    private Context context = null;
    private String authentication = null;
    private String credentials = null;
    private String urlPkgPrefixes = null;
    private String securityProtocol = null;

  protected Logger log = Logger.getLogger(this.getClass());

    public void closeContext() throws javax.naming.NamingException {
        if (null != context) {
            context.close();
            context = null;
        }
    }
    public String getAuthentication() {
        return authentication;
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
    public Context getContext() throws javax.naming.NamingException {

        if (null == context) {
            if (getInitialContextFactoryName() != null) {
                Hashtable JMSEnv = new Hashtable();

                JMSEnv.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactoryName);
                if (providerURL != null)
                    JMSEnv.put(Context.PROVIDER_URL, providerURL);
                if (authentication != null)
                    JMSEnv.put(Context.SECURITY_AUTHENTICATION, authentication);
                if (credentials != null)
                    JMSEnv.put(Context.SECURITY_CREDENTIALS, credentials);
                if (urlPkgPrefixes != null)
                    JMSEnv.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
                if (securityProtocol != null)
                    JMSEnv.put(Context.SECURITY_PROTOCOL, securityProtocol);
                context = (Context) new InitialContext(JMSEnv);


            } else {
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

}
