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
package nl.nn.adapterframework.extensions.rekenbox;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.Variant;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
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
 * @version $Id$
 */
public class LabelFormat extends FixedForwardPipe {
	 
	private String direction=null;
	private final String DIRECTION_XML2LABEL = "Xml2Label";

	public void configure() throws ConfigurationException {
		super.configure();
	}	
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
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
