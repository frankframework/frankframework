/*
 * $Log: XmlValidatorBaseXerces26.java,v $
 * Revision 1.15  2012-03-27 10:08:40  europe\m168309
 * in case of incorrect xsd log.error instead of throw exception
 *
 * Revision 1.14  2012/03/19 11:01:38  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * avoid log4j warning "No appenders could be found for logger"
 *
 * Revision 1.13  2012/03/16 15:35:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added EsbSoapValidator and WsdlXmlValidator, made WSDL's available for all adapters and did a bugfix on XML Validator where it seems to be dependent on the order of specified XSD's
 *
 * Revision 1.12  2011/12/15 09:55:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added Ibis WSDL generator (created by Michiel)
 *
 * Revision 1.8  2011/10/03 09:16:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed multithreading issue
 *
 * Revision 1.7  2011/09/27 15:16:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove hard coding
 *
 * Revision 1.6  2011/09/26 17:18:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * document schemaSessionKey
 *
 * Revision 1.5  2011/09/26 17:05:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed dynamic schema selection
 *
 * Revision 1.4  2011/09/26 11:25:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix for threading problem (experimental)
 * show xsdlocation in error message for dynamic schemas too
 *
 * Revision 1.3  2011/09/13 13:40:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE in error handling when rootElement not yet parsed
 *
 * Revision 1.2  2011/09/12 14:27:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed schemaLocation handling using public and systemIds
 *
 * Revision 1.1  2011/08/22 09:52:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * baseclasse for XmlValidation, that uses Xerces grammar pool
 *
 */
package nl.nn.adapterframework.util;


import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.*;
import org.apache.xerces.util.*;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.*;
import org.apache.xerces.xni.parser.*;
import org.xml.sax.*;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;

import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;

import static org.apache.xerces.parsers.XMLGrammarCachingConfiguration.BIG_PRIME;


/**
 * baseclass for validating input message against a XML-Schema.
 *
 * <p><b>Notice:</b> this implementation relies on Xerces and is rather
 * version-sensitive. It relies on the validation features of it. You should test the proper
 * working of this pipe extensively on your deployment platform.</p>
 * <p>The XmlValidator relies on the properties for <code>external-schemaLocation</code> and
 * <code>external-noNamespaceSchemaLocation</code>. In
 * Xerces-J-2.4.0 there came a bug-fix for these features, so a previous version was erroneous.<br/>
 * Xerces-j-2.2.1 included a fix on this, so before this version there were problems too (the features did not work).<br/>
 * Therefore: old versions of
 * Xerses on your container may not be able to set the necessary properties, or
 * accept the properties but not do the actual validation! This functionality should
 * work (it does! with Xerces-J-2.6.0 anyway), but testing is necessary!</p>
 * <p><i>Careful 1: test this on your deployment environment</i></p>
 * <p><i>Careful 2: beware of behaviour differences between different JDKs: JDK 1.4 works much better than JDK 1.3</i></p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method. (effectively the same as noNamespaceSchemaLocation)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>key of session variable holding dynamic value of noNamespaceSchemaLocation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>none</td></tr>
 * <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>none</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValidateFile(boolean) validateFile}</td><td>when set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file, only used when {@link #setValidateFile(boolean) validateFile} is <code>true</code></td><td>UTF-8</td></tr>
 * </table>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link nl.nn.adapterframework.pipes.XmlValidator#setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"notValid"</td><td>if a validation error occurred</td></tr>
 * </table>
 * <br>
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * @version Id
 * @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)
 */
public class XmlValidatorBaseXerces26 extends XmlValidatorBaseBase {
    private static final XMLEventFactory EVENT_FACTORY   = XMLEventFactory.newInstance();
    private static final XMLInputFactory INPUT_FACTORY   = XMLInputFactory.newInstance();
    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    /** Property identifier: symbol table. */
    public static final String SYMBOL_TABLE = Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: grammar pool. */
    public static final String GRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    // feature ids

    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    protected static final String REASON_ERROR      = "parserError";
    protected static final String REASON_INVALID    = "failure";
    protected static final String REASON_WRONG_ROOT = "illegalRoot";


	private String globalSchema = null;
	private CachingParserPool globalParserPool;
	private Map<String, CachingParserPool> parserPools;

	private XMLParserConfiguration globalParserConfig = null;
	private Map<String, XMLParserConfiguration> parserConfigurations;

	private SymbolTable globalSymbolTable = null;
	private Map<String, SymbolTable> symbolTables = null;
	private XMLGrammarPool globalGrammarPool = null;
	private Map<String, XMLGrammarPool> grammarPools = null;
    private boolean addNamespaceToSchema = false;

	private static final int MODE = 2;

    @Override
	public void configure(String logPrefix) throws ConfigurationException {
		super.configure(logPrefix);
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			StringUtils.isNotEmpty(getSchemaLocation())) {
			globalSchema = getSchemaLocation();
			if (StringUtils.isEmpty(globalSchema)) {
				globalSchema = getNoNamespaceSchemaLocation();
			}
			try {
				switch (MODE) {
				case 0:
					globalParserPool = createParserPool(globalSchema, StringUtils.isEmpty(getSchemaLocation()));
					break;
				case 1:
					globalParserConfig = createParserConfiguration(globalSchema, StringUtils.isEmpty(getSchemaLocation()));
					break;
				case 2:
					globalSymbolTable = createSymbolTable();
					globalGrammarPool = createGrammarPool(globalSymbolTable, globalSchema, StringUtils.isEmpty(getSchemaLocation()));
					break;
				}
			} catch (XmlValidatorException e) {
				throw new ConfigurationException(e);
			} catch (Exception e) {
				throw new ConfigurationException("cannot compile schema for [" + globalSchema + "]", e);
			}
		} else {
			switch (MODE) {
			case 0:
				parserPools = new ConcurrentHashMap<String, CachingParserPool>();
				break;
			case 1:
				parserConfigurations = new ConcurrentHashMap<String, XMLParserConfiguration>();
				break;
			case 2:
				symbolTables = new ConcurrentHashMap<String, SymbolTable>();
				grammarPools = new ConcurrentHashMap<String, XMLGrammarPool>();
				break;
			}
		}
	}


	private SymbolTable createSymbolTable() {
		SymbolTable sym = new SymbolTable(BIG_PRIME);
		return sym;
	}

	private XMLGrammarPool createGrammarPool(SymbolTable sym, String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(sym);
		XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();

        preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
        preparser.setProperty(GRAMMAR_POOL, grammarPool);
        preparser.setFeature(NAMESPACES_FEATURE_ID, true);
        preparser.setFeature(VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        MyErrorHandler errorHandler = new MyErrorHandler();
        preparser.setErrorHandler(errorHandler);
        if (singleSchema) {
			preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS(new Definition(null, schemas)));
		} else {

            List<Definition> definitions = new ArrayList<Definition>();
            String[] schema = schemas.trim().split("\\s+");
            for (int i = 0; i < schema.length; i+=2) {
                definitions.add(new Definition(schema[i], schema[i + 1]));
            }

            // Loop over the definitions until nothing changes
            // This makes sure that the _order_ is not important in the 'schemas'.
            boolean changes;
            do {
                changes = false;
                for (Iterator<Definition> i = definitions.iterator(); i.hasNext();) {
                    Definition def = i.next();
                    try {
                        preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS(def));
                        changes = true;
                        i.remove();
                    } catch (RetryException e) {
                        // Try in next iteration
                    }
                }
            } while (changes);
            // loop the remaining ones, they seem to be unresolvable, so let the exception go then
            errorHandler.throwOnError = false;
            for (Definition def : definitions) {
                preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS(def));
            }
        }
		grammarPool.lockPool();
		return grammarPool;
	}

	private CachingParserPool createParserPool(String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		SymbolTable sym = new SymbolTable(BIG_PRIME);
		XMLGrammarPool grammarPool = createGrammarPool(sym, schemas, singleSchema);
		CachingParserPool parserPool;

		parserPool = new CachingParserPool(sym,grammarPool);
		//parserPool.setShadowSymbolTable(true);

        return parserPool;
	}

	private XMLParserConfiguration createParserConfiguration(String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		SymbolTable sym = createSymbolTable();
		XMLGrammarPool grammarPool = createGrammarPool(sym, schemas, singleSchema);

		XMLParserConfiguration parserConfiguration = new IntegratedParserConfiguration(new SynchronizedSymbolTable(sym), new CachingParserPool.SynchronizedGrammarPool(grammarPool));

        parserConfiguration.setFeature(NAMESPACES_FEATURE_ID, true);
        parserConfiguration.setFeature(VALIDATION_FEATURE_ID, true);
        parserConfiguration.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        parserConfiguration.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());

        return parserConfiguration;
	}


	private  CachingParserPool getParserPool(String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		CachingParserPool result = parserPools.get(schemas);
		if (result==null) {
			result = createParserPool(schemas,singleSchema);
			parserPools.put(schemas, result);
		}
		return result;
	}
	private  XMLParserConfiguration getParserConfiguration(String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		XMLParserConfiguration result = parserConfigurations.get(schemas);
		if (result == null) {
			result = createParserConfiguration(schemas,singleSchema);
			parserConfigurations.put(schemas, result);
		}
		return result;
	}

	private SymbolTable getSymbolTable(String schemas) throws XmlValidatorException {
		SymbolTable result = symbolTables.get(schemas);
		if (result == null) {
			result = createSymbolTable();
			symbolTables.put(schemas, result);
		}
		return result;
	}

	private XMLGrammarPool getGrammarPool(SymbolTable symbolTable, String schemas, boolean singleSchema) throws XmlValidatorException, XMLStreamException, IOException {
		XMLGrammarPool result = grammarPools.get(schemas);
		if (result == null) {
			result = createGrammarPool(symbolTable, schemas, singleSchema);
			grammarPools.put(schemas, result);
		}
		return result;
	}

	/**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}
      * @return MonitorEvent declared in{@link XmlValidatorBaseBase}

      * @throws XmlValidatorException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public String validate(Object input, PipeLineSession session, String logPrefix) throws XmlValidatorException {

        Variant in = new Variant(input);

		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
			session.remove(getXmlReasonSessionKey());
		}

		String schema = globalSchema;
		CachingParserPool parserPool = globalParserPool;
		XMLParserConfiguration parserConfig = globalParserConfig;
		SymbolTable symbolTable = globalSymbolTable;
		XMLGrammarPool grammarPool = globalGrammarPool;
		if (schema == null) {
   			// now look for the new session way
   			String schemaSessionKey = getSchemaSessionKey();
   			String schemaLocation;
   			if (session.containsKey(schemaSessionKey)) {
				schemaLocation = session.get(schemaSessionKey).toString();
   			} else {
   				throw new XmlValidatorException(logPrefix + "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
    		}
			URL url = ClassUtils.getResourceURL(this, schemaLocation);
			if (url == null) {
				throw new XmlValidatorException(logPrefix + "could not find schema at [" + schemaLocation + "]");
			}
			String resolvedLocation = url.toExternalForm();
			log.info(logPrefix + "resolved noNamespaceSchemaLocation [" + schemaLocation + "] to [" + resolvedLocation + "]");

            try {
                switch (MODE) {
                case 0:
                    parserPool = getParserPool(resolvedLocation, true);
                    break;
                case 1:
                    parserConfig = getParserConfiguration(resolvedLocation, true);
                    break;
                case 2:
                    symbolTable = getSymbolTable(resolvedLocation);
                    grammarPool = getGrammarPool(symbolTable, resolvedLocation, true);
                    break;
                }
            } catch (IOException e) {
                throw new XmlValidatorException(e.getMessage(), e);
            } catch (XMLStreamException e) {
                throw new XmlValidatorException(e.getMessage(), e);
            }
            schema = schemaLocation;
        }

		XmlErrorHandler xeh;

		XMLReader parser = null;
		try {
			switch (MODE) {
			case 0:
				parser = getParser(parserPool);
				break;
			case 1:
				parser = getParser(parserConfig);
				break;
			case 2:
				parser = getParser(symbolTable, grammarPool);
				break;
			}
			if (parser == null) {
				throw new XmlValidatorException(logPrefix +  "could not obtain parser");
			}
	        if (StringUtils.isNotEmpty(getRoot()) || StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
	        	parser.setContentHandler(new XmlFindingHandler());
	        }
			xeh = new XmlErrorHandler(parser);
            parser.setErrorHandler(xeh);
		} catch (SAXNotRecognizedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not support necessary feature", e);
		} catch (SAXException e) {
			throw new XmlValidatorException(logPrefix + "error configuring the parser", e);
		}

		InputSource is;
		if (isValidateFile()) {
			try {
				is = new InputSource(new InputStreamReader(new FileInputStream(in.asString()),getCharset()));
			} catch (FileNotFoundException e) {
				throw new XmlValidatorException("could not find file ["+in.asString()+"]",e);
			} catch (UnsupportedEncodingException e) {
				throw new XmlValidatorException("could not use charset ["+getCharset()+"]",e);
			}
		} else {
			is = in.asXmlInputSource();
		}

        try {
            parser.parse(is);
         } catch (Exception e) {
			return handleFailures(xeh,session,"", REASON_ERROR, XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT, e);
        }

        if (StringUtils.isNotEmpty(getRoot())) {
        	String parsedRootElementName=((XmlFindingHandler)parser.getContentHandler()).getRootElementName();
    		boolean illegalRoot = !getRoot().equals(parsedRootElementName);
			if (illegalRoot) {
				String str = "got xml with root element ["+parsedRootElementName+"] instead of ["+getRoot()+"]";
				xeh.addReason(str,"");
				return handleFailures(xeh,session,"",REASON_WRONG_ROOT, XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT, null);
			}
        }
		boolean isValid = !(xeh.hasErrorOccured());

		if (!isValid) {
			String mainReason = logPrefix + "got invalid xml according to schema [" + schema + "]";
			return handleFailures(xeh,session,mainReason,REASON_INVALID, XML_VALIDATOR_NOT_VALID_MONITOR_EVENT, null);
        }
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
    }


    /**
     * Get a configured parser.
     */
    private XMLReader getParser(CachingParserPool parserPool) throws SAXException {

    	XMLReader parser = parserPool.createSAXParser();
    	parser.setFeature(NAMESPACES_FEATURE_ID, true);
    	parser.setFeature(VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        return parser;
    }

    private XMLReader getParser(XMLParserConfiguration parserConfiguration) throws SAXException {

    	XMLReader parser = new SAXParser(parserConfiguration);
        return parser;
    }

    private XMLReader getParser(SymbolTable symbolTable, XMLGrammarPool grammarpool) throws  SAXException {
    	XMLReader parser = new SAXParser(new ShadowedSymbolTable(symbolTable),grammarpool);
    	parser.setFeature(NAMESPACES_FEATURE_ID, true);
    	parser.setFeature(VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        return parser;
    }

    private static final String XSD = "http://www.w3.org/2001/XMLSchema";
    protected static final QName SCHEMA = new QName(XSD, "schema");
    protected static final QName TNS = new QName(null, "targetNamespace");
    protected static final QName ELFORMDEFAULT = new QName(null, "elementFormDefault");

    private XMLInputSource stringToXIS(Definition def) throws IOException, XMLStreamException {
        if (addNamespaceToSchema) {
            XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(new URL(def.systemId).openStream(), "UTF-8");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // using a copy-thread, it would be possible to avoid this buffer too.
            XMLEventWriter writer = OUTPUT_FACTORY.createXMLEventWriter(buffer, "UTF-8");

            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (e.isStartElement()) {
                    StartElement el = e.asStartElement();
                    if (el.getName().equals(SCHEMA)) {
                        if (def.publicId == null) {
                            log.warn("No publicId for " + def.systemId);
                        } else {
                            StartElement ne =
                                XMLStreamUtils.mergeAttributes(el,
                                    Collections.singletonList(new AttributeEvent(TNS, def.publicId)).iterator(), EVENT_FACTORY);
                            if (!ne.equals(e)) {
                                e = XMLStreamUtils.mergeAttributes(ne,
                                    Collections.singletonList(new AttributeEvent(ELFORMDEFAULT, "qualified")).iterator(), EVENT_FACTORY);
                                log.warn(def.systemId + " Corrected " + el + " -> " + e);

                            }
                        }
                    }
                }
                writer.add(e);
            }
            writer.close();
            InputStream input = new ByteArrayInputStream(buffer.toByteArray());

            return new XMLInputSource(def.publicId, def.systemId, null, input, "UTF-8");
        } else {
            return new XMLInputSource(def.publicId, def.systemId, null);
        }
    }

    public boolean isAddNamespaceToSchema() {
        return addNamespaceToSchema;
    }

    public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
        this.addNamespaceToSchema = addNamespaceToSchema;
    }

    private static class Definition {
        final String publicId;
        final String systemId;
        Definition(String pid, String sid) {
            this.publicId = pid;
            this.systemId = sid;
        }

        @Override
        public int hashCode() {
            int result = publicId != null ? publicId.hashCode() : 0;
            result = 31 * result + (systemId != null ? systemId.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Definition that = (Definition) o;

            if (publicId != null ? !publicId.equals(that.publicId) : that.publicId != null) return false;
            if (systemId != null ? !systemId.equals(that.systemId) : that.systemId != null) return false;

            return true;
        }
    }
    private static class RetryException extends XNIException {
        public RetryException(String s) {
            super(s);
        }
    }
    private static class MyErrorHandler implements XMLErrorHandler {
    	protected Logger log = LogUtil.getLogger(this);
        protected boolean throwOnError = true;
        public void warning(String domain, String key, XMLParseException e) throws XNIException {
            log.debug(domain + "." + key + ":" + e.getMessage(), e);
        }

        public void error(String domain, String key, XMLParseException e) throws XNIException {
            if (throwOnError) {
            	// TODO: show errors as configuration warnings in the IBIS console main page
                //throw new RetryException(e.getMessage());
            	log.error(domain + "." + key + ":" + e.getMessage(), e);
            } else {
            	log.error(domain + "." + key + ":" + e.getMessage(), e);
            }
        }

        public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
        	log.error(domain + "." + key + ":" + e.getMessage(), e);
            throw new XNIException(e.getMessage());
        }
    }
}
