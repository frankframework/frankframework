/*
 * $Log: LabelFormat.java,v $
 * Revision 1.1  2008-11-25 10:17:43  m168309
 * first version
 *
 * Revision 1.3  2006/08/25 09:35:42  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Ibis4juice versie 24-08-2006
 *
 * Revision 1.1  2005/09/29 12:43:16  Jaco de Groot <jaco.de.groot@ibissource.org>
 * First check-in of Ibis4Juice at NN.
 * This project was originally created in Gouda (Centric).
 * Ibis4Juice has just been upgraded to Adapterframework 4.3.0 at the time of this check-in.
 *
 * Revision 1.2  2005/03/21 09:18:31  kees
 * revision
 *
 * Revision 1.1  2005/03/17 11:52:03  kees
 * initial
 *
 * Revision 1.1  2005/03/09 08:28:08  richard
 * initial
 *
 * Revision 1.9  2005/01/05 12:31:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for colons in 'waarde'
 *
 * Revision 1.8  2005/01/04 12:55:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * automatic recognition of xml-input for adios2xml conversion
 * ignore unknown labels converting adios2xml
 *
 * Revision 1.7  2004/10/05 10:38:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.6  2004/08/09 13:58:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved check for existence of interface-file
 *
 * Revision 1.5  2004/04/07 06:58:21  Johan Verrips <johan.verrips@ibissource.org>
 * adjusted bericht.toXML() to bericht.toXML(true) to add an xml header
 *
 * Revision 1.4  2004/04/07 06:36:40  Johan Verrips <johan.verrips@ibissource.org>
 * removed unused log variable
 *
 * Revision 1.3  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.2  2004/03/24 15:28:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused variables
 *
 */
package nl.nn.adapterframework.extensions.rekenbox;

import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.ClassUtils;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Transforms between ascii and an XML representation.
 *
 * <p>
 * Sample xml:<br/><code><pre>
 * 	&lt;CALCBOXMESSAGE&gt;
		&lt;OPDRACHT&gt;
			&lt;OPDRACHTSOORT&gt;ONTTREK_RISICO_EN_KOSTEN&lt;/OPDRACHTSOORT&gt;
			&lt;BASISRENDEMENTSOORT&gt;NVT&lt;/BASISRENDEMENTSOORT&gt;
			&lt;BEDRAG&gt;625&lt;/BEDRAG&gt;
			&lt;DATUM&gt;20071201&lt;/DATUM&gt;
 *
 *          ...
 * 	&lt;/CALCBOXMESSAGE&gt;
 * </pre></code> <br/>
 *
 * Sample ascii:<br/><code><pre>
 * 	OPDRACHT : #SAMENGESTELD
 * 	OPDRACHT.OPDRACHTSOORT :ONTTREK_RISICO_EN_KOSTEN
 * 	OPDRACHT.BASISRENDEMENTSOORT :NVT
 * 	OPDRACHT.BEDRAG :625
 * 	OPDRACHT.DATUM :20071201
 *
 *          ...
 * 	EINDEREKENVERZOEK :EINDE
 * </p>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>transformation direction. Possible values 
 * <ul>
 *   <li>"Xml2Label": transform an XML file to ascii</li>
 *   <li>"Label2Xml": transform an ascii file to XML</li>
 * </ul></td><td>Xml2Label</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>default, when specified</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 * @version Id
 */
public class LabelFormat extends FixedForwardPipe {
	 
	private String direction=null;
	private final String DIRECTION_XML2LABEL = "Xml2Label";

	public void configure() throws ConfigurationException {
		super.configure();
	}	
	
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			String result;
			if (getDirection().equalsIgnoreCase(DIRECTION_XML2LABEL)) {
				
				Variant v = new Variant(input); 

				DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = documentBuilder.parse(new InputSource(new StringReader(v.asString())));

				result = XmlToLabelFormat.doTransformation(document).toString();
				return new PipeRunResult(getForward(), result);
			}
			else {
				Variant v = new Variant(input);
				XMLReader reader = XMLReaderFactory.createXMLReader("nl.nn.adapterframework.extensions.rekenbox.CalcboxOutputReader");
				CalcboxContentHandler handler = new CalcboxContentHandler(v.asString());
				reader.setContentHandler(handler);
				
				return new PipeRunResult(getForward(), handler.getStringResult());
			}
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot transform", e);
		}
	}

	/**
	 * sets transformation direction. Possible values 
	 * <ul>
	 *   <li>"Xml2Label": transform an XML file to ascii</li>
	 *   <li>"Label2Xml": transform an ascii file to XML</li>
	 * </ul>
	 * default: None
	 */
	public void setDirection(String newDirection) {
		direction = newDirection;
	}
	public String getDirection() {
		return direction;
	}
}
