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
 */

public class ServiceDispatcher_ServiceProxy
{
		public static final String version="$Id: ServiceDispatcher_ServiceProxy.java,v 1.1 2004-02-04 08:36:20 a1909356#db2admin Exp $";

  private Call call = new Call();
  private URL url = null;
  private String SOAPActionURI = "";
  private SOAPMappingRegistry smr = call.getSOAPMappingRegistry();

  public ServiceDispatcher_ServiceProxy() throws MalformedURLException
  {
    call.setTargetObjectURI("urn:service-dispatcher");
    call.setEncodingStyleURI("http://schemas.xmlsoap.org/soap/encoding/");
    this.url = new URL("http://localhost:8080/AdapterFramework/servlet/rpcrouter");
    this.SOAPActionURI = "urn:service-dispatcher";
  }
  public synchronized java.lang.String dispatchRequest
    (java.lang.String meth1_inType1,
    java.lang.String meth1_inType2) throws SOAPException
  {
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
  public synchronized URL getEndPoint()
  {
    return url;
  }
  public synchronized void setEndPoint(URL url)
  {
    this.url = url;
  }
}
