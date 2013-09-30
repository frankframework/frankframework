/*
   Copyright 2013 Nationale-Nederlanden

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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
	private final String URN_GET_ACTIVATED_CONFIGURATION = "urn:getLoadedConfiguration";

	private ServiceDispatcher sd=null;
	private SoapWrapper soapWrapper=null;

	public void locate(DeploymentDescriptor dd, Envelope env, Call call, String methodName, String targetObjectURI, SOAPContext reqContext)
		throws SOAPException {
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
		if (ConfigurationUtils.stubConfiguration() && soapWrapper==null) {
			try {
				soapWrapper = SoapWrapper.getInstance();
			} catch (ConfigurationException e) {
				throw new SOAPException(Constants.FAULT_CODE_SERVER, "cannot instantiate SoapWrapper");
			}
		}
		if (StringUtils.isEmpty(targetObjectURI)) {
			String msg="no targetObjectURI specified";
			log.warn(msg);
			throw new SOAPException(Constants.FAULT_CODE_SERVER, msg);
		}
		if (!ConfigurationUtils.stubConfiguration() || !targetObjectURI.equalsIgnoreCase(URN_GET_ACTIVATED_CONFIGURATION)) {
			if (!sd.isRegisteredServiceListener(targetObjectURI)){
				String msg="no receiver registered for targetObjectURI ["+targetObjectURI+"]";
				log.warn(msg);
				throw new SOAPException(Constants.FAULT_CODE_SERVER, msg);
			}
		}
		reqContext.setProperty(TARGET_OBJECT_URI_KEY, targetObjectURI);
	}
	
	public void invoke(SOAPContext reqContext, SOAPContext resContext) throws SOAPException {

		 try {
		 	String targetObjectURI = (String) reqContext.getProperty(TARGET_OBJECT_URI_KEY);
			if (log.isDebugEnabled()){
				log.debug("Invoking service for targetObjectURI=[" +targetObjectURI+"]");
			}
			String result=null;
			if (ConfigurationUtils.stubConfiguration() && targetObjectURI.equalsIgnoreCase(URN_GET_ACTIVATED_CONFIGURATION)) {
				result = getActivatedConfiguration(reqContext);
			} else {
				//String message=soapWrapper.getBody(reqContext.getBodyPart(0).getContent().toString());
				String message=reqContext.getBodyPart(0).getContent().toString();
				HttpServletRequest httpRequest=(HttpServletRequest) reqContext.getProperty(Constants.BAG_HTTPSERVLETREQUEST);
				ISecurityHandler securityHandler = new HttpSecurityHandler(httpRequest);
				Map messageContext= new HashMap();
				messageContext.put(IPipeLineSession.securityHandlerKey, securityHandler);
				result=sd.dispatchRequest(targetObjectURI, null, message, messageContext);
				//resContext.setRootPart( soapWrapper.putInEnvelope(result,null), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
			}
			resContext.setRootPart( result, Constants.HEADERVAL_CONTENT_TYPE_UTF8);
				
		 }
		 catch( Exception e ) {
		 	//log.warn("GenericSoapProvider caught exception:",e);
			if ( e instanceof SOAPException ) {
				throw (SOAPException ) e;
			} 
			SOAPException se=new SOAPException( Constants.FAULT_CODE_SERVER, "GenericSoapProvider caught exception");
			se.initCause(e);
			throw se;
		 }
	}

	private String getActivatedConfiguration(SOAPContext context) {
		String result = null;
		HttpServletRequest httpRequest=(HttpServletRequest) context.getProperty(Constants.BAG_HTTPSERVLETREQUEST);
		HttpSession session = httpRequest.getSession();
        String attributeKey=AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
        IbisContext ibisContext = (IbisContext) session.getServletContext().getAttribute(attributeKey);
        if (ibisContext != null) {
        	IbisManager ibisManager = ibisContext.getIbisManager();
			if (ibisManager!=null) {
				Configuration config = ibisManager.getConfiguration();
				if (config!=null) {
				    URL configURL = config.getConfigurationURL();
					try {
						result = ConfigurationUtils.getOriginalConfiguration(configURL);
						result = StringResolver.substVars(result, AppConstants.getInstance());
						result = ConfigurationUtils.getActivatedConfiguration(result);
						result = ConfigurationUtils.getStubbedConfiguration(result);
					} catch (ConfigurationException e) {
					 	log.warn("GenericSoapProvider caught exception:",e);
					}
				}
			}
        } 
        return soapWrapper.putInEnvelope(result, null);
	}
}