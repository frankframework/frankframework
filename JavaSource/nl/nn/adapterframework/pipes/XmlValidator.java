/*
 * $Log: XmlValidator.java,v $
 * Revision 1.12  2005-10-17 11:37:34  europe\L190409
 * made thread-safe
 *
 * Revision 1.11  2005/09/27 11:06:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2005/09/26 11:33:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added parserError forward
 *
 * Revision 1.9  2005/09/20 13:26:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed need for baseResourceURL
 *
 * Revision 1.8  2005/09/05 09:33:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed typo in methodname setReasonSessionKey()
 *
 * Revision 1.7  2005/09/05 07:01:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute reasonSessionKey
 *
 * Revision 1.6  2005/08/31 16:36:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced logging
 * added usage note about JDK 1.3 vs JDK 1.4
 *
 * Revision 1.5  2005/08/30 16:04:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework based on code of Jaco de Groot
 *
 */
package nl.nn.adapterframework.pipes;


import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.net.URL;
import java.io.IOException;



/**
 *<code>Pipe</code> that validates the input message against a XML-Schema.
 *
 * <p><b>Notice:</b> this implementation relies on Xerces and is rather
 * version-sensitive. It relies on the validation features of it. You should test the proper
 * working of this pipe extensively on your deployment platform.</p>
 * <p>The XmlValidaor relies on the properties for <code>external-schemaLocation</code> and
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
 * <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>none</td></tr>
 * </table>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
 * </table>
 * @version Id
 * @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)

 */
public class XmlValidator extends FixedForwardPipe {
	public static final String version="$RCSfile: XmlValidator.java,v $ $Revision: 1.12 $ $Date: 2005-10-17 11:37:34 $";

    private String schemaLocation = null;
    private String noNamespaceSchemaLocation = null;
	private String schemaSessionKey = null;
    private boolean throwException = false;
    private boolean fullSchemaChecking = false;
	private String reasonSessionKey = null;

    public class XmlErrorHandler implements ErrorHandler {
        private boolean errorOccured = false;
        private String reasons;

		protected void addReason(SAXParseException exception) {
			String msg = "at ("+exception.getLineNumber()+ ","+exception.getColumnNumber()+"): "+exception.getMessage();
			errorOccured = true;
			if (reasons == null) {
				 reasons = msg;
			 } else {
				 reasons = reasons + "\n" + msg;
			 }
		}

		public void warning(SAXParseException exception) {
			addReason(exception);
		}
        public void error(SAXParseException exception) {
        	addReason(exception);
        }
        public void fatalError(SAXParseException exception) {
			addReason(exception);
        }

        public boolean hasErrorOccured() {
            return errorOccured;
        }

         public String getReasons() {
            return reasons;
        }
    }

    /**
     * Configure the XmlValidator
     * @throws ConfigurationException when:
     * <ul><li>the schema cannot be found</li>
     * <ul><li><{@link #isThrowException()} is false and there is no forward defined
     * for "failure"</li>
     * <li>when the parser does not accept setting the properties for validating</li>
     * </ul>
     */
    public void configure() throws ConfigurationException {
        super.configure();
        
		if (!isThrowException()){
            if (findForward("failure")==null) throw new ConfigurationException(
            getLogPrefix(null)+ "has no forward with name [failure]");
        }
		if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			 StringUtils.isNotEmpty(getSchemaLocation())) &&
			StringUtils.isNotEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
		}
        if (StringUtils.isNotEmpty(getSchemaLocation())) {
        	String resolvedLocations = XmlUtils.resolveSchemaLocations(getSchemaLocation());
        	log.info(getLogPrefix(null)+"resolved schemaLocation to ["+resolvedLocations+"]");
        	setSchemaLocation(resolvedLocations);
        }
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			URL url = ClassUtils.getResourceURL(this, getNoNamespaceSchemaLocation());
			if (url==null) {
				throw new ConfigurationException(getLogPrefix(null)+"could not find schema at ["+getNoNamespaceSchemaLocation()+"]");
			}
			String resolvedLocation =url.toExternalForm();
			log.info(getLogPrefix(null)+"resolved noNamespaceSchemaLocation to ["+resolvedLocation+"]");
			setNoNamespaceSchemaLocation(resolvedLocation);
		}
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}
    }

     /**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {

        Variant in = new Variant(input);

		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(getLogPrefix(session)+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

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
   				throw new PipeRunException(this, getLogPrefix(session)+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
    		}
    
    		URL url = ClassUtils.getResourceURL(this, noNamespaceSchemaLocation);
    		if (url == null) {
    			throw new PipeRunException(this, getLogPrefix(session)+ "cannot retrieve [" + noNamespaceSchemaLocation + "]");
    		}
    
			noNamespaceSchemaLocation = url.toExternalForm();
        }

		XmlErrorHandler xeh = new XmlErrorHandler();
		XMLReader parser=null;
		try {
			parser=getParser(schemaLocation,noNamespaceSchemaLocation);
			if (parser==null) {
				throw new PipeRunException(this, getLogPrefix(session)+ "could not obtain parser");
			}
			parser.setErrorHandler(xeh);
		} catch (SAXNotRecognizedException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "parser does not support necessary feature", e);
		} catch (SAXException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "error configuring the parser", e);
		}

		InputSource is = in.asXmlInputSource();

        try {
            parser.parse(is);
        } catch (IOException e) {
            throw new PipeRunException(this, getLogPrefix(session)+ "IoException occured on parsing the document", e);
        } catch (SAXException e) {
        	PipeForward error = findForward("parserError");
        	String msg=getLogPrefix(session)+ "SAXException occured on parsing the document";
        	if (error!=null) {
        		log.warn(msg,e);
				return new PipeRunResult(error, input);
        	} else {
				throw new PipeRunException(this, msg, e);
        	}
        }

		boolean isValid = !(xeh.hasErrorOccured());
		
		if (!isValid) { 
			String reasons = getLogPrefix(session) + "got invalid xml according to schema [" + Misc.concatStrings(schemaLocation," ",noNamespaceSchemaLocation) + "]:\n" + xeh.getReasons(); 
            if (isThrowException()) {
                throw new PipeRunException(this, reasons);
			} else {
				log.warn(reasons);
				if (StringUtils.isNotEmpty(getReasonSessionKey())) {
					log.debug(getLogPrefix(session) + "storing reasons under sessionKey ["+getReasonSessionKey()+"]");
					session.put(getReasonSessionKey(),reasons);
				}
				return new PipeRunResult(findForward("failure"), input);
			}
        }
        return new PipeRunResult(getForward(), input);
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
        if (isFullSchemaChecking())
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        return parser;
    }
    
    /**
     * Enable full schema grammar constraint checking, including
     * checking which may be time-consuming or memory intensive.
     *  Currently, particle unique attribution constraint checking and particle
     * derivation resriction checking are controlled by this option.
     * <p> see property http://apache.org/xml/features/validation/schema-full-checking</p>
     * Defaults to <code>false</code>;
     */
    public void setFullSchemaChecking(boolean fullSchemaChecking) {
        this.fullSchemaChecking = fullSchemaChecking;
    }
	public boolean isFullSchemaChecking() {
		return fullSchemaChecking;
	}

    /**
     * <p>The filename of the schema on the classpath. The filename (which e.g.
     * can contain spaces) is translated to an URI with the
     * ClassUtils.getResourceURL(Object,String) method (e.g. spaces are translated to %20).
     * It is not possible to specify a namespace using this attribute.
     * <p>An example value would be "xml/xsd/GetPartyDetail.xsd"</p>
     * <p>The value of the schema attribute is only used if the schemaLocation
     * attribute and the noNamespaceSchemaLocation are not set</p>
     * @see ClassUtils.getResource(Object,String)
     */
    public void setSchema(String schema) {
        setNoNamespaceSchemaLocation(schema);
    }
	public String getSchema() {
		return getNoNamespaceSchemaLocation();
	}

    /**
     * <p>Pairs of URI references (one for the namespace name, and one for a
     * hint as to the location of a schema document defining names for that
     * namespace name).</p>
     * <p> The syntax is the same as for schemaLocation attributes
     * in instance documents: e.g, "http://www.example.com file%20name.xsd".</p>
     * <p>The user can specify more than one XML Schema in the list.</p>
     * <p><b>Note</b> that spaces are considered separators for this attributed. 
     * This means that, for example, spaces in filenames should be escaped to %20.
     * </p>
     * 
     * N.B. since 4.3.0 schema locations are resolved automatically, without the need for ${baseResourceURL}
     */
    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
	public String getSchemaLocation() {
		return schemaLocation;
	}

    /**
     * <p>A URI reference as a hint as to the location of a schema document with
     * no target namespace.</p>
     */
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
    }
	public String getNoNamespaceSchemaLocation() {
		return noNamespaceSchemaLocation;
	}

	/**
	 * <p>The sessionkey to a value that is the uri to the schema definition.</P>
	 */
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}
	public String getSchemaSessionKey() {
		return schemaSessionKey;
	}

	/**
	 * @deprecated name changed to {@link setSchemaSessionKey()}
	 */
	public void setSchemaSession(String schemaSessionKey) {
		log.warn(getLogPrefix(null)+"attribute 'setSchemaSession' is deprecated. Please use 'schemaSessionKey' instead.");
		this.schemaSessionKey = schemaSessionKey;
	}


    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant.
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
	public boolean isThrowException() {
		return throwException;
	}
	
	/**
	 * The sessionkey to store the reasons of misvalidation in.
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}
	public String getReasonSessionKey() {
		return reasonSessionKey;
	}

}
