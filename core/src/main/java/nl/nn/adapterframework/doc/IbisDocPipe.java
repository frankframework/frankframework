/*
   Copyright 2018-2020 WeAreFrank!

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
package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.objects.DocInfo;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Generate documentation and XSD for code completion of beautiful Ibis configurations in Eclipse
 *
 * @author Jaco de Groot
 */
public class IbisDocPipe extends FixedForwardPipe {
	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		DocInfo docInfo;
		try {
			docInfo = new InfoBuilder().build();
		} catch(Exception e) {
			throw new PipeRunException(this, "Could not gather the necessary information", e);
		}
		String uri = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			try {
				ParameterValueList pvl = parameterList.getValues(message, session);
				uri = pvl.getParameterValue("uri").asStringValue(null);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}
		if (uri == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "uri parameter not found or null");
		}
		String result = "Not found";
		String contentType = "text/html";
		if ("/ibisdoc/ibisdoc.xsd".equals(uri)) {
			result = DocWriter.getSchema(docInfo);
			contentType = "application/xml";
		} else if ("/ibisdoc/uglify_lookup.xml".equals(uri)) {
			result = DocWriter.getUglifyLookup(docInfo);
			contentType = "application/xml";
		} else if ("/ibisdoc/ibisdoc.json".equals(uri)) {
			result = DocWriter.getJson(docInfo);
			contentType = "application/json";
		} else if ("/ibisdoc".equals(uri)) {
			result = "<html>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.html\">ibisdoc.html (deprecated)</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.xsd\">ibisdoc.xsd</a><br/>\n"
					+ "  <a href=\"ibisdoc/uglify_lookup.xml\">uglify_lookup.xml</a><br/>\n"
					+ "  <a href=\"ibisdoc/ibisdoc.json\">ibisdoc.json</a><br/>\n"
					+ "  <a href=\"../iaf/ibisdoc\">The new ibisdoc application</a><br/>\n"
					+ "</html>";
		} else if ("/ibisdoc/ibisdoc.html".equals(uri)) {
			result = DocWriter.getHtmlFrankDocTopLevel();
		} else if (uri.endsWith(".html")) {
			if ("/ibisdoc/topmenu.html".equals(uri)) {
				result = DocWriter.getHtmlFrankDocTopMenu(docInfo);
			} else if ("/ibisdoc/all.html".equals(uri)) {
				result = DocWriter.getHtmlFrankDocAll(docInfo);
			} else if ("/ibisdoc/excludes.html".equals(uri)) {
				result = DocWriter.getHtmlFrankDocExcludes(docInfo);
			} else {
				if (uri.length() > "/ibisdoc/".length() && uri.indexOf(".") != -1) {
					String page = uri.substring("/ibisdoc/".length(), uri.lastIndexOf("."));
					result = DocWriter.getHtmlFrankDocGroupOrBean(page, docInfo);
				}
			}
		}
		session.put("contentType", contentType);
		return new PipeRunResult(getForward(), result);
	}
}
