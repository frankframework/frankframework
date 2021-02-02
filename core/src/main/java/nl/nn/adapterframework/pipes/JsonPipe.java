/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an JSON to XML transformation
 *
 * @author Martijn Onstwedder
 * @author Tom van der Heijden
 */

public class JsonPipe extends FixedForwardPipe {
	private String direction = "json2xml";
	private String version = "1";
	private boolean addXmlRootElement=true;

	private TransformerPool tpXml2Json;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException("direction must be set");
		}
		if (!"json2xml".equals(dir) && !"xml2json".equals(dir)) {
			throw new ConfigurationException("illegal value for direction [" + dir + "], must be 'xml2json' or 'json2xml'");
		}
		if ("xml2json".equals(dir) && "2".equals(getVersion())) {
			tpXml2Json = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), this, "/xml/xsl/xml2json.xsl", 0);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {

		if (message == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "got null input");
		}

		try {
			String stringResult=null;
			String actualDirection = getDirection();
			String actualVersion = getVersion();
			
			if ("json2xml".equalsIgnoreCase(actualDirection)) {
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

				if(addXmlRootElement()) {
					boolean isWellFormed = XmlUtils.isWellFormed(stringResult);
					if (!isWellFormed) {
						stringResult = "<root>" + stringResult + "</root>";
					}
				}
			}

			if ("xml2json".equalsIgnoreCase(actualDirection)) {
				if ("2".equals(actualVersion)) {
					stringResult = tpXml2Json.transform(message,null);
				} else {
					JSONObject jsonObject = XML.toJSONObject(message.asString());
					stringResult = jsonObject.toString();
				}
			}
			return new PipeRunResult(getForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + " Exception on transforming input", e);
		}
	}

	@IbisDoc({"Direction of the transformation. Either json2xml or xml2json", "json2xml"})
	public void setDirection(String string) {
		direction = string;
	}

	public String getDirection() {
		return StringUtils.lowerCase(direction);
	}

	@IbisDoc({"Version of the jsonpipe. Either 1 or 2.", "1"})
	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public boolean addXmlRootElement() {
		return addXmlRootElement;
	}

	@IbisDoc({"when true, and direction is json2xml, it wraps a root element around the converted message", "true"})
	public void setAddXmlRootElement(boolean addXmlRootElement) {
		this.addXmlRootElement = addXmlRootElement;
	}
}
