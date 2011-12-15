package nl.nn.adapterframework.soap;

import javanet.staxutils.XMLStreamUtils;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.Map;

/**
* Ibis uses soap in a bit odd way.
* It doesn't actually use parameters. Only 'serviceName' and 'request'. Service name is the ibis adapter, and request then simply contains an XML.
* So, the WSDL is always more or less the same it only varies in the XSD describing the 'request' parameter.
* This servlet is soap, but targeting precisely this. So we don't use CXF or Apache RPC or so, because those will be much too powerful.
*
* @todo factor away a few java 6 things I probably used (@Override, StringBuilder)
*
* @author  Michiel Meeuwissen
*/
class Soap {


   private static final String XSD      = "http://www.w3.org/2001/XMLSchema";
   private static final String XSI      = "http://www.w3.org/2001/XMLSchema-instance";

   private static final String SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
   private static final String SOAP_ENC = "http://schemas.xmlsoap.org/soap/encoding/";

   private static final String IBIS     = "urn:service-dispatcher";

   private static final XMLInputFactory  INPUT_FACTORY  = XMLInputFactory.newInstance();
   private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    static {
        INPUT_FACTORY.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.valueOf(true));
        OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.valueOf(false));
    }

    private boolean outputSoap = true;
    private boolean inputSoap  = true;


    /**
     * We don't actually do anything soap ourselves. We just completely delegate everything to the service adapter, hoping that it does it ok.
    */
   public void soap(HttpServletRequest req, HttpServletResponse res) throws TransformerException, IOException, ParserConfigurationException, SAXException, ListenerException, XMLStreamException {

       // we set up a copying stream from req.getInputStream to the a string, while parsing it, just to find
       // some values from it.
       StringWriter string = new StringWriter();
       SOAPParser parser = new SOAPParser();// this recognizes soap and ibis a bit.
       {
           XMLStreamReader reader = INPUT_FACTORY.createFilteredReader(INPUT_FACTORY.createXMLStreamReader(req.getInputStream()), parser);
           XMLStreamWriter writer = OUTPUT_FACTORY.createXMLStreamWriter(string);
           XMLStreamUtils.copy(reader, writer); // the actual copying
       }
       String correlationId = null; // TODO
       Map requestContext = null;   // TODO


       String result;
       final ServiceDispatcher dispatcher = ServiceDispatcher.getInstance();
       if (parser.serviceName != null) {
           String input = inputSoap ? parser.request : string.toString();


           result = dispatcher.dispatchRequest(
               parser.serviceName,
               correlationId,
               input,
               requestContext);
       } else {
           result = dispatcher.dispatchRequest("", correlationId, string.toString(), requestContext);
       }

       if (outputSoap) {
           writeSoapEnvelopResponse(res.getOutputStream(), result);
       } else {
           // suppose ibis also produces soap output
           res.getWriter().write(result);
       }

   }
   protected void writeSoapEnvelopResponse(OutputStream out, String result) throws XMLStreamException {
       XMLStreamWriter w = OUTPUT_FACTORY.createXMLStreamWriter(out);
       w.writeStartDocument();
       w.setPrefix("soap", SOAP_ENV);
       w.setPrefix("ibis", IBIS);
       w.writeStartElement(SOAP_ENV, "Envelope");
       {
           w.writeNamespace("soap", SOAP_ENV);
           w.writeStartElement(SOAP_ENV, "Body");
           {
               w.writeStartElement(IBIS, "dispatchRequestResponse");
               {
                   w.writeNamespace("ibis", IBIS);
                   w.writeAttribute(SOAP_ENV, "encodingStyle", SOAP_ENC);
                   w.writeStartElement(IBIS, "return");
                   {
                       w.writeNamespace("xsi", XSI);
                       w.writeNamespace("xsd", XSD);
                       w.writeAttribute(XSI, "type", "xsd:string");
                       w.writeCharacters(result);
                   }
                   w.writeEndElement();
               }
               w.writeEndElement();
           }
           w.writeEndElement();
       }
       w.writeEndElement();
       w.writeEndDocument();
       w.close();
   }


   // This parses the legacy format (I didn't know that is was not used any more)
   static class SOAPParser implements StreamFilter {
       String serviceName;
       String request;

       private String currentNameSpace;
       private String currentName;
       //private final StringBuilder currentValue = new StringBuilder();
       private final StringBuffer currentValue = new StringBuffer();

       //@Override
       public boolean accept(XMLStreamReader xmlStreamReader) {
           switch (xmlStreamReader.getEventType()) {
           case XMLStreamConstants.START_ELEMENT:
               String uri = xmlStreamReader.getNamespaceURI();
               if (uri != null) currentNameSpace = uri;
               currentName = xmlStreamReader.getLocalName();
               currentValue.setLength(0);
               break;
           case XMLStreamConstants.CDATA:
           case XMLStreamConstants.CHARACTERS:
           case XMLStreamConstants.SPACE:
               currentValue.append(xmlStreamReader.getText());
               break;
           case XMLStreamConstants.END_ELEMENT:
               if (IBIS.equals(currentNameSpace)) {
                   if ("serviceName".equals(currentName)) {
                       serviceName = currentValue.toString();
                   } else if ("request".equals(currentName)) {
                       request = currentValue.toString();
                   }
               }
               break;
           default:

           }
           return true;
       }
   }
}
