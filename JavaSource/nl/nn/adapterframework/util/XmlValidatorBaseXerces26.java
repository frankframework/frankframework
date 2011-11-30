/*
 * $Log: XmlValidatorBaseXerces26.java,v $
 * Revision 1.10  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.CachingParserPool;
import org.apache.xerces.parsers.IntegratedParserConfiguration;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarCachingConfiguration;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.ShadowedSymbolTable;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.SynchronizedSymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

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
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
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

    /** Property identifier: symbol table. */
    public static final String SYMBOL_TABLE = Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: grammar pool. */
    public static final String GRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    // feature ids

    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX+Constants.NAMESPACES_FEATURE;

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = Constants.SAX_FEATURE_PREFIX+Constants.VALIDATION_FEATURE;

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX+Constants.SCHEMA_VALIDATION_FEATURE;

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX+Constants.SCHEMA_FULL_CHECKING;
    
    protected static final String REASON_ERROR="parserError";
    protected static final String REASON_INVALID="failure";
    protected static final String REASON_WRONG_ROOT="illegalRoot";
    
    
	private String globalSchema=null;
	private CachingParserPool globalParserPool;
	private Map parserPools;

	private XMLParserConfiguration globalParserConfig=null;
	private Map parserConfigurations;
	
	private SymbolTable globalSymbolTable=null;
	private Map symbolTables=null;
	private XMLGrammarPool globalGrammarPool=null;
	private Map grammarPools=null;
 
	private static final int mode=2;
	
    public static final int BIG_PRIME = XMLGrammarCachingConfiguration.BIG_PRIME;
	
	public void configure(String logPrefix) throws ConfigurationException {
		super.configure(logPrefix);
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			StringUtils.isNotEmpty(getSchemaLocation())) {
			globalSchema=getSchemaLocation();
			if (StringUtils.isEmpty(globalSchema)) {
				globalSchema=getNoNamespaceSchemaLocation();
			}
			try {
				switch (mode) {
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
			switch (mode) {
			case 0:
				parserPools = new HashMap();
				break;
			case 1:
				parserConfigurations=new HashMap();
				break;
			case 2:
				symbolTables=new HashMap();
				grammarPools=new HashMap();
				break;
			}
		}
	}
 
	private void parseSchema(XMLGrammarPreparser preparser, String publicId, String systemId) throws XmlValidatorException {
		try {
			preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS(publicId,systemId));
		} catch (Exception e) {
			throw new XmlValidatorException("cannot compile schema for [" + systemId + "]",e);
		}
	}
	

	private SymbolTable createSymbolTable() {
		SymbolTable sym = new SymbolTable(BIG_PRIME);
		return sym;
	}
	
	private XMLGrammarPool createGrammarPool(SymbolTable sym, String schemas, boolean singleSchema) throws XmlValidatorException {
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(sym);
		XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();
		
        preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
        preparser.setProperty(GRAMMAR_POOL, grammarPool);
        preparser.setFeature(NAMESPACES_FEATURE_ID, true);
        preparser.setFeature(VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());

		if (singleSchema) {
			parseSchema(preparser, null,schemas);
		} else {
			StringTokenizer st = new StringTokenizer(schemas);
			while (st.hasMoreElements()) {
				String publicId=st.nextToken();
				String systemId=st.nextToken();
				log.debug("parsing publicId ["+publicId+"] for systemId ["+systemId+"]");
				parseSchema(preparser, publicId,systemId);
			}
		}
		grammarPool.lockPool();
		return grammarPool;
	}

	private CachingParserPool createParserPool(String schemas, boolean singleSchema) throws XmlValidatorException {
		SymbolTable sym = new SymbolTable(BIG_PRIME);
		XMLGrammarPool grammarPool = createGrammarPool(sym, schemas, singleSchema);
		CachingParserPool parserPool;
		
		parserPool = new CachingParserPool(sym,grammarPool);
		//parserPool.setShadowSymbolTable(true);

        return parserPool;
	}
	
	private XMLParserConfiguration createParserConfiguration(String schemas, boolean singleSchema) throws XmlValidatorException {
		SymbolTable sym = createSymbolTable();
		XMLGrammarPool grammarPool = createGrammarPool(sym, schemas, singleSchema);

		XMLParserConfiguration parserConfiguration = new IntegratedParserConfiguration(new SynchronizedSymbolTable(sym), new CachingParserPool.SynchronizedGrammarPool(grammarPool));

        parserConfiguration.setFeature(NAMESPACES_FEATURE_ID, true);
        parserConfiguration.setFeature(VALIDATION_FEATURE_ID, true);
        parserConfiguration.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        parserConfiguration.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        
        return parserConfiguration;
	}

	
	private synchronized CachingParserPool getParserPool(String schemas, boolean singleSchema) throws XmlValidatorException {
		CachingParserPool result=(CachingParserPool)parserPools.get(schemas);
		if (result==null) {
			result=createParserPool(schemas,singleSchema);
			parserPools.put(schemas, result);
		}
		return result;
	}
	private synchronized XMLParserConfiguration getParserConfiguration(String schemas, boolean singleSchema) throws XmlValidatorException {
		XMLParserConfiguration result=(XMLParserConfiguration)parserConfigurations.get(schemas);
		if (result==null) {
			result=createParserConfiguration(schemas,singleSchema);
			parserConfigurations.put(schemas, result);
		}
		return result;
	}

	private synchronized SymbolTable getSymbolTable(String schemas) throws XmlValidatorException {
		SymbolTable result=(SymbolTable)symbolTables.get(schemas);
		if (result==null) {
			result=createSymbolTable();
			symbolTables.put(schemas, result);
		}
		return result;
	}

	private synchronized XMLGrammarPool getGrammarPool(SymbolTable symbolTable, String schemas, boolean singleSchema) throws XmlValidatorException {
		XMLGrammarPool result=(XMLGrammarPool)grammarPools.get(schemas);
		if (result==null) {
			result=createGrammarPool(symbolTable,schemas,singleSchema);
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

		String schema=globalSchema;
		CachingParserPool parserPool = globalParserPool;
		XMLParserConfiguration parserConfig=globalParserConfig;
		SymbolTable symbolTable=globalSymbolTable;
		XMLGrammarPool grammarPool = globalGrammarPool;
		if (schema==null) {
   			// now look for the new session way
   			String schemaSessionKey = getSchemaSessionKey();
   			String schemaLocation;
   			if (session.containsKey(schemaSessionKey)) {
				schemaLocation = session.get(schemaSessionKey).toString();
   			} else {
   				throw new XmlValidatorException(logPrefix+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
    		}
			URL url = ClassUtils.getResourceURL(this, schemaLocation);
			if (url==null) {
				throw new XmlValidatorException(logPrefix+"could not find schema at ["+schemaLocation+"]");
			}
			String resolvedLocation =url.toExternalForm();
			log.info(logPrefix+"resolved noNamespaceSchemaLocation ["+schemaLocation+"] to ["+resolvedLocation+"]");
			
			switch (mode) {
			case 0:
				parserPool = getParserPool(resolvedLocation, true);
				break;
			case 1:
				parserConfig=getParserConfiguration(resolvedLocation, true);
				break;
			case 2:
				symbolTable = getSymbolTable(resolvedLocation);
				grammarPool = getGrammarPool(symbolTable, resolvedLocation, true);
				break;
			}
   			schema=schemaLocation;
        }
		
		XmlErrorHandler xeh;
		
		XMLReader parser=null;
		try {
			switch (mode) {
			case 0:
				parser = getParser(parserPool);
				break;
			case 1:
				parser=getParser(parserConfig);
				break;
			case 2:
				parser=getParser(symbolTable,grammarPool);
				break;
			}
			if (parser==null) {
				throw new XmlValidatorException(logPrefix+ "could not obtain parser");
			}
	        if (StringUtils.isNotEmpty(getRoot()) || StringUtils.isNotEmpty(getXmlReasonSessionKey())) {    
	        	parser.setContentHandler(new XmlFindingHandler());
	        }
			xeh = new XmlErrorHandler(parser);
		
			parser.setErrorHandler(xeh);
		} catch (SAXNotRecognizedException e) {
			throw new XmlValidatorException(logPrefix+ "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new XmlValidatorException(logPrefix+ "parser does not support necessary feature", e);
		} catch (SAXException e) {
			throw new XmlValidatorException(logPrefix+ "error configuring the parser", e);
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
    private XMLReader getParser(CachingParserPool parserPool) throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
    	
    	XMLReader parser = parserPool.createSAXParser();
    	parser.setFeature(NAMESPACES_FEATURE_ID, true);
    	parser.setFeature(VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        return parser;
    }

    private XMLReader getParser(XMLParserConfiguration parserConfiguration) throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
    	
    	XMLReader parser = new SAXParser(parserConfiguration); 
        return parser;
    }

    private XMLReader getParser(SymbolTable symbolTable, XMLGrammarPool grammarpool) throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
    	
    	XMLReader parser = new SAXParser(new ShadowedSymbolTable(symbolTable),grammarpool); 
    	parser.setFeature(NAMESPACES_FEATURE_ID, true);
    	parser.setFeature(VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
    	parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        return parser;
    }

    private static XMLInputSource stringToXIS(String publicId, String systemId) {
        return new XMLInputSource(publicId, systemId, null);
    }

   

}
