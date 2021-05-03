/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden

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

import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;

/**
 * Converts an XML string (input) to a set of java objects using the
 * <a href="http://jakarta.apache.org/commons/digester">digester</a>.
 * <p>The result is an anonymous object. Your digester-rules file should specify
 * how the xml file is parsed, and what the root object will be.</p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * </table></p>
 * @author Richard Punt
 * @since 4.0.1 : adjustments to support multi-threading
 */

public class DigesterPipe extends FixedForwardPipe {

	private String digesterRulesFile;
	private URL rulesURL;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		try {
			rulesURL = ClassUtils.getResourceURL(this, digesterRulesFile);
 			 DigesterLoader.createDigester(rulesURL); // load rules to check if they can be loaded when needed
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix(null)+"Digester rules file ["+digesterRulesFile+"] not found", e);
		}
		log.debug(getLogPrefix(null)+"End of configuration");
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		//Multi threading: instantiate digester for each request as the digester is NOT thread-safe.
		//TODO: make a pool of digesters
		Digester digester = DigesterLoader.createDigester(rulesURL);

		try {
			return new PipeRunResult(getSuccessForward(), digester.parse(message.asReader()));
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)+"exception in digesting", e);
		}
	}


	/**
	 * Sets the location of the resource with digester rules used for processing messages.
	 */
	@IbisDoc({"name of file that containts the rules for xml parsing", "(none)"})
	public void setDigesterRulesFile(String digesterRulesFile) {
		this.digesterRulesFile = digesterRulesFile;
	}
	public String getDigesterRulesFile() {
		return digesterRulesFile;
	}

}
