/*
 * $Log: RestListener.java,v $
 * Revision 1.1  2011-05-19 15:11:27  L190409
 * first version of Rest-provider support
 *
 */
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class RestListener extends PushingListenerAdapter {

	private String uriPattern;
	private String method;
	private String etagSessionKey;
	private String contentTypeSessionKey;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		RestServiceDispatcher.getInstance().registerServiceClient(getName(), this, getUriPattern(), method, etagSessionKey, contentTypeSessionKey);
	}

	public String getUriPattern() {
		return uriPattern;
	}
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}


	public String getEtagSessionKey() {
		return etagSessionKey;
	}
	public void setEtagSessionKey(String etagSessionKey) {
		this.etagSessionKey = etagSessionKey;
	}

	public String getContentTypeSessionKey() {
		return contentTypeSessionKey;
	}
	public void setContentTypeSessionKey(String contentTypeSessionKey) {
		this.contentTypeSessionKey = contentTypeSessionKey;
	}

}
