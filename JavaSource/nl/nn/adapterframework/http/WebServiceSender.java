/*
 * $Log: WebServiceSender.java,v $
 * Revision 1.25  2010-02-11 12:59:28  m168309
 * fixed typo in javadoc
 *
 * Revision 1.24  2009/12/24 08:32:31  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Prevent warning "Going to buffer response body of large or unknown size. Using getResponseAsStream instead is recommended"
 *
 * Revision 1.23  2009/08/26 11:47:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * upgrade to HttpClient 3.0.1 - including idle connection cleanup
 *
 * Revision 1.22  2009/02/10 10:58:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * removed configuration warning when attribute SoapActionURI is empty
 *
 * Revision 1.21  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.20  2008/10/31 15:02:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * WS-Security made possible
 *
 * Revision 1.19  2008/08/12 15:35:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * warn about empty SoapActionURI
 *
 * Revision 1.18  2008/05/21 08:43:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * content-type configurable
 *
 * Revision 1.17  2008/04/28 08:02:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Encoding fix for WebServiceSender ("text/xml" -> "text/xml;charset=UTF-8") to make Encoding scenarios in Ibis4Test work.
 *
 * Revision 1.16  2006/06/14 09:41:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.15  2005/12/28 08:40:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.14  2005/10/18 08:23:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created separate soap-package
 *
 * Revision 1.13  2005/07/05 12:57:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added serviceNamespaceURI-attribute
 *
 * Revision 1.12  2005/04/26 09:25:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed soapConverter to soapWrapper
 *
 * Revision 1.11  2005/02/24 12:15:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SOAP conversion coding to SOAP-wrapper
 *
 * Revision 1.10  2004/10/14 15:34:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included ParameterValueList in getMethod(), 
 * in order to override HttpSenders' getMethod()
 *
 * Revision 1.9  2004/10/12 15:10:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute throwApplicationFaults
 *
 * Revision 1.8  2004/09/09 14:48:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.7  2004/09/08 14:17:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of SOAP faults
 *
 * Revision 1.6  2004/09/02 13:25:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved fault-handling
 * added handling of encodingStyleUri
 *
 * Revision 1.5  2004/09/01 12:24:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved fault handling
 *
 * Revision 1.4  2004/08/31 15:52:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new version based on HttpSender
 *
 * Revision 1.3  2004/08/23 13:08:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/08/17 15:22:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version using Axis
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Sender that sends a message via a WebService.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.HttpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setContentType(String) contentType}</td><td>conent-type of the request, only for POST methods</td><td>text/xml; charset=UTF-8</td></tr>
 * <tr><td>{@link #setSoapActionURI(String) soapActionURI}</td><td>the SOAPActionUri to be set in the requestheader</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceNamespaceURI(String) serviceNamespaceURI}</td><td>the namespace of the message sent. Identifies the service to be called. May be overriden by an actual namespace setting in the message to be sent</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThrowApplicationFaults(boolean) throwApplicationFaults}</td><td>controls whether soap faults generated by the application generate an exception, or are treated as 'normal' messages</td><td>true</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout ih ms of obtaining a connection/result. 0 means no timeout</td><td>60000</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>the maximum number of concurrent connections</td><td>2</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyRealm(String) proxyRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>&nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setFollowRedirects(boolean) followRedirects}</td><td>when true, a redirect request will be honoured, e.g. to switch to https</td><td>true</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setStaleChecking(boolean) staleChecking}</td><td>controls whether connections checked to be stale, i.e. appear open, but are not.</td><td>true</td></tr>
 * <tr><td>{@link #setWssAuthAlias(String) wssAuthAlias}</td><td>alias used to obtain credentials for authentication to Web Services Security</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWssUserName(String) wssUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWssPassword(String) wssPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 * @since 4.2c
 */
public class WebServiceSender extends HttpSender {
	public static final String version = "$RCSfile: WebServiceSender.java,v $ $Revision: 1.25 $ $Date: 2010-02-11 12:59:28 $";
	
	private String soapActionURI = "";
	private String encodingStyleURI=null;
	private String serviceNamespaceURI=null;
	private boolean throwApplicationFaults=true;
	private String wssAuthAlias;
	private String wssUserName;
	private String wssPassword;

	private SoapWrapper soapWrapper;
	private CredentialFactory wsscf=null;
	
	public String getLogPrefix() {
		return "WebServiceSender ["+getName()+"] to ["+getPhysicalDestinationName()+"] ";
	}
 
 	public WebServiceSender() {
 		super();
 		setMethodType("POST");
		setContentType("text/xml;charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING);
 	}
 

	public void configure() throws ConfigurationException {
		super.configure();
		soapWrapper=SoapWrapper.getInstance();
/*
		if (StringUtils.isEmpty(getSoapActionURI())) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			String msg = getLogPrefix()+"no soapActionURI found, please check if this is appropriate";
			configWarnings.add(log, msg);
		}
*/
		if (StringUtils.isNotEmpty(getWssAuthAlias()) || 
			StringUtils.isNotEmpty(getWssUserName())) {
				wsscf = new CredentialFactory(getWssAuthAlias(), getWssUserName(), getWssPassword());
			log.debug(getLogPrefix()+"created CredentialFactor for  username=["+wsscf.getUsername()+"]");
		}
	}

	protected HttpMethod getMethod(String message, ParameterValueList parameters) throws SenderException {
		
		String soapmsg= soapWrapper.putInEnvelope(message, getEncodingStyleURI(),getServiceNamespaceURI());

		if (wsscf!=null) {
			soapmsg = soapWrapper.signMessage(soapmsg,wsscf.getUsername(),wsscf.getPassword());
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"SOAPMSG [" + soapmsg + "]");

		HttpMethod method = super.getMethod(soapmsg,parameters);
		log.debug("setting Content-Type and SOAPAction header ["+getSoapActionURI()+"]");
		method.addRequestHeader("SOAPAction",getSoapActionURI());
		return method;
	}


	public String extractResult(HttpMethod httpmethod) throws SenderException, IOException {
		String httpResult;
		try {
			httpResult = super.extractResult(httpmethod);
		} catch (SenderException e) {
			soapWrapper.checkForSoapFault(getResponseBodyAsString(httpmethod), e);
			throw e;
		}
		
		if (isThrowApplicationFaults()) {
			soapWrapper.checkForSoapFault(httpResult, null);
		}
		try {
			String result = soapWrapper.getBody(httpResult);
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+"retrieved result ["+result+"]");
			}
			return (result);
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return super.sendMessage(correlationID,message);
	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


	public void setSoapActionURI(String soapActionURI) {
		this.soapActionURI = soapActionURI;
	}
	public String getSoapActionURI() {
		return soapActionURI;
	}


	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
	}
	public String getEncodingStyleURI() {
		return encodingStyleURI;
	}

	
	public void setThrowApplicationFaults(boolean b) {
		throwApplicationFaults = b;
	}
	public boolean isThrowApplicationFaults() {
		return throwApplicationFaults;
	}


	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}
	public String getServiceNamespaceURI() {
		return serviceNamespaceURI;
	}


	public void setWssUserName(String string) {
		wssUserName = string;
	}
	public String getWssUserName() {
		return wssUserName;
	}

	public void setWssPassword(String string) {
		wssPassword = string;
	}
	public String getWssPassword() {
		return wssPassword;
	}

	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}
	public String getWssAuthAlias() {
		return wssAuthAlias;
	}

}