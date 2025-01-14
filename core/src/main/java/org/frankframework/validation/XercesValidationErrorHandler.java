/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.validation;

import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.HasApplicationContext;

@Log4j2
class XercesValidationErrorHandler implements XMLErrorHandler {
	private final boolean allowConsoleWarnings;
	private final HasApplicationContext source;

	public XercesValidationErrorHandler(HasApplicationContext source, boolean allowConsoleWarnings) {
		this.source = source;
		this.allowConsoleWarnings = allowConsoleWarnings;
	}

	@Override
	public void warning(String domain, String key, XMLParseException e) throws XNIException {
		handleException(e, SuppressKeys.XSD_VALIDATION_WARNINGS_SUPPRESS_KEY);
	}

	@Override
	public void error(String domain, String key, XMLParseException e) throws XNIException {
		handleException(e, SuppressKeys.XSD_VALIDATION_ERROR_SUPPRESS_KEY);
	}

	@Override
	public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
		handleException(e, SuppressKeys.XSD_VALIDATION_FATAL_ERROR_SUPPRESS_KEY);
		throw e;
	}

	private void handleException(XMLParseException e, SuppressKeys suppressKey) {
		if (suppressKey!=null) {
			if (allowConsoleWarnings) {
				ConfigurationWarnings.add(source, log, e.toString(), suppressKey);
			}
		} else {
			ConfigurationWarnings.add(source, log, e.toString());
		}

		// In case the XSD doesn't exist throw an exception to prevent the adapter from starting.
		if (e.getMessage() != null && e.getMessage().startsWith("schema_reference.4: Failed to read schema document '")) {
			throw e;
		}
	}
}
