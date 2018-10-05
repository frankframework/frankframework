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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

/**
 * Perform an JSON to XML transformation
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr>
 * <th>attributes</th>
 * <th>description</th>
 * <th>default</th>
 * </tr>
 * <tr>
 * <td>className</td>
 * <td>nl.nn.adapterframework.pipes.JsonPipe</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setDirection(String) direction}</td>
 * <td>Direction of the transformation. Either json2xml or xml2json</td>
 * <td>json2xml</td>
 * </tr>
 * <tr>
 * <td>{@link #setVersion(String) version}</td>
 * <td>Version of the jsonpipe. Either 1 or 2.</td>
 * <td>1</td>
 * </tr>
 * <tr><td>{@link #setAddXmlRootElement(boolean) addXmlRootElement}</td><td>when true, and direction is json2xml, it wraps a root element around the converted message</td><td>true</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>Exits:</b>
 * <table border="1">
 * <tr>
 * <th>state</th>
 * <th>condition</th>
 * </tr>
 * <tr>
 * <td>"success"</td>
 * <td>default</td>
 * </tr>
 * <tr>
 * <td>
 * <i>{@link #setForwardName(String) forwardName}</i>
 * </td>
 * <td>if specified</td>
 * </tr>
 * </table>
 * </p>
 * @author Martijn Onstwedder
 * @author Tom van der Heijden
 */

public class JsonPipe extends FixedForwardPipe {
	private String direction = "json2xml";
	private String version = "1";
	private boolean addXmlRootElement=true;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException(getLogPrefix(null) + "direction must be set");
		}
		if (!"json2xml".equals(dir) && !"xml2json".equals(dir)) {
			throw new ConfigurationException(
					getLogPrefix(null) + "illegal value for direction [" + dir + "], must be 'xml2json' or 'json2xml'");
		}
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {

		if (input == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "got null input");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "got an invalid type as input, expected String, got " + input.getClass().getName());
		}

		try {
			String stringResult = (String) input;
			String actualDirection = getDirection();
			String actualVersion = getVersion();
			
			if ("json2xml".equalsIgnoreCase(actualDirection)) {
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
					stringResult = (String) input;
					ParameterResolutionContext prc = new ParameterResolutionContext(stringResult, session, true);
					TransformerPool transformerPool = TransformerPool.configureTransformer0(getLogPrefix(null), classLoader, null, null,
							"/xml/xsl/xml2json.xsl", null, false, null, true);
					stringResult = transformerPool.transform(prc.getInputSource(), null);
				} else {
					JSONObject jsonObject = XML.toJSONObject(stringResult);
					stringResult = jsonObject.toString();
				}
			}
			return new PipeRunResult(getForward(), stringResult);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + " Exception on transforming input", e);
		}
	}

	public void setDirection(String string) {
		direction = string;
	}

	public String getDirection() {
		return StringUtils.lowerCase(direction);
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public boolean addXmlRootElement() {
		return addXmlRootElement;
	}
	public void setAddXmlRootElement(boolean addXmlRootElement) {
		this.addXmlRootElement = addXmlRootElement;
	}
}
