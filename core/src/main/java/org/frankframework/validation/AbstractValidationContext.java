/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.xs.XSModel;

public abstract class AbstractValidationContext {

	private XmlValidatorContentHandler contentHandler;
	private XmlValidatorErrorHandler errorHandler;

	public abstract String getSchemasId();
	public abstract Set<String> getNamespaceSet();
	public abstract List<XSModel> getXsModels();

	public void init(SchemasProvider schemasProvider, String schemasId, Set<String> validNamespaces, RootValidations rootValidations, Map<List<String>, List<String>> invalidRootNamespaces, Boolean ignoreUnknownNamespaces) {
		String mainFailureMessage = "Validation using " + schemasProvider.getClass().getSimpleName() + " with '" + schemasId + "' failed";
		contentHandler = new XmlValidatorContentHandler(validNamespaces,rootValidations, invalidRootNamespaces, ignoreUnknownNamespaces);
		errorHandler = new XmlValidatorErrorHandler(contentHandler, mainFailureMessage);
		contentHandler.setXmlValidatorErrorHandler(errorHandler);
	}

	public XmlValidatorContentHandler getContentHandler() {
		return contentHandler;
	}
	public void setContentHandler(XmlValidatorContentHandler contentHandler) {
		this.contentHandler = contentHandler;
	}

	public XmlValidatorErrorHandler getErrorHandler() {
		return errorHandler;
	}
	public void setErrorHandler(XmlValidatorErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}
}
