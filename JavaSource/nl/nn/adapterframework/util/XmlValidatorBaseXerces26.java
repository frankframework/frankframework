/*
 * $Log: XmlValidatorBaseXerces26.java,v $
 * Revision 1.3  2011-09-13 13:40:01  l190409
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
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.IntegratedParserConfiguration;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
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
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
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
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    
    
	private String globalSchema=null;

	private SymbolTable sym = new SymbolTable(BIG_PRIME);
	private XMLGrammarPreparser preparser = new XMLGrammarPreparser(sym);
	private XMLGrammarPoolImpl grammarPool = new XMLGrammarPoolImpl();
	private XMLParserConfiguration parserConfiguration;
	private Set loadedSchemas=new HashSet();
	
	// a larg(ish) prime to use for a symbol table to be shared
    // among
    // potentially man parsers.  Start one as close to 2K (20
    // times larger than normal) and see what happens...
    public static final int BIG_PRIME = 2039;
	
	public void configure(String logPrefix) throws ConfigurationException {
		super.configure(logPrefix);
        preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
        preparser.setProperty(GRAMMAR_POOL, grammarPool);
        preparser.setFeature(NAMESPACES_FEATURE_ID, true);
        preparser.setFeature(VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
        preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			StringUtils.isNotEmpty(getSchemaLocation())) {
			globalSchema=getSchemaLocation();
			if (StringUtils.isEmpty(globalSchema)) {
				globalSchema=getNoNamespaceSchemaLocation();
			}
			try {
				if (StringUtils.isEmpty(getSchemaLocation())) {
					parseSchema(null,globalSchema);
				} else {
					StringTokenizer st = new StringTokenizer(globalSchema);
					while (st.hasMoreElements()) {
						String publicId=st.nextToken();
						String systemId=st.nextToken();
						log.debug("parsing publicId ["+publicId+"] for systemId ["+systemId+"]");
						parseSchema(publicId,systemId);
					}
				}
			} catch (XmlValidatorException e) {
				throw new ConfigurationException(e);
			} catch (Exception e) {
				throw new ConfigurationException("cannot compile schema for [" + globalSchema + "]", e);
			}
		}
        parserConfiguration = new IntegratedParserConfiguration(sym, grammarPool);
        try{
            parserConfiguration.setFeature(NAMESPACES_FEATURE_ID, true);
            parserConfiguration.setFeature(VALIDATION_FEATURE_ID, true);
            parserConfiguration.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
            parserConfiguration.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
        } catch (Exception e) {
			throw new ConfigurationException("cannot set parser features", e);
        }
	}
 
	private synchronized void parseSchema(String publicId, String systemId) throws XmlValidatorException {
    	if (!loadedSchemas.contains(systemId)) {
    		try {
    			Grammar g = preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS(publicId,systemId));
        		loadedSchemas.add(systemId);
			} catch (Exception e) {
				throw new XmlValidatorException("cannot compile schema for [" + systemId + "]",e);
			}
    	}
	}
	

	/**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
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
		if (globalSchema==null) {
   			// now look for the new session way
   			String schemaSessionKey = getSchemaSessionKey();
   			String schemaLocation;
   			if (session.containsKey(schemaSessionKey)) {
				schemaLocation = session.get(schemaSessionKey).toString();
   			} else {
   				throw new XmlValidatorException(logPrefix+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
    		}
   			parseSchema(null,schemaLocation);
        }
		
		XmlErrorHandler xeh;
		
		XMLReader parser=null;
		try {
			parser=getParser();
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
			return handleFailures(xeh,session,"", "parserError", XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT, e);
        }

        if (StringUtils.isNotEmpty(getRoot())) {
        	String parsedRootElementName=((XmlFindingHandler)parser.getContentHandler()).getRootElementName();
    		boolean illegalRoot = !getRoot().equals(parsedRootElementName);
			if (illegalRoot) {
				String str = "got xml with root element ["+parsedRootElementName+"] instead of ["+getRoot()+"]";
				xeh.addReason(str,"");
				return handleFailures(xeh,session,"","illegalRoot", XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT, null);
			} 
        }
		boolean isValid = !(xeh.hasErrorOccured());
		
		if (!isValid) { 
			String mainReason = logPrefix + "got invalid xml according to schema [" + schema + "]";
			return handleFailures(xeh,session,mainReason,"failure", XML_VALIDATOR_NOT_VALID_MONITOR_EVENT, null);
        }
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
    }


    /**
     * Get a configured parser.
     */
    private XMLReader getParser() throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
    	
    	XMLReader parser = new SAXParser(parserConfiguration); 
        return parser;
    }

    private static XMLInputSource stringToXIS(String publicId, String systemId) {
        return new XMLInputSource(publicId, systemId, null);
    }

   

}
