/*
   Copyright 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.core;

import java.util.Set;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.validation.IXSD;

public interface IXmlValidator extends IValidator {

	public ConfigurationException getConfigurationException();

	public String getMessageRoot();

	/**
	 * @return noNamespaceSchemalocation, if specified
	 */
	public String getSchema();
	public String getSchemaLocation();
	public Set<IXSD> getXsds() throws ConfigurationException;

	/**
	 * Provide additional generic documentation on the validation of the
	 * subsequent processing. This documentation will be included in generated
	 * schema's like WSDL or OpenApi
	 */
	public String getDocumentation();
}
