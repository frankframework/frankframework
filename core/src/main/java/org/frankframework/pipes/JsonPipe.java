/*
   Copyright 2013-2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Default;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.documentbuilder.ArrayBuilder;
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.DocumentUtils;
import org.frankframework.documentbuilder.IDocumentBuilder;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.TransformerPool;

/**
 * JSON is not aware of the element order. This pipe performs a <strong>best effort</strong> JSON to XML transformation.
 * If you wish to validate or add structure to the converted (xml) file, please use the {@link Json2XmlValidator}.
 *
 * @author Martijn Onstwedder
 * @author Tom van der Heijden
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class JsonPipe extends FixedForwardPipe {

	private static final String DEFAULT_ROOT_ELEMENT_NAME = "root";

	private @Getter Direction direction = Direction.JSON2XML;
	private Boolean addXmlRootElement = null;
	private @Getter String rootElementName;
	private @Getter boolean prettyPrint = false;

	private TransformerPool tpXml2Json;

	public enum Direction {
		JSON2XML,
		XML2JSON
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// rootElementName has been modified from its default value
		if (getDirection() == Direction.XML2JSON && StringUtils.isNotEmpty(rootElementName)) {
			throw new ConfigurationException("rootElementName can not be used when direction is XML2JSON");
		}

		if (StringUtils.isEmpty(rootElementName)) {
			rootElementName = DEFAULT_ROOT_ELEMENT_NAME;
		}

		Direction dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException("direction must be set");
		}
		if(addXmlRootElement == null) {
			addXmlRootElement = dir == Direction.JSON2XML;
		}
		if (dir == Direction.XML2JSON) {
			tpXml2Json = TransformerPool.configureStyleSheetTransformer(this, "/xml/xsl/xml2json.xsl", 2); // shouldn't this be a utility transformer?
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		if (Message.isEmpty(message)) {
			throw new PipeRunException(this, "got "+(Message.isNull(message)?"null":"empty")+" input");
		}

		try {
			switch (getDirection()) {
			case JSON2XML:
				try(JsonReader jr = Json.createReader(message.asReader())) {
					return convertJsonToXml(jr.read());
				} catch (JsonException e) {
					log.debug("cannot parse input as JsonStructure", e);
					Message result = new Message(String.format("<%s>%s</%s>", rootElementName, message.asString(), rootElementName));
					result.getContext().withMimeType(MediaType.APPLICATION_XML);
					return new PipeRunResult(getSuccessForward(), result);
				}
			case XML2JSON:
				Map<String, Object> parameterValues = Collections.singletonMap("includeRootElement", addXmlRootElement);
				Message result = tpXml2Json.transform(message, parameterValues);
				result.getContext().withMimeType(MediaType.APPLICATION_JSON);
				return new PipeRunResult(getSuccessForward(), result);
			default:
				throw new IllegalStateException("unknown direction ["+getDirection()+"]");
			}
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on transforming input", e);
		}
	}

	private PipeRunResult convertJsonToXml(JsonValue jValue) throws SAXException, IOException, PipeRunException {
		MessageBuilder builder = new MessageBuilder();
		if (jValue instanceof JsonObject jObj) { // {"d":{"convert":{"__metadata":{"type":"ZCD_API_FCC_SRV.convertcurrencys"},"amount":"0.000000000","currency":"EUR"}}}
			String root = rootElementName;
			if (!addXmlRootElement) {
				if (jObj.size()>1) {
					throw new PipeRunException(this, "Cannot extract root element name from object with ["+jObj.size()+"] names");
				}
				Entry<String,JsonValue> firstElem=jObj.entrySet().stream().findFirst().orElseThrow(()->new PipeRunException(this, "Cannot extract root element name from empty object"));
				root = firstElem.getKey();
				jValue = firstElem.getValue();
			}
			try (IDocumentBuilder documentBuilder = DocumentBuilderFactory.startDocument(DocumentFormat.XML, root, builder, isPrettyPrint())) {
				DocumentUtils.jsonValue2Document(jValue, documentBuilder);
			}
		} else {
			String root = addXmlRootElement ? rootElementName : null;
			try (IDocumentBuilder documentBuilder = DocumentBuilderFactory.startDocument(DocumentFormat.XML, root, builder, isPrettyPrint())) {
				try (ArrayBuilder arrayBuilder = documentBuilder.asArrayBuilder("item")) {
					DocumentUtils.jsonArray2Builder((JsonArray) jValue, arrayBuilder);
				}
			}
		}

		builder.setMimeType(MediaType.APPLICATION_XML);
		return new PipeRunResult(getSuccessForward(), builder.build());
	}

	/**
	 * Direction of the transformation.
	 * @ff.default JSON2XML
	 */
	public void setDirection(Direction value) {
		direction = value;
	}

	/**
	 * When direction is JSON2XML, it wraps a root element around the converted message.
	 * When direction is XML2JSON, it includes the name of the root element as a key in the converted message, thus preserving the structure of the original input message.
	 * @ff.default {@code true} when JSON2XML and {@code false} when XML2JSON
	 */
	public void setAddXmlRootElement(boolean addXmlRootElement) {
		this.addXmlRootElement = addXmlRootElement;
	}

	/**
	 * When direction is JSON2XML, specifies the name of the root element when {@literal addXmlRootElement} is {@code true}.
	 * When direction is XML2JSON, can not be used.
	 */
	@Default(DEFAULT_ROOT_ELEMENT_NAME)
	public void setRootElementName(String rootElementName) {
		this.rootElementName = rootElementName;
	}

	/** Format the output in easy legible way (currently only for JSON2XML) */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
}
