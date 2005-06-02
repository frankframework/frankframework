/*
 * $Log: ServiceDispatcher_ServiceProxy.java,v $
 * Revision 1.10  2005-06-02 12:08:34  europe\L190409
 * cosmetic changes
 *
 * Revision 1.9  2005/05/19 12:32:09  Johan Verrips <johan.verrips@ibissource.org>
 * Updated VersionID
 *
 * Revision 1.8  2005/05/19 12:31:24  Johan Verrips <johan.verrips@ibissource.org>
 * Updated VersionID
 *
 * Revision 1.7  2005/05/19 12:30:18  Johan Verrips <johan.verrips@ibissource.org>
 * Updated VersionID
 *
 */
package nl.nn.adapterframework.receivers;

import org.apache.soap.Constants;
import org.apache.soap.Fault;
import org.apache.soap.SOAPException;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.Response;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * WebService proxy class that can be used in applications that need to communciate with
 * the IBIS Adapterframework via a webservice.
 * @version Id
 * @Author Johan Verrips
 */

public class ServiceDispatcher_ServiceProxy {
	public static final String version = "$RCSfile: ServiceDispatcher_ServiceProxy.java,v $  $Revision: 1.10 $ $Date: 2005-06-02 12:08:34 $";

	private URL url = null;
	
	public ServiceDispatcher_ServiceProxy() throws MalformedURLException {
	}
	
  /**
	 * Dispatch a request.
	 * @param meth1_inType1 ServiceName
	 * @param meth1_inType2 Message
	 * @return String the result
	 * @throws SOAPException
	 */
	public String dispatchRequest(String servicename, String message) throws SOAPException {
		Call call = new Call();
		call.setTargetObjectURI("urn:service-dispatcher");
		call.setEncodingStyleURI("http://schemas.xmlsoap.org/soap/encoding/");
		String SOAPActionURI = "urn:service-dispatcher";
 
	    if (url == null) {
	      throw new SOAPException(Constants.FAULT_CODE_CLIENT,
	      "A URL must be specified via " +
	      "ServiceDispatcher_ServiceProxy.setEndPoint(URL).");
	    }

	    call.setMethodName("dispatchRequest");
	    Vector params = new Vector();
		params.addElement(new Parameter("meth1_inType1", String.class, servicename, null));
		params.addElement(new Parameter("meth1_inType2", String.class, message, null));
	    call.setParams(params);
	    Response resp = call.invoke(url, SOAPActionURI);

	    // Check the response.
	    if (resp.generatedFault()) {
	      Fault fault = resp.getFault();
	
	      throw new SOAPException(fault.getFaultCode(), fault.getFaultString());
	    } else {
	      Parameter retValue = resp.getReturnValue();
	      return (String)retValue.getValue();
	    }
	}
	
  /**
   * Dispatch a request.
   * @param meth1_inType1 ServiceName
   * @param meth1_inType2 CorrelationID
   * @param meth1_inType3 Message
   * @return String the result
   * @throws SOAPException
   */
	public  String dispatchRequest(String servicename, String correlationId, String message) throws SOAPException {
		Call call = new Call();
		call.setTargetObjectURI("urn:service-dispatcher");
		call.setEncodingStyleURI("http://schemas.xmlsoap.org/soap/encoding/");
		String SOAPActionURI = "urn:service-dispatcher";

		if (url == null) {
			throw new SOAPException(Constants.FAULT_CODE_CLIENT,
			"A URL must be specified via " +
			"ServiceDispatcher_ServiceProxy.setEndPoint(URL).");
		}

		call.setMethodName("dispatchRequest");
		Vector params = new Vector();
		params.addElement(new Parameter("meth1_inType1", String.class, servicename, null));
		params.addElement(new Parameter("meth1_inType2", String.class, correlationId, null));
		params.addElement(new Parameter("meth1_inType3", String.class, message, null));
		call.setParams(params);
		Response resp = call.invoke(url, SOAPActionURI);

		// Check the response.
		if (resp.generatedFault()) {
			Fault fault = resp.getFault();
	
			throw new SOAPException(fault.getFaultCode(), fault.getFaultString());
		} else {
			Parameter retValue = resp.getReturnValue();
			return (String)retValue.getValue();
		}
	}
   
	public synchronized URL getEndPoint()   {
		return url;
	}
	
	public synchronized void setEndPoint(URL url) {
		this.url = url;
	}
	
}
