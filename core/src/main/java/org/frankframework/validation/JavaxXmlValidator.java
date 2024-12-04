/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2022, 2023 WeAreFrank!

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.lifecycle.LifecycleException;

/**
 * Straightforward XML-validation based on javax.validation. This is work in programs.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class JavaxXmlValidator extends AbstractXmlValidator {

	public static final String PARSING_FEATURE_SECURE="http://javax.xml.XMLConstants/feature/secure-processing";
	public static final String PARSING_FEATURE_EXTERNAL_GENERAL_ENTITIES="http://xml.org/sax/features/external-general-entities";
	public static final String PARSING_FEATURE_EXTERNAL_PARAMETER_ENTITIES="http://xml.org/sax/features/external-parameter-entities";
	public static final String PARSING_FEATURE_DISALLOW_INLINE_DOCTYPE="http://apache.org/xml/features/disallow-doctype-decl";

	private final Map<String, Schema> javaxSchemas = new LinkedHashMap<>();

	@Override
	public void configure(IConfigurationAware owner) throws ConfigurationException {
		if (!isXmlSchema1_0()) {
			throw new ConfigurationException("class ("+this.getClass().getName()+") only supports XmlSchema version 1.0, no ["+getXmlSchemaVersion()+"]");
		}
		super.configure(owner);
	}

	@Override
	public void start()  {
		super.start();

		try {
			String schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				getSchemaObject(schemasId, schemasProvider.getSchemas());
			}
		} catch (ConfigurationException e) {
			throw new LifecycleException(e);
		}
	}

	@Override
	public JavaxValidationContext createValidationContext(PipeLineSession session, RootValidations rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		super.createValidationContext(session, rootValidations, invalidRootNamespaces);

		String schemasId;
		Schema schema;
		Set<String> namespaceSet = new HashSet<>();
		List<XSModel> xsModels = null;

		start();
		schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			getSchemaObject(schemasId, schemasProvider.getSchemas(session));
		}
		schema = javaxSchemas.get(schemasId);

		if (schema!=null) {
			org.apache.xerces.jaxp.validation.XSGrammarPoolContainer xercesSchema = (org.apache.xerces.jaxp.validation.XSGrammarPoolContainer)schema;
			xercesSchema.getGrammarPool();

			xsModels = new ArrayList<>();
			Grammar[] grammars=xercesSchema.getGrammarPool().retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
			//namespaceSet.add(""); // allow empty namespace, to cover 'ElementFormDefault="Unqualified"'. N.B. beware, this will cause SoapValidator to miss validation failure of a non-namespaced SoapBody
			for (Grammar grammar : grammars) {
				XSModel model = ((XSGrammar) grammar).toXSModel();
				xsModels.add(model);
				StringList namespaces = model.getNamespaces();
				for (int j = 0; j < namespaces.getLength(); j++) {
					String namespace = namespaces.item(j);
					namespaceSet.add(namespace);
				}
			}
		}

		JavaxValidationContext result = new JavaxValidationContext(schemasId, schema, namespaceSet, xsModels);
		result.init(schemasProvider, schemasId, namespaceSet, rootValidations, invalidRootNamespaces, ignoreUnknownNamespaces);
		return result;
	}

	@Override
	public ValidatorHandler getValidatorHandler(PipeLineSession session, AbstractValidationContext context) throws ConfigurationException, PipeRunException {
		Schema schema=getSchemaObject(context.getSchemasId(), schemasProvider.getSchemas(session));
		return schema.newValidatorHandler();
	}

	/**
	 * Returns the {@link Schema} associated with this validator. This is an XSD schema containing knowledge about the
	 * schema source as returned by {@link #getSchemaSources(List)}
	 */
	protected synchronized Schema getSchemaObject(String schemasId, List<org.frankframework.validation.Schema> schemas) throws ConfigurationException {
		Schema schema = javaxSchemas.get(schemasId);
		if (schema == null) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

			factory.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException e) {
					handleException(e, SuppressKeys.XSD_VALIDATION_WARNINGS_SUPPRESS_KEY);
				}

				@Override
				public void error(SAXParseException e) {
					handleException(e, SuppressKeys.XSD_VALIDATION_ERROR_SUPPRESS_KEY);
				}

				@Override
				public void fatalError(SAXParseException e) throws SAXException {
					handleException(e, SuppressKeys.XSD_VALIDATION_FATAL_ERROR_SUPPRESS_KEY);
					throw e;
				}

				private void handleException(SAXParseException e, SuppressKeys suppressKey) {
					if (suppressKey != null) {
						ConfigurationWarnings.add(getOwner(), log, e.toString(), suppressKey);
					} else {
						ConfigurationWarnings.add(getOwner(), log, e.toString());
					}
				}
			});
			factory.setResourceResolver((s, s1, s2, s3, s4) -> null);
			try {
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

				Collection<Source> sources = getSchemaSources(schemas);
				schema = factory.newSchema(sources.toArray(new Source[sources.size()]));
				javaxSchemas.put(schemasId, schema);
			} catch (Exception e) {
				throw new ConfigurationException("cannot read schema's ["
						+ schemasId + "]", e);
			}
		}
		return schema;
	}

	protected List<Source> getSchemaSources(List<org.frankframework.validation.Schema> schemas) throws IOException {
		List<Source> result = new ArrayList<>();
		for (org.frankframework.validation.Schema schema : schemas) {
			result.add(new StreamSource(schema.getReader(), schema.getSystemId()));
		}
		return result;
	}

	protected List<XSModel> getXSModels(List<org.frankframework.validation.Schema> schemas) {
		List<XSModel> result = new ArrayList<>();
		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		for (org.frankframework.validation.Schema schema : schemas) {
			XSModel xsModel = xsLoader.loadURI(schema.getSystemId());
			result.add(xsModel);
		}
		return result;
	}

	@Override
	public List<XSModel> getXSModels() {
		throw new NotImplementedException("getXSModels()");
	}
}

class JavaxValidationContext extends AbstractValidationContext {

	String schemasId;
	Schema schema;
	Set<String> namespaceSet;
	List<XSModel> xsModels;

	JavaxValidationContext(String schemasId, Schema schema, Set<String> namespaceSet, List<XSModel> xsModels) {
		super();
		this.schemasId=schemasId;
		this.schema=schema;
		this.namespaceSet=namespaceSet;
		this.xsModels=xsModels;
	}

	@Override
	public String getSchemasId() {
		return schemasId;
	}

	@Override
	public Set<String> getNamespaceSet() {
		return namespaceSet;
	}

	@Override
	public List<XSModel> getXsModels() {
		return xsModels;
	}

}
