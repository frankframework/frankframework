/*
 * $Log: XmlValidator.java,v $
 * Revision 1.4  2005-07-28 07:41:06  europe\L190409
 * added log-keyword
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.util.Variant;

import java.io.IOException;

import nl.nn.adapterframework.configuration.*;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import java.net.URL;

import nl.nn.adapterframework.util.ClassUtils;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 *<code>Pipe</code> that validates the input message against a XML-Schema.
 * <p><b>Notice:</b> this implementation relies on Xerces and is rather
 * version-sensitive. It relies on the validation features of it. You should test the proper
 * working of this pipe extensively on your deployment platform.</p>
 * <p>The XmlValidaor relies on the properties for <code>external-schemaLocation</code> and
 * <code>external-noNamespaceSchemaLocation</code>.. In
 * Xerces-J-2.4.0 there came a bug-fix for these features, so a previous version was erroneous.<br/>
 *  Xerces-j-2.2.1 included a fix on this, so before this version there were problems too (the features did not work).<br/>
 * Therefore: old versions of
 * Xerses on your container may not be able to set the necessary properties, or
 * accept the properties but not do the actual validation! This functionality should
 * work (it does! with Xerces-J-2.6.0 anyway), but testing is necessary!</p>
  *<p><i>Careful: test this on your deployment environment</i></p>
* <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSchema(String) schema}</td><td>name of the xml schema on the classpath. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
  * </table>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
 * </table>
 * @version Id
 * @author Johan Verrips IOS
 */
public class XmlValidator extends FixedForwardPipe {
	public static final String version="$RCSfile: XmlValidator.java,v $ $Revision: 1.4 $ $Date: 2005-07-28 07:41:06 $";

    private String schema;
    private String schemaURL;
    private boolean throwException = false;
    private boolean isFullSchemaChecking = false;

    public class XmlErrorHandler implements org.xml.sax.ErrorHandler {
        private boolean errorOccured = false;
        private Exception reason;

        public void error(org.xml.sax.SAXParseException exception) {
            log.error(exception);
            errorOccured = true;
            reason = exception;
        }

        public void warning(org.xml.sax.SAXParseException exception)
                throws SAXException {
            log.warn(exception);
            errorOccured = true;
            reason = exception;
        }

        public void fatalError(org.xml.sax.SAXParseException exception) {
            log.error(exception);
            errorOccured = true;
            reason = exception;
        }

        public boolean hasErrorOccured() {
            return errorOccured;
        }

        public Exception getReason() {
            return reason;
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
            "Pipe [" + getName() + "]"
                    + " has no forward with name [failure]");
        }

        URL url = ClassUtils.getResourceURL(this, schema);
        if (url == null) {
            throw new ConfigurationException(
                    "Pipe [" + getName() + "] cannot retrieve [" + schema + "]");
        }

        schemaURL = url.toExternalForm();

        try {
            getParser();
        } catch (SAXNotRecognizedException e) {
            throw new ConfigurationException("Pipe [" + getName() + " : parser did not recognize necessary feature ", e);
        } catch (SAXNotSupportedException e) {
            throw new ConfigurationException("Pipe [" + getName() + " : parser did not support necessary feature ", e);
        } catch (SAXException e) {
            throw new ConfigurationException("Pipe [" + getName() + " : error configuring the parser", e);
        }


    }
     /**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public PipeRunResult doPipe(Object input, PipeLineSession session)
            throws PipeRunException {

        boolean isValid = false;
        Variant in = new Variant(input);

        InputSource is = in.asXmlInputSource();
        XmlErrorHandler xeh = new XmlErrorHandler();
        XMLReader parser=null;
        try {
            parser = getParser();
            parser.setErrorHandler(xeh);
        } catch (SAXNotRecognizedException e) {
            throw new PipeRunException(this, getLogPrefix(session)+ "Unrecognized feature is requested",  e);
        } catch (SAXNotSupportedException e) {
            throw new PipeRunException(this, getLogPrefix(session)+ "Not supported feature is requested",  e);
        } catch (SAXException e) {
            throw new PipeRunException(this, getLogPrefix(session)+ "Error instantiating parser",  e);
        }

        try {
            parser.parse(is);
            isValid = !(xeh.hasErrorOccured());
            if (!isValid) log.error(getLogPrefix(session) + "got error validating:", xeh.getReason());
        } catch (IOException e) {
            throw new PipeRunException(this, "IoException occured on parsing the document", e);
        } catch (SAXException e) {
            throw new PipeRunException(this, "SAXException occured on parsing the document", e);
        }

        if (!isValid) {
            if (isThrowException())
                throw new PipeRunException(
                        this,
                        getLogPrefix(session) + "got invalid xml according to schema [" + schema + "]", xeh.getReason());
            else
                return new PipeRunResult(findForward("failure"), input);
        }
        return new PipeRunResult(getForward(), input);
    }
    /**
     * Enable full schema grammar constraint checking, including
     *  checking which may be time-consuming or memory intensive.
     *  Currently, particle unique attribution constraint checking and particle
     * derivation resriction checking are controlled by this option.
     */
    public boolean getFullSchemaChecking() {
        return isFullSchemaChecking;
    }
    /**
     * Get a configured parser.
     * @return XMLReader
     * @throws SAXNotRecognizedException
     * @throws SAXNotSupportedException
     * @throws SAXException
     */
    private XMLReader getParser() throws SAXNotRecognizedException,
            SAXNotSupportedException, SAXException {
        XMLReader parser = null;

        parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://apache.org/xml/features/validation/schema", true);
        if (schema.indexOf(' ') > 0)
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", schemaURL);
        else
            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", schemaURL);
        if (getFullSchemaChecking())
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

        return parser;
    }
    /**
     * get the name of the schema
     * <p>Creation date: (05-12-2003 10:29:35)</p>
     * @return java.lang.String the name of the schema
     */
    public java.lang.String getSchema() {
        return schema;
    }
    /**
     * 
     * <p>Creation date: (05-12-2003 10:29:35)</p>
     * @return boolean indicating wether an exception should be thrown or not.
     */
    public boolean isThrowException() {
        return throwException;
    }
    /**
     * Enable full schema grammar constraint checking, including
     *  checking which may be time-consuming or memory intensive.
     *  Currently, particle unique attribution constraint checking and particle
     * derivation resriction checking are controlled by this option.
     * <p> see property http://apache.org/xml/features/validation/schema-full-checking</p>
     * Defaults to <code>false</code>;
     */
    public void setFullSchemaChecking(boolean fullSchemaChecking) {
        isFullSchemaChecking = fullSchemaChecking;
    }
    /**
     * <p>The uri to the schema definition. If there is a space in the string, the Pipe assumes
     * you are using a namespace with a url to the xml schema.</p>
     * <p> The syntax is the same as for schemaLocation attributes
     *  in instance documents: e.g, "http://www.example.com file_name.xsd" (namespace aware).</p>
     *  <p>The user can specify more than one XML Schema in the list.</p>
     * <p>Alternatively (no namespaces) you just set the name of the schema,
     * e.g. "xml/xsd/GetPartyDetail.xsd"</p>
     * <p>Creation date: (05-12-2003 10:29:35)</p>
     * @param newSchema java.lang.String
     */
    public void setSchema(java.lang.String newSchema) {
        schema = newSchema;
    }
    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant.
     * <p>Creation date: (05-12-2003 10:29:35)</p>
     * @param newThrowException boolean
     */
    public void setThrowException(boolean newThrowException) {
        throwException = newThrowException;
    }
}
