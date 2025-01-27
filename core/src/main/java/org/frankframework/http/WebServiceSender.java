/*
   Copyright 2013, 2017-2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.doc.Protected;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

/**
 * Sender that sends a message via a WebService.
 *
 * @author Gerrit van Brakel
 * @author Niels Meijer
 * @since 7.0
 * @version 2.0
 */
public class WebServiceSender extends HttpSender {

	private @Getter boolean soap = true;
	private @Getter String soapAction = "";
	private @Getter String soapActionParam = "soapAction";
	private @Getter String encodingStyle=null;
	private @Getter String serviceNamespace=null;
	private @Getter String serviceNamespaceParam="serviceNamespace";
	private @Getter String namespaceDefs = null;
	private @Getter boolean throwApplicationFaults=true;
	private @Getter String wssAuthAlias;
	private @Getter String wssUserName;
	private @Getter String wssPassword;
	private @Getter boolean wssPasswordDigest = true;

	private SoapWrapper soapWrapper;
	private CredentialFactory wsscf=null;
	private IParameter soapActionParameter;
	private IParameter serviceNamespaceURIParameter;

	public WebServiceSender() {
		super();
		setMethodType(HttpMethod.POST);
		setContentType("text/xml");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (isSoap()) {
			//ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			//String msg = "the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			//configWarnings.add(log, msg);
		}
		soapWrapper = SoapWrapper.getInstance();

		if (paramList!=null && StringUtils.isNotEmpty(getSoapActionParam())) {
			soapActionParameter=paramList.findParameter(getSoapActionParam());
			if(soapActionParameter != null) {
				requestOrBodyParamsSet.remove(soapActionParameter.getName());
				headerParamsSet.remove(soapActionParameter.getName());
			}
			serviceNamespaceURIParameter=paramList.findParameter(getServiceNamespaceParam());
			if(serviceNamespaceURIParameter != null) {
				requestOrBodyParamsSet.remove(serviceNamespaceURIParameter.getName());
				headerParamsSet.remove(serviceNamespaceURIParameter.getName());
			}
		}

		if (StringUtils.isNotEmpty(getWssAuthAlias()) || StringUtils.isNotEmpty(getWssUserName())) {
			wsscf = new CredentialFactory(getWssAuthAlias(), getWssUserName(), getWssPassword());
			log.debug("created CredentialFactory for username=[{}]", wsscf::getUsername);
		}
	}

	@Override
	protected HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException {

		String serviceNamespaceURI;
		if (serviceNamespaceURIParameter!=null) {
			serviceNamespaceURI=parameters.get(getServiceNamespaceParam()).asStringValue(getServiceNamespace());
		} else {
			serviceNamespaceURI=getServiceNamespace();
		}

		String soapActionURI;
		if (soapActionParameter!=null) {
			soapActionURI=parameters.get(getSoapActionParam()).asStringValue(getSoapAction());
		} else {
			soapActionURI=getSoapAction();
		}

		Message soapmsg;
		try {
			if (isSoap()) {
				soapmsg = soapWrapper.putInEnvelope(message, getEncodingStyle(), serviceNamespaceURI, null, getNamespaceDefs());
			} else {
				soapmsg = message;
			}
		} catch (IOException e) {
			throw new SenderException("error reading message", e);
		}

		if (wsscf != null) {
			soapmsg = soapWrapper.signMessage(soapmsg, wsscf.getUsername(), wsscf.getPassword(), isWssPasswordDigest());
		}
		log.debug("SOAPMSG [{}]", soapmsg);

		HttpRequestBase method = super.getMethod(uri, soapmsg, parameters, session);
		log.debug("setting SOAPAction header [{}]", soapActionURI);
		method.setHeader("SOAPAction", soapActionURI);
		return method;
	}

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		Message httpResult;
		try {
			httpResult = super.extractResult(responseHandler, session);
			httpResult.preserve();
		} catch (SenderException e) {
			soapWrapper.checkForSoapFault(getResponseBody(responseHandler), e, session);
			throw e;
		}

		if (isThrowApplicationFaults()) {
			soapWrapper.checkForSoapFault(httpResult, null, session);
		}
		try {
			if (isSoap()) {
				return soapWrapper.getBody(httpResult, false, session, null);
			}
			return httpResult;
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
	}

	/**
	 * when <code>true</code>, messages sent are put in a soap envelope and the soap envelope is removed from received messages (soap envelope will not be visible to the pipeline)
	 * @ff.default true
	 */
	public void setSoap(boolean b) {
		soap = b;
	}

	/**
	 * Hide this property from being set. SOAP should always be POST
	 */
	@Protected
	@Override
	public void setMethodType(HttpMethod method) {
		super.setMethodType(method);
	}

	/** the soapactionuri to be set in the requestheader */
	public void setSoapAction(String soapAction) {
		this.soapAction = soapAction;
	}

	/** parameter to obtain the soapactionuri */
	public void setSoapActionParam(String soapActionParam) {
		this.soapActionParam = soapActionParam;
	}

	/** the encodingstyle to be set in the messageheader */
	public void setEncodingStyle(String encodingStyle) {
		this.encodingStyle = encodingStyle;
	}

	/**
	 * controls whether soap faults generated by the application generate an exception, or are treated as 'normal' messages
	 * @ff.default true
	 */
	public void setThrowApplicationFaults(boolean b) {
		throwApplicationFaults = b;
	}

	/** the namespace of the message sent. identifies the service to be called. may be overriden by an actual namespace setting in the message to be sent */
	public void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}

	/** parameter to obtain the servicenamespace */
	public void setServiceNamespaceParam(String serviceNamespaceParam) {
		this.serviceNamespaceParam = serviceNamespaceParam;
	}

	/** namespace defintions to be added in the soap envelope tag. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/** username used to obtain credentials for authentication to web services security */
	public void setWssUserName(String string) {
		wssUserName = string;
	}

	/** password used to obtain credentials for authentication to web services security */
	public void setWssPassword(String string) {
		wssPassword = string;
	}

	/** alias used to obtain credentials for authentication to web services security */
	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}

	/**
	 * when true, the password is sent digested. otherwise it is sent in clear text
	 * @ff.default true
	 */
	public void setWssPasswordDigest(boolean b) {
		wssPasswordDigest = b;
	}
}
