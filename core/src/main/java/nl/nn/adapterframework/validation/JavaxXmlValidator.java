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
package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSModel;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

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

	// TODO I think many (if not all) schemas can simply be registered globally, because xmlns should be uniquely defined.
	// missing a generic generic mechanism for now
//	private static final Map<String, URL> globalRegistry = new HashMap<String, URL>();
//
//	static {
//		globalRegistry.put("http://schemas.xmlsoap.org/soap/envelope/", ClassUtils.getResourceURL("/Tibco/xsd/soap/envelope.xsd"));
//		//globalRegistry.put("http://ing.nn.afd/AFDTypes",                ClassUtils.getResourceURL("/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd"));
//	}

	private Map<String, Schema> javaxSchemas = new HashMap<String, Schema>();

	@Override
	protected void init() throws ConfigurationException {
		if (needsInit) {
			super.init();
			String schemasId = null;
			schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				getSchemaObject(schemasId, schemasProvider.getSchemas());
			}
		}
	}

	@Override
	public JavaxValidationContext createValidationContext(IPipeLineSession session, Set<List<String>> rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		super.createValidationContext(session, rootValidations, invalidRootNamespaces);
	
		String schemasId;
		Schema schema;
		Set<String> namespaceSet=new HashSet<String>();
		List<XSModel> xsModels=null;

		init();
		schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			getSchemaObject(schemasId, schemasProvider.getSchemas(session));
		}
		schema = javaxSchemas.get(schemasId);
		String mainFailureMessage = "Validation using " + schemasProvider.getClass().getSimpleName() + " with '" + schemasId + "' failed";

		if (schema!=null) {
			org.apache.xerces.jaxp.validation.XSGrammarPoolContainer xercesSchema = (org.apache.xerces.jaxp.validation.XSGrammarPoolContainer)schema;
			xercesSchema.getGrammarPool();
	
			xsModels=new LinkedList<XSModel>();
			Grammar[] grammars=xercesSchema.getGrammarPool().retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
			for(int i=0;i<grammars.length;i++) {
				XSModel model=((XSGrammar)grammars[i]).toXSModel();
				xsModels.add(model);
				StringList namespaces=model.getNamespaces();
				for (int j=0;j<namespaces.getLength();j++) {
					String namespace=namespaces.item(j);
					namespaceSet.add(namespace);
				}
			}
		}

		JavaxValidationContext result= new JavaxValidationContext(schemasId, schema, namespaceSet, xsModels);
		result.init(schemasProvider, schemasId, namespaceSet, rootValidations, invalidRootNamespaces, ignoreUnknownNamespaces);
		return result;
	}

	@Override
	public ValidatorHandler getValidatorHandler(IPipeLineSession session, ValidationContext context) throws ConfigurationException, PipeRunException {
		Schema schema=getSchemaObject(context.getSchemasId(), schemasProvider.getSchemas(session));
		return schema.newValidatorHandler();
	}

	@Override
	public XMLReader createValidatingParser(IPipeLineSession session, ValidationContext context) throws XmlValidatorException, PipeRunException {
		SAXParser parser;
		try {
			SAXParserFactory parserFactory=SAXParserFactory.newInstance();
			parserFactory.setValidating(false);
			parserFactory.setNamespaceAware(true);
			parserFactory.setFeature(PARSING_FEATURE_SECURE, true);
			//parserFactory.setFeature(PARSING_FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
			//parserFactory.setFeature(PARSING_FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
			//parserFactory.setFeature(PARSING_FEATURE_DISALLOW_INLINE_DOCTYPE, true);

			Schema schema=getSchemaObject(context.getSchemasId(), schemasProvider.getSchemas(session));
			parserFactory.setSchema(schema);
			
			parser = parserFactory.newSAXParser();
			return parser.getXMLReader();
		} catch (ParserConfigurationException e) {
			throw new XmlValidatorException(logPrefix + "cannot configure parser", e);
		} catch (ConfigurationException e) {
			throw new XmlValidatorException(logPrefix + "cannot configure parser", e);
		} catch (SAXException e) {
			throw new XmlValidatorException(logPrefix + "cannot create parser", e);
		}
	}



//	protected String validate(Source source, IPipeLineSession session) throws XmlValidatorException, ConfigurationException, PipeRunException {
//        init();
//		String schemasId = schemasProvider.getSchemasId();
//		if (schemasId == null) {
//			schemasId = schemasProvider.getSchemasId(session);
//			getSchemaObject(schemasId, schemasProvider.getSchemas(session));
//		}
//		Schema xsd = javaxSchemas.get(schemasId);
//		try {
//			Validator validator = xsd.newValidator();
//			validator.setResourceResolver(new LSResourceResolver() {
//				public LSInput resolveResource(String s, String s1, String s2, String s3, String s4) {
//                    System.out.println("--");
//					return null;//To change body of implemented methods Settings | File Templates.
//				}
//			});
//			validator.setErrorHandler(new ErrorHandler() {
//				public void warning(SAXParseException e) throws SAXException {
//                    log.warn(e.getMessage());
//				}
//
//				public void error(SAXParseException e) throws SAXException {
//					log.error(e.getMessage());
//                }
//
//				public void fatalError(SAXParseException e) throws SAXException {
//					log.error(e.getMessage());
//				}
//			});
//			//validator.setFeature("http://xml.org/sax/features/namespace-prefixes", true); /// DOESNT" WORK any more?
//			validator.validate(source);
//		} catch (SAXException e) {
//			throw new XmlValidatorException(e.getClass() + " " + e.getMessage());
//		} catch (IOException e) {
//			throw new XmlValidatorException(e.getMessage(), e);
//		}
//		return XML_VALIDATOR_VALID_MONITOR_EVENT;
//	}

	/**
	 * Returns the {@link Schema} associated with this validator. This ia an XSD schema containing knowledge about the
	 * schema source as returned by {@link #getSchemaSources(List)}
	 * @throws ConfigurationException
	 */
	protected synchronized Schema getSchemaObject(String schemasId, List<nl.nn.adapterframework.validation.Schema> schemas) throws  ConfigurationException {
		Schema schema = javaxSchemas.get(schemasId);
		if (schema == null) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			factory.setResourceResolver(new LSResourceResolver() {
				public LSInput resolveResource(String s, String s1, String s2, String s3, String s4) {
					return null;
				}
			});
			try {
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

	protected List<Source> getSchemaSources(List<nl.nn.adapterframework.validation.Schema> schemas) throws IOException, XMLStreamException, ConfigurationException {
		List<Source> result = new ArrayList<Source>();
		for (nl.nn.adapterframework.validation.Schema schema : schemas) {
			result.add(new StreamSource(schema.getInputStream(), schema.getSystemId()));
		}
		return result;
	}

	protected List<XSModel> getXSModels(List<nl.nn.adapterframework.validation.Schema> schemas) throws IOException, XMLStreamException, ConfigurationException {
		List<XSModel> result = new ArrayList<XSModel>();
		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		for (nl.nn.adapterframework.validation.Schema schema : schemas) {
			XSModel xsModel = xsLoader.loadURI(schema.getSystemId());
			result.add(xsModel);
		}
		return result;
	}
}

class JavaxValidationContext extends ValidationContext {

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
