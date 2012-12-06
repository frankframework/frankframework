package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Straightforward XML-validation based on javax.validation. This is work in programs.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class JavaxXmlValidator extends AbstractXmlValidator {

	protected static final Logger LOG = LogUtil.getLogger(JavaxXmlValidator.class);

	// TODO I think many (if not all) schemas can simply be registered globally, because xmlns should be uniquely defined.
	// missing a generic generic mechanism for now
	private static final Map<String, URL> globalRegistry = new HashMap<String, URL>();

	static {
		globalRegistry.put("http://schemas.xmlsoap.org/soap/envelope/", ClassUtils.getResourceURL("/Tibco/xsd/soap/envelope.xsd"));
		//globalRegistry.put("http://ing.nn.afd/AFDTypes",                ClassUtils.getResourceURL("/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd"));
	}

	private Map<String, Schema> javaxSchemas = new HashMap();

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
	public String validate(Object input/*impossible to understand*/,
			IPipeLineSession session, String logPrefix) throws XmlValidatorException, ConfigurationException, PipeRunException {
		InputSource source = getInputSource(input);
		SAXSource sax = new SAXSource(source);
		return validate(sax, session);
	}

	protected String validate(Source source, IPipeLineSession session) throws XmlValidatorException, ConfigurationException, PipeRunException {
		String schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			getSchemaObject(schemasId, schemasProvider.getSchemas(session));
		}
		Schema xsd = javaxSchemas.get(schemasId);
		try {
			Validator validator = xsd.newValidator();
			validator.setResourceResolver(new LSResourceResolver() {
				public LSInput resolveResource(String s, String s1, String s2, String s3, String s4) {
					System.out.println("--");
					return null;//To change body of implemented methods Settings | File Templates.
				}
			});
			validator.setErrorHandler(new ErrorHandler() {
				public void warning(SAXParseException e) throws SAXException {
					System.out.println("--");
				}

				public void error(SAXParseException e) throws SAXException {
					System.out.println("--");
					//To change body of implemented methods Settings | File Templates.
				}

				public void fatalError(SAXParseException e) throws SAXException {
					System.out.println("--");
					//To change body of implemented methods Settings | File Templates.
				}
			});
			validator.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
			validator.validate(source);
		} catch (SAXException e) {
			throw new XmlValidatorException(e.getMessage());
		} catch (IOException e) {
			throw new XmlValidatorException(e.getMessage(), e);
		}
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
	}

	/**
	 * Returns the {@link Schema} associated with this validator. This ia an XSD schema containing knowledge about the
	 * schema source as returned by {@link #getSchemaSources()}
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

}
