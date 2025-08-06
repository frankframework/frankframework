package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;

import javax.xml.validation.ValidatorHandler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.frankframework.core.PipeLineSession;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.validation.AbstractValidationContext;
import org.frankframework.validation.AbstractXmlValidator;
import org.frankframework.validation.DummySchemasProviderImpl;
import org.frankframework.validation.JavaxXmlValidator;
import org.frankframework.validation.XercesXmlValidator;
import org.frankframework.validation.XmlValidatorErrorHandler;

public class EntityResolvingTest {

	public String INPUT_FILE_SMALL_ENTITIES                ="/Entities/SmallEntity.xml";
	public String INPUT_FILE_TOO_LARGE_ENTITIES            ="/Entities/TooLargeEntity.xml";
	public String INPUT_FILE_FILE_EXTERNAL_ENTITIES        ="/Entities/FileExternalEntity.xml";
	public String INPUT_FILE_HTTP_EXTERNAL_ENTITIES        ="/Entities/HttpExternalEntity.xml";
	public String INPUT_FILE_SMALL_ENTITIES_RESULT         ="/Entities/SmallEntityResult.xml";
	public String INPUT_FILE_TOO_LARGE_ENTITIES_RESULT     ="/Entities/TooLargeEntityResult.xml";
	public String INPUT_FILE_FILE_EXTERNAL_ENTITIES_RESULT ="/Entities/FileExternalEntityResult.xml";
	public String INPUT_FILE_HTTP_EXTERNAL_ENTITIES_RESULT ="/Entities/HttpExternalEntityResult.xml";
	public String TOO_MANY_ENTITIES_ERROR_MESSAGE_PATTERN ="The parser has encountered more than \"100.000\" entity expansions in this document";

	public String SCHEMA_NAMESPACE="urn:entities";
	public String SCHEMA_LOCATION_ENTITIES         ="/Entities/schema.xsd";


	@NullSource
	@ValueSource(classes = {XercesXmlValidator.class, JavaxXmlValidator.class})
	@ParameterizedTest
	public void testSmallEntityExpansion(Class<? extends AbstractXmlValidator> impl) throws Exception {
		// a small number of internal entities are allowed
		testEntityExpansion(impl, SCHEMA_LOCATION_ENTITIES, INPUT_FILE_SMALL_ENTITIES, true, INPUT_FILE_SMALL_ENTITIES_RESULT);
	}

	@NullSource
	@ValueSource(classes = {XercesXmlValidator.class, JavaxXmlValidator.class})
	@ParameterizedTest
	public void testTooLargeEntityExpansion(Class<? extends AbstractXmlValidator> impl) throws Exception {
		// if the number of internal entities exceeds 100.000, and error must be raised
		testEntityExpansion(impl, SCHEMA_LOCATION_ENTITIES, INPUT_FILE_TOO_LARGE_ENTITIES, false, TOO_MANY_ENTITIES_ERROR_MESSAGE_PATTERN);
	}

	@NullSource
	@ValueSource(classes = {XercesXmlValidator.class, JavaxXmlValidator.class})
	@ParameterizedTest
	public void testFileExternalEntityExpansion(Class<? extends AbstractXmlValidator> impl) throws Exception {
		// external entities are not allowed by default
		testEntityExpansion(impl, SCHEMA_LOCATION_ENTITIES, INPUT_FILE_FILE_EXTERNAL_ENTITIES, true, INPUT_FILE_FILE_EXTERNAL_ENTITIES_RESULT);
	}

	@NullSource
	@ValueSource(classes = {XercesXmlValidator.class, JavaxXmlValidator.class})
	@ParameterizedTest
	public void testHttpExternalEntityExpansion(Class<? extends AbstractXmlValidator> impl) throws Exception {
		// external entities are not allowed by default
		testEntityExpansion(impl, SCHEMA_LOCATION_ENTITIES, INPUT_FILE_HTTP_EXTERNAL_ENTITIES, true, INPUT_FILE_HTTP_EXTERNAL_ENTITIES_RESULT);
	}

	public void testEntityExpansion(Class<? extends AbstractXmlValidator> impl, String xsd, String inputFile, boolean expectValid, String expectedResult) throws Exception {
		String xmlIn= TestFileUtils.getTestFile(inputFile);
		try {
			String actual = parseAndRenderString(impl, xsd, xmlIn);
			if (!expectValid) {
				fail("expected to fail with message: "+expectedResult);
			}
			String expected = TestFileUtils.getTestFile(expectedResult);
			assertEquals(expected, actual);
		} catch (Exception e) {
			LogUtil.getLogger(this).error("error message: "+e.getMessage());
			if (expectValid) {
				fail("expected to be valid with result: "+expectedResult);
			}
			if (e.getMessage().indexOf(expectedResult)<0) {
				LogUtil.getLogger(this).error("error message does not contain ["+expectedResult+"], but is ["+e.getMessage()+"]");
			}
		}
	}

	public String parseAndRenderString(Class<? extends AbstractXmlValidator> impl, String xsd, String xmlIn) throws Exception {
		if(impl == null) {
			return parseAndRenderNative(xsd, xmlIn);
		}
		return parseAndRenderWithValidator(impl, xsd, xmlIn);
	}


	public String parseAndRenderNative(String xsd, String xmlIn) throws Exception {
		Document doc= XmlUtils.buildDomDocument(xmlIn);
		String actual=XmlUtils.nodeToString(doc);
		return actual;
	}

	public String parseAndRenderWithValidator(Class<? extends AbstractXmlValidator> impl, String xsd, String xmlIn) throws Exception {
//      AbstractXmlValidator instance = implementation.newInstance();
//      instance.setSchemasProvider(new SchemasProviderImpl(SCHEMA_NAMESPACE, xsd));
//      instance.setIgnoreUnknownNamespaces(false);
//      instance.setAddNamespaceToSchema(false);
//      instance.configure("Setup");
//      String result=instance.validate(xmlIn, new PipeLineSessionBase(), "test");
//      System.out.println("Validation Result:"+result);
//      return instance.get
//		return result;

		AbstractXmlValidator instance = ClassUtils.newInstance(impl);
		instance.setSchemasProvider(new DummySchemasProviderImpl(SCHEMA_NAMESPACE, xsd));
		instance.setThrowException(true);
		instance.setFullSchemaChecking(true);
		instance.configure(null);
		instance.start();

		PipeLineSession session = new PipeLineSession();
		AbstractValidationContext context = instance.createValidationContext(session, null, null);
		ValidatorHandler validatorHandler = instance.getValidatorHandler(session, context);
		StringReader sr = new StringReader(xmlIn);
		InputSource is = new InputSource(sr);
		final StringBuilder sb = new StringBuilder();

		ContentHandler ch = new DefaultHandler() {

			boolean elementOpen;

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (elementOpen) {
					sb.append(">");
					elementOpen=false;
				}
				sb.append(ch, start, length);
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				sb.append("<").append(localName);
				sb.append(" xmlns=\"").append(uri).append("\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
				for (int i=0;i<atts.getLength();i++) {
					sb.append(' ').append(atts.getLocalName(i)).append("=\"").append(atts.getValue(i)).append('"');
				}
				elementOpen=true;
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (elementOpen) {
					sb.append("/>");
					elementOpen=false;
				} else {
					sb.append("</").append(localName).append(">");
				}
			}


			@Override
			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
				//ignore
			}

		};

		validatorHandler.setContentHandler(ch);

		instance.validate(is, validatorHandler, session, context);

		XmlValidatorErrorHandler errorHandler = context.getErrorHandler();
		if (errorHandler.isErrorOccurred()) {
			throw new SAXException(errorHandler.getReasons());
		}
		return sb.toString();
	}

}
