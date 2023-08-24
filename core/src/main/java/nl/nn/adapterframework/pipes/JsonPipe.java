/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentUtils;
import nl.nn.adapterframework.stream.document.XmlDocumentBuilder;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * JSON is not aware of the element order. This pipe performs a <strong>best effort</strong> JSON to XML transformation.
 * If you wish to validate or add structure to the converted (xml) file, please use the {@link Json2XmlValidator}.
 *
 * @author Martijn Onstwedder
 * @author Tom van der Heijden
 */
@ElementType(ElementTypes.TRANSLATOR)
public class JsonPipe extends FixedForwardPipe {
	private @Getter Direction direction = Direction.JSON2XML;
	private Boolean addXmlRootElement = null;
	private @Getter boolean prettyPrint = false;

	private TransformerPool tpXml2Json;

	public enum Direction {
		JSON2XML,
		XML2JSON;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		Direction dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException("direction must be set");
		}
		if(addXmlRootElement == null) {
			addXmlRootElement = dir == Direction.JSON2XML;
		}
		if (dir == Direction.XML2JSON) {
			tpXml2Json = TransformerPool.configureStyleSheetTransformer(this, "/xml/xsl/xml2json.xsl", 0);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		if (Message.isEmpty(message)) {
			throw new PipeRunException(this, "got "+(Message.isNull(message)?"null":"empty")+" input");
		}

		try {
			String stringResult=null;

			switch (getDirection()) {
			case JSON2XML:
				try(JsonReader jr = Json.createReader(message.asReader())) {
					JsonValue jValue=null;
					try {
						jValue = jr.read();
					} catch (JsonException e) {
						log.debug("cannot parse as JsonStructure", e);
						stringResult="<root>"+message.asString()+"</root>";
						break;
					}
					String root="root";
					StringWriter writer = new StringWriter();
					if (jValue instanceof JsonObject) {
						if (!addXmlRootElement) {
							JsonObject jObj = (JsonObject)jValue;
							if (jObj.size()>1) {
								throw new PipeRunException(this, "Cannot extract root element name from object with ["+jObj.size()+"] names");
							}
							Entry<String,JsonValue> firstElem=jObj.entrySet().stream().findFirst().orElseThrow(()->new PipeRunException(this, "Cannot extract root element name from empty object"));
							root = firstElem.getKey();
							jValue = firstElem.getValue();
						}
						try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder(root, writer, isPrettyPrint())) {
							DocumentUtils.jsonValue2Document(jValue, documentBuilder);
						}
					} else {
						if (addXmlRootElement) {
							try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder(root, writer, isPrettyPrint())) {
								DocumentUtils.jsonValue2Document(jValue, documentBuilder);
							}
						} else {
							for (JsonValue item:(JsonArray)jValue) {
								try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder("item", writer, isPrettyPrint())) {
									DocumentUtils.jsonValue2Document(item, documentBuilder);
								}
							}
						}
					}
					stringResult = writer.toString();
				}
				break;
			case XML2JSON:
				Map<String, Object> parameterValues = new HashMap<>(1);
				parameterValues.put("includeRootElement", addXmlRootElement);
				stringResult = tpXml2Json.transform(message, parameterValues);
				break;
			default:
				throw new IllegalStateException("unknown direction ["+getDirection()+"]");
			}

			return new PipeRunResult(getSuccessForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on transforming input", e);
		}
	}

	/**
	 * Direction of the transformation.
	 * @ff.default JSON2XML
	 */
	public void setDirection(Direction value) {
		direction = value;
	}

	@Deprecated
	public void setVersion(String version) {
		if("2".equals(version)) {
			setAddXmlRootElement(true);
		}
	}

	/**
	 * When direction is JSON2XML, it wraps a root element around the converted message. 
	 * When direction is XML2JSON, it includes the name of the root element as a key in the converted message.
	 * @ff.default TRUE when JSON2XML and FALSE when XML2JSON
	 */
	public void setAddXmlRootElement(boolean addXmlRootElement) {
		this.addXmlRootElement = addXmlRootElement;
	}

	/** Format the output in easy legible way (currently only for XML) */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
}
