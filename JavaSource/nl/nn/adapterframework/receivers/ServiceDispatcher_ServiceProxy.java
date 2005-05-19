/*
 * $Log: ServiceDispatcher_ServiceProxy.java,v $
 * Revision 1.8  2005-05-19 12:31:24  europe\l180564
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
import org.apache.soap.encoding.SOAPMappingRegistry;
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

public class ServiceDispatcher_ServiceProxy
{
 public static final String version="$Source: /tmp/Ibis/Ibis/JavaSource/nl/nn/adapterframework/receivers/ServiceDispatcher_ServiceProxy.java,v $ $Revision: 1.8 $ $Date: 2005-05-19 12:31:24 $";

  private URL url = null;

  public ServiceDispatcher_ServiceProxy() throws MalformedURLException
  {
  }
  /**
	 * Dispatch a request
	 * @param meth1_inType1 ServiceName
	 * @param meth1_inType2 Message
	 * @return String the result
	 * @throws SOAPException
	 */
  public java.lang.String dispatchRequest
    (java.lang.String meth1_inType1,
    java.lang.String meth1_inType2) throws SOAPException
  {
	 Call call = new Call();
	 SOAPMappingRegistry smr = call.getSOAPMappingRegistry();
	 call.setTargetObjectURI("urn:service-dispatcher");
	 call.setEncodingStyleURI("http://schemas.xmlsoap.org/soap/encoding/");
	 String SOAPActionURI = "urn:service-dispatcher";
 

    if (url == null)
    {
      throw new SOAPException(Constants.FAULT_CODE_CLIENT,
      "A URL must be specified via " +
      "ServiceDispatcher_ServiceProxy.setEndPoint(URL).");
    }

    call.setMethodName("dispatchRequest");
    Vector params = new Vector();
    Parameter meth1_inType1Param = new Parameter("meth1_inType1",
      java.lang.String.class, meth1_inType1, null);
    params.addElement(meth1_inType1Param);
    Parameter meth1_inType2Param = new Parameter("meth1_inType2",
      java.lang.String.class, meth1_inType2, null);
    params.addElement(meth1_inType2Param);
    call.setParams(params);
    Response resp = call.invoke(url, SOAPActionURI);

    // Check the response.
    if (resp.generatedFault())
    {
      Fault fault = resp.getFault();

      throw new SOAPException(fault.getFaultCode(), fault.getFaultString());
    }
    else
    {
      Parameter retValue = resp.getReturnValue();
      return (java.lang.String)retValue.getValue();
    }
  }
  /**
   * Dispatch a request
   * @param meth1_inType1 ServiceName
   * @param meth1_inType2 CorrelationID
   * @param meth1_inType3 Message
   * @return String the result
   * @throws SOAPException
   */
  public  java.lang.String dispatchRequest
	 (java.lang.String meth1_inType1,
	 java.lang.String meth1_inType2, java.lang.String meth1_inType3) throws SOAPException
   {
	Call call = new Call();
	SOAPMappingRegistry smr = call.getSOAPMappingRegistry();
	call.setTargetObjectURI("urn:service-dispatcher");
	call.setEncodingStyleURI("http://schemas.xmlsoap.org/soap/encoding/");
	String SOAPActionURI = "urn:service-dispatcher";

	 if (url == null)
	 {
	   throw new SOAPException(Constants.FAULT_CODE_CLIENT,
	   "A URL must be specified via " +
	   "ServiceDispatcher_ServiceProxy.setEndPoint(URL).");
	 }

	 call.setMethodName("dispatchRequest");
	 Vector params = new Vector();
	 Parameter meth1_inType1Param = new Parameter("meth1_inType1",
	   java.lang.String.class, meth1_inType1, null);
	 params.addElement(meth1_inType1Param);
	 Parameter meth1_inType2Param = new Parameter("meth1_inType2",
	   java.lang.String.class, meth1_inType2, null);
	 params.addElement(meth1_inType2Param);
	Parameter meth1_inType3Param = new Parameter("meth1_inType3",
	  java.lang.String.class, meth1_inType3, null);
	params.addElement(meth1_inType3Param);
	 call.setParams(params);
	 Response resp = call.invoke(url, SOAPActionURI);

	 // Check the response.
	 if (resp.generatedFault())
	 {
	   Fault fault = resp.getFault();

	   throw new SOAPException(fault.getFaultCode(), fault.getFaultString());
	 }
	 else
	 {
	   Parameter retValue = resp.getReturnValue();
	   return (java.lang.String)retValue.getValue();
	 }
   }
  public synchronized URL getEndPoint()
  {
    return url;
  }
  public synchronized void setEndPoint(URL url)
  {
    this.url = url;
  }
  public static void main (String argv[]) {
	try {
  		ServiceDispatcher_ServiceProxy proxy=new ServiceDispatcher_ServiceProxy();
  		proxy.setEndPoint ( new URL("http://b0934103.itc-nl01.ing.nld:80/AdapterFramework/servlet/rpcrouter"));
  		String result=proxy.dispatchRequest("RekenBoxWebService", "correlationID01", "rekenboxverzoek");
  		System.out.println(result);
  		
  	} catch (Exception e) {
  		System.out.println(e.getMessage());
  	}
  	
  }
}
