/*
   Copyright 2013, 2017-2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.http;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;

/**
 * Sender that sends a message via a WebService.
 * 
 * 
 * @author Gerrit van Brakel
 * @author Niels Meijer
 * @since 7.0
 * @version 2.0
 */
public class WebServiceSender extends HttpSender {

	private boolean soap = true;
	private String soapAction = "";
	private String soapActionParam = "soapAction";
	private String encodingStyle=null;
	private String serviceNamespace=null;
	private String serviceNamespaceParam="serviceNamespace";
	private String namespaceDefs = null; 
	private boolean throwApplicationFaults=true;
	private String wssAuthAlias;
	private String wssUserName;
	private String wssPassword;
	private boolean wssPasswordDigest = true;

	private SoapWrapper soapWrapper;
	private CredentialFactory wsscf=null;
	private Parameter soapActionParameter;
	private Parameter serviceNamespaceURIParameter;

	public WebServiceSender() {
		super();
		setMethodType("POST");
	}

	public String getLogPrefix() {
		return "WebServiceSender ["+getName()+"] to ["+getPhysicalDestinationName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (isSoap()) {
			//ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			//String msg = getLogPrefix()+"the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			//configWarnings.add(log, msg);
		}
		soapWrapper = SoapWrapper.getInstance();

		if (paramList!=null && StringUtils.isNotEmpty(getSoapActionParam())) {
			soapActionParameter=paramList.findParameter(getSoapActionParam());
			if(soapActionParameter != null)
				addParameterToSkip(soapActionParameter.getName());
			serviceNamespaceURIParameter=paramList.findParameter(getServiceNamespaceParam());
			if(serviceNamespaceURIParameter != null)
				addParameterToSkip(serviceNamespaceURIParameter.getName());
		}

		if (StringUtils.isNotEmpty(getWssAuthAlias()) || 
			StringUtils.isNotEmpty(getWssUserName())) {
				wsscf = new CredentialFactory(getWssAuthAlias(), getWssUserName(), getWssPassword());
			log.debug(getLogPrefix()+"created CredentialFactory for username=["+wsscf.getUsername()+"]");
		}
	}

	@Override
	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters) throws SenderException {
		setContentType("text/xml; charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING);
		return super.getMethod(uri, message, parameters);
	}

	@Override
	protected HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters, IPipeLineSession session) throws SenderException {

		String serviceNamespaceURI;
		if (serviceNamespaceURIParameter!=null) {
			serviceNamespaceURI=parameters.getParameterValue(getServiceNamespaceParam()).asStringValue(getServiceNamespace());
		} else {
			serviceNamespaceURI=getServiceNamespace();
		}

		String soapActionURI;
		if (soapActionParameter!=null) {
			soapActionURI=parameters.getParameterValue(getSoapActionParam()).asStringValue(getSoapAction());
		} else {
			soapActionURI=getSoapAction();
		}

		String soapmsg;
		if (isSoap()) {
			soapmsg = soapWrapper.putInEnvelope(message, getEncodingStyle(), serviceNamespaceURI, null, getNamespaceDefs());
		} else {
			soapmsg = message;
		}

		if (wsscf!=null) {
			soapmsg = soapWrapper.signMessage(soapmsg, wsscf.getUsername(), wsscf.getPassword(), isWssPasswordDigest());
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"SOAPMSG [" + soapmsg + "]");

		HttpRequestBase method = super.getMethod(uri, soapmsg, parameters, session);
		log.debug(getLogPrefix()+"setting SOAPAction header ["+soapActionURI+"]");
		method.setHeader("SOAPAction", soapActionURI);
		return method;
	}

	@Override
	protected String extractResult(HttpResponseHandler responseHandler, ParameterResolutionContext prc) throws SenderException, IOException {
		String httpResult = null;
		try {
			httpResult = super.extractResult(responseHandler, prc);
		} catch (SenderException e) {
			soapWrapper.checkForSoapFault(getResponseBodyAsString(responseHandler), e);
			throw e;
		}

		if (isThrowApplicationFaults()) {
			soapWrapper.checkForSoapFault(httpResult, null);
		}
		try {
			if (isSoap()) {
				return soapWrapper.getBody(httpResult);
			} else {
				return httpResult;
			}
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
	}

	@IbisDoc({"when <code>true</code>, messages sent are put in a soap envelope and the soap envelope is removed from received messages (soap envelope will not be visible to the pipeline)", "<code>true</code>"})
	public void setSoap(boolean b) {
		soap = b;
	}
	public boolean isSoap() {
		return soap;
	}

	/**
	 * @deprecated please use setSoapAction() instead
	 */
	@IbisDoc({"deprecated: please use soapAction instead", ""})
	public void setSoapActionURI(String soapAction) {
		ConfigurationWarnings.getInstance().add(log, getLogPrefix()+" the attribute 'soapActionURI' has been renamed 'soapAction'");
		setSoapAction(soapAction);
	}

	/**
	 * @deprecated please use setSoapActionParam instead
	 */
	@IbisDoc({"deprecated: please use soapActionParam instead", ""})
	public void setSoapActionURIParam(String soapActionParam) {
		ConfigurationWarnings.getInstance().add(log, getLogPrefix()+" the attribute 'soapActionURIParam' has been renamed 'soapActionParam'");
		setSoapActionParam(soapActionParam);
	}

	public String getSoapAction() {
		return soapAction;
	}

	@IbisDoc({"the soapactionuri to be set in the requestheader", ""})
	public void setSoapAction(String soapAction) {
		this.soapAction = soapAction;
	}

	public String getSoapActionParam() {
		return soapActionParam;
	}

	@IbisDoc({"parameter to obtain the soapactionuri", ""})
	public void setSoapActionParam(String soapActionParam) {
		this.soapActionParam = soapActionParam;
	}
	
	@IbisDoc({"deprecated: please use encodingstyle instead", ""})
	public void setEncodingStyleURI(String encodingStyle) {
		ConfigurationWarnings.getInstance().add(log, getLogPrefix()+" the attribute 'encodingStyleURI' has been renamed 'encodingStyle'");
		setEncodingStyle(encodingStyle);
	}

	@IbisDoc({"the encodingstyle to be set in the messageheader", ""})
	public void setEncodingStyle(String encodingStyle) {
		this.encodingStyle = encodingStyle;
	}
	public String getEncodingStyle() {
		return encodingStyle;
	}

	
	@IbisDoc({"controls whether soap faults generated by the application generate an exception, or are treated as 'normal' messages", "true"})
	public void setThrowApplicationFaults(boolean b) {
		throwApplicationFaults = b;
	}
	public boolean isThrowApplicationFaults() {
		return throwApplicationFaults;
	}


	@IbisDoc({"deprecated: please use servicenamespace instead", ""})
	public void setServiceNamespaceURI(String serviceNamespace) {
		ConfigurationWarnings.getInstance().add(log, getLogPrefix()+" the attribute 'serviceNamespaceURI' has been renamed 'serviceNamespace'");
		setServiceNamespace(serviceNamespace);
	}

	@IbisDoc({"the namespace of the message sent. identifies the service to be called. may be overriden by an actual namespace setting in the message to be sent", ""})
	public void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}
	public String getServiceNamespace() {
		return serviceNamespace;
	}

	@IbisDoc({"deprecated: please use servicenamespaceparam instead", ""})
	public void setServiceNamespaceURIParam(String serviceNamespaceParam) {
		ConfigurationWarnings.getInstance().add(log, getLogPrefix()+" the attribute 'serviceNamespaceURIParam' has been renamed 'serviceNamespaceParam'");
		setServiceNamespaceParam(serviceNamespaceParam);
	}

	@IbisDoc({"parameter to obtain the servicenamespace", ""})
	public void setServiceNamespaceParam(String serviceNamespaceParam) {
		this.serviceNamespaceParam = serviceNamespaceParam;
	}
	public String getServiceNamespaceParam() {
		return serviceNamespaceParam;
	}

	@IbisDoc({"namespace defintions to be added in the soap envelope tag. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"", " "})
	public void setWssUserName(String string) {
		wssUserName = string;
	}
	public String getWssUserName() {
		return wssUserName;
	}

	@IbisDoc({"", " "})
	public void setWssPassword(String string) {
		wssPassword = string;
	}
	public String getWssPassword() {
		return wssPassword;
	}

	@IbisDoc({"alias used to obtain credentials for authentication to web services security", ""})
	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}
	public String getWssAuthAlias() {
		return wssAuthAlias;
	}

	@IbisDoc({"when true, the password is sent digested. otherwise it is sent in clear text", "true"})
	public void setWssPasswordDigest(boolean b) {
		wssPasswordDigest = b;
	}
	public boolean isWssPasswordDigest() {
		return wssPasswordDigest;
	}
}