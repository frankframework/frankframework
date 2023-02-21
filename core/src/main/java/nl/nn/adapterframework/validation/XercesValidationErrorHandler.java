/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.validation;

import org.apache.logging.log4j.Logger;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.util.LogUtil;

class XercesValidationErrorHandler implements XMLErrorHandler {
	protected Logger log = LogUtil.getLogger(this);
	protected boolean allowConsoleWarnings = true;
	private IConfigurationAware source;

	public XercesValidationErrorHandler(IConfigurationAware source, boolean allowConsoleWarnings) {
		this.source = source;
		this.allowConsoleWarnings = allowConsoleWarnings;
	}

	@Override
	public void warning(String domain, String key, XMLParseException e) throws XNIException {
		if (allowConsoleWarnings) {
			ConfigurationWarnings.add(source, log, e.getMessage(), SuppressKeys.XSD_VALIDATION_SUPPRESS_KEY);
		}

		// In case the XSD doesn't exist throw an exception to prevent the adapter from starting.
		if (e.getMessage() != null && e.getMessage().startsWith("schema_reference.4: Failed to read schema document '")) {
			throw e;
		}
	}

	@Override
	public void error(String domain, String key, XMLParseException e) throws XNIException {
		warning(domain, key, e);
	}

	@Override
	public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
		warning(domain, key, e);
		throw e;
	}
}
