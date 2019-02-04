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
package nl.nn.adapterframework.batch;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Translate a record using XSL.
 * 
 * 
 * @author  John Dekker
 * @deprecated Please replace by RecordXmlTransformer.
 */
public class RecordXslTransformer extends RecordXmlTransformer {

	public void configure() throws ConfigurationException {
		super.configure();
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "class ["+this.getClass().getName()+"] is deprecated. Please replace by [nl.nn.adapterframework.batch.RecordXmlTransformer]";
		configWarnings.add(log, msg);
	}

	/**
	 * @deprecated configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName'
	 * @param xslFile the xsl file
	 */
	public void setXslFile(String xslFile) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName'";
		configWarnings.add(log, msg);
		setStyleSheetName(xslFile);
	}

}
