package nl.nn.adapterframework.jms;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * Provides all JNDI functions and is meant to act as a base class.
 * <br/>
 * <p>$Id: JNDIBase.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $</p>
 * @author Johan Verrips IOS
 */
public class JNDIBase {
	public static final String version="$Id: JNDIBase.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $";

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
    /**
     * Insert the method's description here.
     * Creation date: (31-03-2003 8:19:59)
     * @return java.lang.String
     */
    public java.lang.String getAuthentication() {
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
    /**
     * Insert the method's description here.
     * Creation date: (31-03-2003 8:19:59)
     * @return java.lang.String
     */
    public java.lang.String getCredentials() {
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
    /**
     * Insert the method's description here.
     * Creation date: (03-04-2003 8:50:36)
     * @return java.lang.String
     */
    public java.lang.String getUrlPkgPrefixes() {
        return urlPkgPrefixes;
    }
    /**
     * Insert the method's description here.
     * Creation date: (31-03-2003 8:19:59)
     * @param newAuthentication java.lang.String
     */
    public void setAuthentication(java.lang.String newAuthentication) {
        authentication = newAuthentication;
    }
    /**
     * Insert the method's description here.
     * Creation date: (31-03-2003 8:19:59)
     * @param newCredentials java.lang.String
     */
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
}
