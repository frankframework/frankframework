/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package org.frankframework.extensions.rekenbox;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;

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
 * </pre></code>
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
 * @author Gerrit van Brakel
 */
@Category("NN-Special")
public class LabelFormat extends FixedForwardPipe {

	private String direction=null;
	private static final String DIRECTION_XML2LABEL = "Xml2Label";

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			String result;
			if (getDirection().equalsIgnoreCase(DIRECTION_XML2LABEL)) {

				DocumentBuilder documentBuilder = XmlUtils.getDocumentBuilderFactory().newDocumentBuilder();
				Document document = documentBuilder.parse(message.asInputSource());

				result = XmlToLabelFormat.doTransformation(document);
				return new PipeRunResult(getSuccessForward(), result);
			}
			else {
				XMLReader reader = XMLReaderFactory.createXMLReader("org.frankframework.extensions.rekenbox.CalcboxOutputReader");
				CalcboxContentHandler handler = new CalcboxContentHandler(message.asString());
				reader.setContentHandler(handler);

				return new PipeRunResult(getSuccessForward(), handler.getStringResult());
			}
		}
		catch (Exception e) {
			throw new PipeRunException(this, "cannot transform", e);
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
