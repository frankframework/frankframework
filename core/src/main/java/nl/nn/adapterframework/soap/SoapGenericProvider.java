/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.soap;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.soap.Constants;
import org.apache.soap.Envelope;
import org.apache.soap.SOAPException;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.server.DeploymentDescriptor;
import org.apache.soap.util.Provider;

/**
 * Soap Provider that accepts any message and routes it to a listener with a corresponding TargetObjectNamespacURI.
 * 
 * @author Gerrit van Brakel
 */
public class SoapGenericProvider implements Provider {
	protected Logger log=LogUtil.getLogger(this);
	
	private final String TARGET_OBJECT_URI_KEY = "TargetObjectNamespaceURI";

	private ServiceDispatcher sd=null;
	//private SoapWrapper soapWrapper=null;

	@Override
	public void locate(DeploymentDescriptor dd, Envelope env, Call call, String methodName, String targetObjectURI, SOAPContext reqContext) throws SOAPException {
		if (log.isDebugEnabled()){
			log.debug("Locate: dd=["+dd+"]+ targetObjectURI=[" +targetObjectURI+"]");
			try {
				log.debug("Incoming SOAP message: " + reqContext.getBodyPart(0).getContent().toString());
			} catch (Exception e) {
				log.debug("Could not log SOAP message", e);
			}
		}
		if (sd==null) {
			sd= ServiceDispatcher.getInstance();
		}
		/*if (soapWrapper==null) {
			try {
				soapWrapper = SoapWrapper.getInstance();
			} catch (ConfigurationException e) {
				throw new SOAPException(Constants.FAULT_CODE_SERVER, "cannot instantiate SoapWrapper");
			}
		}*/
		if (StringUtils.isEmpty(targetObjectURI)) {
			String msg="no targetObjectURI specified";
			log.warn(msg);
			throw new SOAPException(Constants.FAULT_CODE_SERVER, msg);
		}
		if (!sd.isRegisteredServiceListener(targetObjectURI)){
			String msg="no receiver registered for targetObjectURI ["+targetObjectURI+"]";
			log.warn(msg);
			throw new SOAPException(Constants.FAULT_CODE_SERVER, msg);
		}
		reqContext.setProperty(TARGET_OBJECT_URI_KEY, targetObjectURI);
	}
	
	@Override
	public void invoke(SOAPContext reqContext, SOAPContext resContext) throws SOAPException {

		try {
			String targetObjectURI = (String) reqContext.getProperty(TARGET_OBJECT_URI_KEY);
			if (log.isDebugEnabled()){
				log.debug("Invoking service for targetObjectURI=[" +targetObjectURI+"]");
			}
			//String message=soapWrapper.getBody(reqContext.getBodyPart(0).getContent().toString());
			String message=reqContext.getBodyPart(0).getContent().toString();
			HttpServletRequest httpRequest=(HttpServletRequest) reqContext.getProperty(Constants.BAG_HTTPSERVLETREQUEST);
			HttpServletResponse httpResponse=(HttpServletResponse) reqContext.getProperty(Constants.BAG_HTTPSERVLETRESPONSE);
			ISecurityHandler securityHandler = new HttpSecurityHandler(httpRequest);
			Map<String,Object> messageContext= new HashMap<>();
			messageContext.put(PipeLineSession.securityHandlerKey, securityHandler);
			messageContext.put("httpListenerServletRequest", httpRequest);
			messageContext.put("httpListenerServletResponse", httpResponse);
			String result=sd.dispatchRequest(targetObjectURI, null, message, messageContext);
			//resContext.setRootPart( soapWrapper.putInEnvelope(result,null), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
			resContext.setRootPart( result, Constants.HEADERVAL_CONTENT_TYPE_UTF8);
				
		} catch (Exception e) {
			//log.warn("GenericSoapProvider caught exception:",e);
			if ( e instanceof SOAPException ) {
				throw (SOAPException ) e;
			} 
			SOAPException se=new SOAPException( Constants.FAULT_CODE_SERVER, "GenericSoapProvider caught exception");
			se.initCause(e);
			throw se;
		}
	}
	
}

