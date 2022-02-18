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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * JSON is not aware of the element order. This pipe performs a <strong>best effort</strong> JSON to XML transformation. 
 * If you wish to validate or add structure to the converted (xml) file, please use the {@link Json2XmlValidator}.
 *
 * @author Martijn Onstwedder
 * @author Tom van der Heijden
 */
public class JsonPipe extends FixedForwardPipe {
	private @Getter Direction direction = Direction.JSON2XML;
	private @Getter String version = "2";
	private @Getter boolean addXmlRootElement=true;

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
		if (dir == Direction.XML2JSON && "2".equals(getVersion())) {
			tpXml2Json = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), this, "/xml/xsl/xml2json.xsl", 0);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		if (message == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "got null input");
		}

		try {
			String stringResult=null;

			switch (getDirection()) {
			case JSON2XML:
				stringResult = message.asString();
				JSONTokener jsonTokener = new JSONTokener(stringResult);
				if (stringResult.startsWith("{")) {
					JSONObject jsonObject = new JSONObject(jsonTokener);
					stringResult = XML.toString(jsonObject);
				}
				if (stringResult.startsWith("[")) {
					JSONArray jsonArray = new JSONArray(jsonTokener);
					stringResult = XML.toString(jsonArray);
				}

				if(isAddXmlRootElement()) {
					boolean isWellFormed = XmlUtils.isWellFormed(stringResult);
					if (!isWellFormed) {
						stringResult = "<root>" + stringResult + "</root>";
					}
				}
				break;
			case XML2JSON:
				if ("2".equals(getVersion())) {
					stringResult = tpXml2Json.transform(message,null);
				} else {
					JSONObject jsonObject = XML.toJSONObject(message.asString());
					stringResult = jsonObject.toString();
				}
				break;
			default:
				throw new IllegalStateException("unknown direction ["+getDirection()+"]");
			}

			return new PipeRunResult(getSuccessForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + " Exception on transforming input", e);
		}
	}

	@IbisDoc({"Direction of the transformation.", "JSON2XML"})
	public void setDirection(Direction value) {
		direction = value;
	}

	@IbisDoc({"Version of the JsonPipe. Either 1 or 2.", "2"})
	public void setVersion(String version) {
		this.version = version;
	}

	@IbisDoc({"When true, and direction is json2xml, it wraps a root element around the converted message", "true"})
	public void setAddXmlRootElement(boolean addXmlRootElement) {
		this.addXmlRootElement = addXmlRootElement;
	}
}
