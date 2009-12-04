/*
 * $Log: XmlValidatorSender.java,v $
 * Revision 1.4  2009-12-04 18:23:34  m00f069
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.3  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.2  2008/08/13 13:45:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.1  2008/05/15 15:08:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlFindingHandler;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 *<code>Sender</code> that validates the input message against a XML-Schema.
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
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
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
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class XmlValidatorSender extends SenderWithParametersBase {

	private String schemaLocation = null;
	private String noNamespaceSchemaLocation = null;
	private String schemaSessionKey = null;
	private boolean throwException = false;
	private boolean fullSchemaChecking = false;
	private String reasonSessionKey = null;
	private String xmlReasonSessionKey = null;
	private String root = null;
	private boolean validateFile=false;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	public class XmlErrorHandler implements ErrorHandler {
		private boolean errorOccured = false;
		private String reasons;
		private XMLReader parser;
		private XmlBuilder xmlReasons = new XmlBuilder("reasons");


		public XmlErrorHandler(XMLReader parser) {
			this.parser = parser;
		}

		public void addReason(String message, String location) {
			try {
				ContentHandler ch = parser.getContentHandler();
				if (ch!=null && ch instanceof XmlFindingHandler) {
					XmlFindingHandler xfh = (XmlFindingHandler)ch;

					XmlBuilder reason = new XmlBuilder("reason");
					XmlBuilder detail;
				
					detail = new XmlBuilder("message");;
					detail.setCdataValue(message);
					reason.addSubElement(detail);

					detail = new XmlBuilder("elementName");;
					detail.setValue(xfh.getElementName());
					reason.addSubElement(detail);

					detail = new XmlBuilder("xpath");;
					detail.setValue(xfh.getXpath());
					reason.addSubElement(detail);
				
					xmlReasons.addSubElement(reason);	
				}
			} catch (Throwable t) {
				log.error("Exception handling errors",t);
			
				XmlBuilder reason = new XmlBuilder("reason");
				XmlBuilder detail;
				
				detail = new XmlBuilder("message");;
				detail.setCdataValue(t.getMessage());
				reason.addSubElement(detail);

				xmlReasons.addSubElement(reason);	
			}

			if (StringUtils.isNotEmpty(location)) {
				message = location + ": " + message;
			}
			errorOccured = true;
			if (reasons == null) {
				 reasons = message;
			 } else {
				 reasons = reasons + "\n" + message;
			 }
		}
	
		public void addReason(Throwable t) {
			String location=null;
			if (t instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException)t;
				location = "at ("+spe.getLineNumber()+ ","+spe.getColumnNumber()+")";
			}
			addReason(t.getMessage(),location);
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

		public String getXmlReasons() {
		   return xmlReasons.toXML();
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
    
		if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			 StringUtils.isNotEmpty(getSchemaLocation())) &&
			StringUtils.isNotEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix()+"cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
		}
		if (StringUtils.isNotEmpty(getSchemaLocation())) {
			String resolvedLocations = XmlUtils.resolveSchemaLocations(getSchemaLocation());
			log.info(getLogPrefix()+"resolved schemaLocation ["+getSchemaLocation()+"] to ["+resolvedLocations+"]");
			setSchemaLocation(resolvedLocations);
		}
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			URL url = ClassUtils.getResourceURL(this, getNoNamespaceSchemaLocation());
			if (url==null) {
				throw new ConfigurationException(getLogPrefix()+"could not find schema at ["+getNoNamespaceSchemaLocation()+"]");
			}
			String resolvedLocation =url.toExternalForm();
			log.info(getLogPrefix()+"resolved noNamespaceSchemaLocation to ["+resolvedLocation+"]");
			setNoNamespaceSchemaLocation(resolvedLocation);
		}
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix()+"must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}
	}

	protected String handleFailures(XmlErrorHandler xeh, String input, PipeLineSession session, String mainReason, Throwable t) throws SenderException {
	
		String fullReasons=mainReason;
		if (StringUtils.isNotEmpty(xeh.getReasons())) {
			if (StringUtils.isNotEmpty(mainReason)) {
				fullReasons+=":\n"+xeh.getReasons();
			} else {
				fullReasons=xeh.getReasons();
			}
		}
		if (isThrowException()) {
			throw new SenderException(fullReasons, t);
		} else {
			log.warn(fullReasons, t);
			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				log.debug(getLogPrefix() + "storing reasons under sessionKey ["+getReasonSessionKey()+"]");
				session.put(getReasonSessionKey(),fullReasons);
			}
			if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
				log.debug(getLogPrefix() + "storing reasons (in xml format) under sessionKey ["+getXmlReasonSessionKey()+"]");
				session.put(getXmlReasonSessionKey(),xeh.getXmlReasons());
			}
			return input;
		}
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		message = debugSenderInput(correlationID, message);
		try {
			PipeLineSession session=prc.getSession();
			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				log.debug(getLogPrefix()+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
				session.remove(getReasonSessionKey());
			}
	
			if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
				log.debug(getLogPrefix()+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
				session.remove(getXmlReasonSessionKey());
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
					throw new SenderException(getLogPrefix()+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
				}
	
				URL url = ClassUtils.getResourceURL(this, noNamespaceSchemaLocation);
				if (url == null) {
					throw new SenderException(getLogPrefix()+ "cannot retrieve [" + noNamespaceSchemaLocation + "]");
				}
	
				noNamespaceSchemaLocation = url.toExternalForm();
			}
	
			XmlErrorHandler xeh;
			XMLReader parser=null;
			try {
				parser=getParser(schemaLocation,noNamespaceSchemaLocation);
				if (parser==null) {
					throw new SenderException(getLogPrefix()+ "could not obtain parser");
				}
				xeh = new XmlErrorHandler(parser);
				parser.setErrorHandler(xeh);
			} catch (SAXNotRecognizedException e) {
				throw new SenderException(getLogPrefix()+ "parser does not recognize necessary feature", e);
			} catch (SAXNotSupportedException e) {
				throw new SenderException(getLogPrefix()+ "parser does not support necessary feature", e);
			} catch (SAXException e) {
				throw new SenderException(getLogPrefix()+ "error configuring the parser", e);
			}
	
			InputSource is;
			if (isValidateFile()) {
				try {
					is = new InputSource(new InputStreamReader(new FileInputStream(message),getCharset()));
				} catch (FileNotFoundException e) {
					throw new SenderException(getLogPrefix()+"could not find file ["+message+"]",e);
				} catch (UnsupportedEncodingException e) {
					throw new SenderException(getLogPrefix()+"could not use charset ["+getCharset()+"]",e);
				}
			} else {
				is = new Variant(message).asXmlInputSource();
			}
	
			try {
				parser.parse(is);
			 } catch (Exception e) {
			 	String result = handleFailures(xeh,message,session,"", e);
				result = debugSenderOutput(correlationID, result);
				return result;
			}
	
			boolean illegalRoot = StringUtils.isNotEmpty(getRoot()) && 
								!((XmlFindingHandler)parser.getContentHandler()).getRootElementName().equals(getRoot());
			if (illegalRoot) {
				String str = "got xml with root element '"+((XmlFindingHandler)parser.getContentHandler()).getRootElementName()+"' instead of '"+getRoot()+"'";
				xeh.addReason(str,"");
				String result = handleFailures(xeh,message,session,"",null);
				result = debugSenderOutput(correlationID, result);
				return result;
			} 
			boolean isValid = !(xeh.hasErrorOccured());
		
			if (!isValid) { 
				String mainReason = getLogPrefix() + "got invalid xml according to schema [" + Misc.concatStrings(schemaLocation," ",noNamespaceSchemaLocation) + "]";
				String result = handleFailures(xeh,message,session,mainReason,null);
				result = debugSenderOutput(correlationID, result);
				return result;
			}
		} catch(Throwable throwable) {
			debugSenderAbort(correlationID, throwable);
		}
		return debugSenderOutput(correlationID, message);
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

	public boolean isSynchronous() {
		return true;
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

	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		this.xmlReasonSessionKey = xmlReasonSessionKey;
	}
	public String getXmlReasonSessionKey() {
		return xmlReasonSessionKey;
	}

	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

	public void setValidateFile(boolean b) {
		validateFile = b;
	}
	public boolean isValidateFile() {
		return validateFile;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

}
