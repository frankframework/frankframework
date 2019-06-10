/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;


/** 
 * @author Richard Punt
 * @since 4.0.1 : adjustments to support multi-threading
 */
@IbisDescription(
	"Converts an XML string (input) to a set of java objects using the \n" + 
	"<a href=\"http://jakarta.apache.org/commons/digester\">digester</a>. \n" + 
	"<p>The result is an anonymous object. Your digester-rules file should specify \n" + 
	"how the xml file is parsed, and what the root object will be.</p> \n" + 
	"<p><b>Exits:</b> \n" + 
	"<table border=\"1\"> \n" + 
	"<tr><th>state</th><th>condition</th></tr> \n" + 
	"<tr><td>\"success\"</td><td>default</td></tr> \n" + 
	"</table></p> \n" 
)

public class DigesterPipe extends FixedForwardPipe {

	private String digesterRulesFile;
	private URL rulesURL;

	@Override
    public void configure() throws ConfigurationException {
		super.configure();

		try {
     		 rulesURL = ClassUtils.getResourceURL(classLoader, digesterRulesFile);
 			 DigesterLoader.createDigester(rulesURL); // load rules to check if they can be loaded when needed
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix(null)+"Digester rules file ["+digesterRulesFile+"] not found", e);
		}
		log.debug(getLogPrefix(null)+"End of configuration");
	}

	@Override
    public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		//Multi threading: instantiate digester for each request as the digester is NOT thread-safe.
		//TODO: make a pool of digesters
		Digester digester = DigesterLoader.createDigester(rulesURL);

		try {
			ByteArrayInputStream xmlInputStream =
				new ByteArrayInputStream(input.toString().getBytes());

			return new PipeRunResult(getForward(), digester.parse(xmlInputStream));

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
