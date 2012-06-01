/*
 * $Log: XmlValidatorBaseOld.java,v $
 * Revision 1.5  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/12/08 10:57:41  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2011/11/30 13:51:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2011/09/27 15:15:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore old XmlValidator
 *
 */
package nl.nn.adapterframework.util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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
 * <br>
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * @version Id
 * @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)
 */
public class XmlValidatorBaseOld extends XmlValidatorBaseBase {

	/**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public String validate(Object input, IPipeLineSession session, String logPrefix) throws XmlValidatorException {

        Variant in = new Variant(input);
        
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
			session.remove(getXmlReasonSessionKey());
		}

		//---
		String schemaLocation = getSchemaLocation();
		String noNamespaceSchemaLocation = getNoNamespaceSchemaLocation();

        // Do filename to URL translation if schemaLocation and
        // noNamespaceSchemaLocation are not set. 
        if (schemaLocation == null && noNamespaceSchemaLocation == null) {
   			// now look for the new session way
   			String schemaToBeUsed = getSchemaSessionKey();
   			if (session.containsKey(schemaToBeUsed)) {
				noNamespaceSchemaLocation = session.get(schemaToBeUsed).toString();
   			} else {
   				throw new XmlValidatorException(logPrefix+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
    		}
    
    		URL url = ClassUtils.getResourceURL(this, noNamespaceSchemaLocation);
    		if (url == null) {
    			throw new XmlValidatorException(logPrefix+ "cannot retrieve [" + noNamespaceSchemaLocation + "]");
    		}
    
			noNamespaceSchemaLocation = url.toExternalForm();
        }

		XmlErrorHandler xeh;
		XMLReader parser=null;
		try {
			parser=getParser(schemaLocation,noNamespaceSchemaLocation);
			if (parser==null) {
				throw new XmlValidatorException(logPrefix+ "could not obtain parser");
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

		boolean illegalRoot = StringUtils.isNotEmpty(getRoot()) && 
							!((XmlFindingHandler)parser.getContentHandler()).getRootElementName().equals(getRoot());
		if (illegalRoot) {
			String str = "got xml with root element ["+((XmlFindingHandler)parser.getContentHandler()).getRootElementName()+"] instead of ["+getRoot()+"]";
			xeh.addReason(str,"");
			return handleFailures(xeh,session,"","illegalRoot", XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT, null);
		} 
		boolean isValid = !(xeh.hasErrorOccured());
		
		if (!isValid) { 
			String mainReason = logPrefix + "got invalid xml according to schema [" + Misc.concatStrings(schemaLocation," ",noNamespaceSchemaLocation) + "]";
			return handleFailures(xeh,session,mainReason,"failure", XML_VALIDATOR_NOT_VALID_MONITOR_EVENT, null);
        }
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
    }


    /**
     * Get a configured parser.
     */
    private XMLReader getParser(String schemaLocation, String noNamespaceSchemaLocation) throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
        XMLReader parser = null;
        parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://apache.org/xml/features/validation/schema", true);
        if (schemaLocation != null) {
            log.debug("Give schemaLocation to parser: " + schemaLocation);
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", schemaLocation);
        }
        if (noNamespaceSchemaLocation != null) {
			log.debug("Give noNamespaceSchemaLocation to parser: " + noNamespaceSchemaLocation);
            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", noNamespaceSchemaLocation);
        }
        if (isFullSchemaChecking()) {
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        }
        if (StringUtils.isNotEmpty(getRoot()) || StringUtils.isNotEmpty(getXmlReasonSessionKey())) {    
        	parser.setContentHandler(new XmlFindingHandler());
        }
        return parser;
    }
    

}
