/*
 * $Log: SoapGenericProvider.java,v $
 * Revision 1.1  2005-10-18 08:14:48  europe\L190409
 * created separate soap-package
 *
 * Revision 1.2  2005/07/05 13:29:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SecurityHandlers
 *
 * Revision 1.1  2005/04/26 09:28:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SoapGenericProvider
 *
 */
package nl.nn.adapterframework.soap;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

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
 * @version Id
 * @author Gerrit van Brakel
 */
public class SoapGenericProvider implements Provider {
	public static final String version = "$RCSfile: SoapGenericProvider.java,v $ $Revision: 1.1 $ $Date: 2005-10-18 08:14:48 $";
	protected Logger log=Logger.getLogger(this.getClass());
	
	private final String TARGET_OBJECT_URI_KEY = "TargetObjectNamespaceURI";

	private ServiceDispatcher sd=null;
	private SoapWrapper soapWrapper=null;

	public void locate(DeploymentDescriptor dd, Envelope env, Call call, String methodName, String targetObjectURI, SOAPContext reqContext)
		throws SOAPException {
		if (log.isDebugEnabled()){
			log.debug("Locate: dd=["+dd+"]+ targetObjectURI=[" +targetObjectURI+"]");
		}
		if (sd==null) {
			sd= ServiceDispatcher.getInstance();
		}
		if (soapWrapper==null) {
			try {
				soapWrapper = SoapWrapper.getInstance();
			} catch (ConfigurationException e) {
				throw new SOAPException(Constants.FAULT_CODE_SERVER, "cannot instantiate SoapWrapper");
			}
		}
		
		if (!sd.isRegisteredServiceListener(targetObjectURI)){
			throw new SOAPException(Constants.FAULT_CODE_SERVER, "["+targetObjectURI+"] is not a registered receiver");
		}
		reqContext.setProperty(TARGET_OBJECT_URI_KEY, targetObjectURI);
	}
	
	public void invoke(SOAPContext reqContext, SOAPContext resContext) throws SOAPException {

		 try {
		 	String targetObjectURI = (String) reqContext.getProperty(TARGET_OBJECT_URI_KEY);
			if (log.isDebugEnabled()){
				log.debug("Invoking service for targetObjectURI=[" +targetObjectURI+"]");
			}
			String message=soapWrapper.getBody(reqContext.getBodyPart(0).getContent().toString());
			HttpServletRequest httpRequest=(HttpServletRequest) reqContext.getProperty(Constants.BAG_HTTPSERVLETREQUEST);
			ISecurityHandler securityHandler = new HttpSecurityHandler(httpRequest);
			HashMap messageContext= new HashMap();
			messageContext.put(PipeLineSession.securityHandlerKey, securityHandler);
			String result=sd.dispatchRequestWithExceptions(targetObjectURI, null, message, messageContext);
			resContext.setRootPart( soapWrapper.putInEnvelope(result,null), Constants.HEADERVAL_CONTENT_TYPE_UTF8);
				
		 }
		 catch( Exception e ) {
		 	log.warn("GenericSoapProvider caught exception:",e);
			if ( e instanceof SOAPException ) {
				throw (SOAPException ) e;
			} 
			throw new SOAPException( Constants.FAULT_CODE_SERVER, e.toString() );
		 }
	}
	
}

