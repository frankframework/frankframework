/*
 * $Log: SoapGenericProvider.java,v $
 * Revision 1.1  2005-04-26 09:28:25  L190409
 * introduction of SoapGenericProvider
 *
 */
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;
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
	public static final String version = "$Id: SoapGenericProvider.java,v 1.1 2005-04-26 09:28:25 L190409 Exp $";
	protected Logger log=Logger.getLogger(this.getClass());
	
	private final String TARGET_OBJECT_URI_KEY = "TargetObjectNamespaceURI";

	private ServiceDispatcher sd=null;
	private SoapWrapper soapWrapper=null;

	public void locate(DeploymentDescriptor dd, Envelope env, Call call, String methodName, String targetObjectURI, SOAPContext reqContext)
		throws SOAPException {
		if (log.isDebugEnabled()){
			log.debug("Locate: dd=["+dd+"]+ targetObjectURI=[" +targetObjectURI+"]");		}
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
			String result=sd.dispatchRequest(targetObjectURI, message);
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

