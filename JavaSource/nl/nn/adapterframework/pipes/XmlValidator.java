/*
 * $Log: XmlValidator.java,v $
 * Revision 1.47  2012-12-06 15:19:28  m00f069
 * Resolved warnings which showed up when using addNamespaceToSchema (src-include.2.1: The targetNamespace of the referenced schema..., src-resolve.4.2: Error resolving component...)
 * Handle includes in XSD's properly when generating a WSDL
 * Removed XSD download (unused and XSD's were not adjusted according to e.g. addNamespaceToSchema)
 * Sort schema's in WSDL (made sure the order is always the same)
 * Indent WSDL with tabs instead of spaces
 * Some cleaning and refactoring (made WSDL generator and XmlValidator share code)
 *
 * Revision 1.46  2012/11/02 10:15:12  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT (isn't used anymore)
 *
 * Revision 1.45  2012/10/26 16:13:38  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Moved *Xmlvalidator*, Schema and SchemasProvider to new validation package
 *
 * Revision 1.44  2012/10/19 09:33:47  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made WsdlXmlValidator extent Xml/SoapValidator to make it use the same validation logic, cleaning XercesXmlValidator on the way
 *
 * Revision 1.43  2012/10/12 16:17:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made (Esb)SoapValidator set SoapNamespace to an empty value, hence validate the SOAP envelope against the SOAP XSD.
 * Made (Esb)SoapValidator check for SOAP Envelope element
 *
 * Revision 1.42  2012/10/01 07:59:29  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Improved messages stored in reasonSessionKey and xmlReasonSessionKey
 * Cleaned XML validation code and documentation a bit.
 *
 * Revision 1.41  2012/09/19 21:40:37  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ignoreUnknownNamespaces attribute
 *
 * Revision 1.40  2012/09/19 09:49:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 * Revision 1.39  2012/09/14 13:35:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Call deprecated getMessageToValidate only when deprecated soapNamespace has been set
 *
 * Revision 1.38  2012/08/23 11:57:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Updates from Michiel
 *
 * Revision 1.37  2012/06/01 10:52:49  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.36  2012/03/30 17:03:45  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added JMS binding/service to WSDL generator, made WSDL validator work for Bis WSDL and made console show syntax problems for schema's used in XmlValidator
 *
 * Revision 1.35  2012/03/19 11:03:20  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed soapNamespace attribute function
 *
 * Revision 1.34  2012/03/16 15:35:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added EsbSoapValidator and WsdlXmlValidator, made WSDL's available for all adapters and did a bugfix on XML Validator where it seems to be dependent on the order of specified XSD's
 *
 * Revision 1.33  2012/03/15 15:45:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added soapNamespace attribute
 *
 * Revision 1.32  2012/03/05 14:39:20  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * for soap messages validate the content of the soap body
 *
 * Revision 1.31  2011/12/15 09:55:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added Ibis WSDL generator (created by Michiel)
 *
 * Revision 1.28  2011/08/22 14:27:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now based on XmlValidatorBase
 *
 * Revision 1.27  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.26  2008/08/06 16:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for flexible monitoring
 *
 * Revision 1.25  2008/02/28 16:24:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved errorMessage
 *
 * Revision 1.24  2008/02/21 12:45:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added option to validate file
 *
 * Revision 1.23  2007/07/19 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * put remark about spaces in javadoc
 *
 * Revision 1.22  2007/07/16 11:34:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fill reason in case of parserError, too
 *
 * Revision 1.21  2007/07/10 08:06:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.20  2007/02/26 13:17:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed non-xml reason setting
 *
 * Revision 1.19  2007/02/05 15:00:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extended diagnostic information
 *
 * Revision 1.18  2006/08/24 09:24:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separated finding wrong root from non-wellformedness;
 * used RootElementFindingHandler in XmlUtils.isWellFormed()
 *
 * Revision 1.17  2006/08/23 14:01:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added root-attribute
 *
 * Revision 1.16  2006/01/05 14:36:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.15  2005/12/29 15:19:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.14  2005/12/28 08:39:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.13  2005/10/24 09:21:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * inmproved logging
 *
 * Revision 1.12  2005/10/17 11:37:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.Schema;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.SchemasProvider;
import nl.nn.adapterframework.validation.XSD;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
*<code>Pipe</code> that validates the input message against a XML-Schema.
*
* <p><b>Configuration:</b>
* <table border="1">
* <tr><th>attributes</th><th>description</th><th>default</th></tr>
* <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
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
* <tr><td>{@link #setSoapNamespace(String) soapNamespace}</td><td>the namespace of the SOAP Envelope, when this property has a value and the input message is a SOAP Message the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate. Please note that this functionality is deprecated, using {@link nl.nn.adapterframework.soap.SoapValidator} is now the preferred solution in case a SOAP Message needs to be validated, in other cases give this property an empty value</td><td>http://schemas.xmlsoap.org/soap/envelope/</td></tr>
* <tr><td>{@link #setIgnoreUnknownNamespaces(boolean) ignoreUnknownNamespaces}</td><td>ignore namespaces in the input message which are unknown</td><td>false when schemaLocation is used, true otherwise</td></tr>
* <tr><td>{@link #setWarn(boolean) warn}</td><td>when set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)</td><td><code>true</code></td></tr>
* </table>
* <p><b>Exits:</b>
* <table border="1">
* <tr><th>state</th><th>condition</th></tr>
* <tr><td>"success"</td><td>default</td></tr>
* <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
* <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
* </table>
* <br>
* N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
* @version Id
* @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)
*/
public class XmlValidator extends FixedForwardPipe implements SchemasProvider {

	protected Logger log = LogUtil.getLogger(this);

	private String soapNamespace = "http://schemas.xmlsoap.org/soap/envelope/";

	protected AbstractXmlValidator validator = new XercesXmlValidator();

	private TransformerPool transformerPoolExtractSoapBody;
	private TransformerPool transformerPoolGetRootNamespace;
	private TransformerPool transformerPoolRemoveNamespaces;

	protected String schemaLocation;
	protected String noNamespaceSchemaLocation;
	protected String schemaSessionKey;

	/**
	 * Configure the XmlValidator
	 * @throws ConfigurationException when:
	 * <ul><li>the schema cannot be found</li>
	 * <ul><li><{@link #isThrowException()} is false and there is no forward defined
	 * for "failure"</li>
	 * <li>when the parser does not accept setting the properties for validating</li>
	 * </ul>
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
				StringUtils.isNotEmpty(getSchemaLocation())) &&
				StringUtils.isNotEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
		}
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}

		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			// Don't use this warning yet as it is used for the IFSA to Tibco
			// migration where an adapter with Tibco listener (with SOAP
			// Envelope and an adapter with IFSA listener (without SOAP Envelop)
			// call an adapter with XmlValidator which should validate both.
			// ConfigurationWarnings.getInstance().add(log, "Using XmlValidator with soapNamespace for Soap validation is deprecated. Please use " + SoapValidator.class.getName());
			String extractNamespaceDefs = "soapenv=" + getSoapNamespace();
			String extractBodyXPath     = "/soapenv:Envelope/soapenv:Body/*";
			try {
				transformerPoolExtractSoapBody = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs, extractBodyXPath, "xml"));
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from getSoapBody", te);
			}
		}

		String getRootNamespace_xslt = XmlUtils.makeGetRootNamespaceXslt();
		try {
			transformerPoolGetRootNamespace = new TransformerPool(getRootNamespace_xslt, true);
		} catch (TransformerConfigurationException te) {
			throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from getRootNamespace", te);
		}

		String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,false);
		try {
			transformerPoolRemoveNamespaces = new TransformerPool(removeNamespaces_xslt);
		} catch (TransformerConfigurationException te) {
			throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from removeNamespaces", te);
		}

		if (!isThrowException()){
			if (findForward("failure")==null) {
				throw new ConfigurationException(getLogPrefix(null)+ "must either set throwException true, or have a forward with name [failure]");
			}
		}

		// Different default value for ignoreUnknownNamespaces when using
		// noNamespaceSchemaLocation.
		if (validator.getIgnoreUnknownNamespaces() == null) {
			if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
				validator.setIgnoreUnknownNamespaces(true);
			} else {
				validator.setIgnoreUnknownNamespaces(false);
			}
		}
		validator.setSchemasProvider(this);
		validator.configure(getLogPrefix(null));
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_NOT_VALID_MONITOR_EVENT);
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
	}

     /**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.IPipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
     @Override
    public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
         String messageToValidate;
         if (StringUtils.isNotEmpty(getSoapNamespace())) {
             messageToValidate = getMessageToValidate(input, session);
         } else {
             messageToValidate = input.toString();
         }
         try {
            PipeForward forward = validate(messageToValidate, session);
			return new PipeRunResult(forward, input);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session), e);
		}

     }
    protected PipeForward validate(String messageToValidate, IPipeLineSession session) throws XmlValidatorException, PipeRunException, ConfigurationException {
        String resultEvent = validator.validate(messageToValidate, session, getLogPrefix(session));
        throwEvent(resultEvent);
        if (AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT.equals(resultEvent)) {
            return getForward();
        }
        PipeForward forward = null;
        if (AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT.equals(resultEvent)) {
            forward = findForward("parserError");
        }
        if (forward == null) {
            forward = findForward("failure");
        }
        if (forward == null) {
            throw new PipeRunException(this, "not implemented: should get reason from validator");
        }
        return forward;
    }

    @Deprecated
     private String getMessageToValidate(Object input, IPipeLineSession session) throws PipeRunException {
    	 String inputStr = input.toString();
    	 if (XmlUtils.isWellFormed(inputStr, "Envelope")) {
     		String inputRootNs;
     		try {
     			inputRootNs = transformerPoolGetRootNamespace.transform(inputStr, null);
 			} catch (Exception e) {
 				throw new PipeRunException(this,"cannot extract root namespace",e);
 			}
      		if (inputRootNs.equals(getSoapNamespace())) {
 				log.debug(getLogPrefix(session)+"message to validate is a SOAP message");
 		    	boolean extractSoapBody = true;
 		    	if (StringUtils.isNotEmpty(getSchemaLocation())) {
 					StringTokenizer st = new StringTokenizer(getSchemaLocation(),", \t\r\n\f");
 					while (st.hasMoreTokens() && extractSoapBody) {
 						if (st.nextToken().equals(getSoapNamespace())) {
 							extractSoapBody = false;
 						}
 					}
 		    	}
 		    	if (extractSoapBody) {
 					log.debug(getLogPrefix(session)+"extract SOAP body for validation");
 					try {
 						inputStr = transformerPoolExtractSoapBody.transform(inputStr,null,true);
 					} catch (Exception e) {
 						throw new PipeRunException(this,"cannot extract SOAP body",e);
 					}
 		    		try {
 		    			inputRootNs = transformerPoolGetRootNamespace.transform(inputStr,null);
 					} catch (Exception e) {
 						throw new PipeRunException(this,"cannot extract root namespace",e);
 					}
 					if (StringUtils.isNotEmpty(inputRootNs) && StringUtils.isEmpty(getSchemaLocation())) {
 						log.debug(getLogPrefix(session)+"remove namespaces from extracted SOAP body");
 			    		try {
 				    		inputStr = transformerPoolRemoveNamespaces.transform(inputStr,null);
 						} catch (Exception e) {
 							throw new PipeRunException(this,"cannot remove namespaces",e);
 						}
 			    	}
 		    	}
      		}
    	 }
    	 return inputStr;
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
        validator.setFullSchemaChecking(fullSchemaChecking);
    }
	public boolean isFullSchemaChecking() {
		return validator.isFullSchemaChecking();
	}

    /**
     * <p>The filename of the schema on the classpath. The filename (which e.g.
     * can contain spaces) is translated to an URI with the
     * ClassUtils.getResourceURL(Object,String) method (e.g. spaces are translated to %20).
     * It is not possible to specify a namespace using this attribute.
     * <p>An example value would be "xml/xsd/GetPartyDetail.xsd"</p>
     * <p>The value of the schema attribute is only used if the schemaLocation
     * attribute and the noNamespaceSchemaLocation are not set</p>
     * @see nl.nn.adapterframework.util.ClassUtils#getResourceURL
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
	 * @deprecated attribute name changed to {@link #setSchemaSessionKey(String) schemaSessionKey}
	 */
	public void setSchemaSession(String schemaSessionKey) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null) + "attribute 'schemaSession' is deprecated. Please use 'schemaSessionKey' instead.";
		configWarnings.add(log, msg);
		setSchemaSessionKey(schemaSessionKey);
	}


    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant.
     */
    public void setThrowException(boolean throwException) {
    	validator.setThrowException(throwException);
    }
	public boolean isThrowException() {
		return validator.isThrowException();
	}

	/**
	 * The sessionkey to store the reasons of misvalidation in.
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		validator.setReasonSessionKey(reasonSessionKey);
	}
	public String getReasonSessionKey() {
		return validator.getReasonSessionKey();
	}

	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		validator.setXmlReasonSessionKey(xmlReasonSessionKey);
	}
	public String getXmlReasonSessionKey() {
		return validator.getXmlReasonSessionKey();
	}

	public void setRoot(String root) {
		validator.setRoot(root);
	}
	public String getRoot() {
		return validator.getRoot();
	}

    /**
     * Not ready yet (namespace not yet correctly parsed)
     *
     */
    public QName getRootTag() {
        return new QName(getSchema()/* TODO*/, getRoot());
    }

	public void setValidateFile(boolean b) {
		validator.setValidateFile(b);
	}
	public boolean isValidateFile() {
		return validator.isValidateFile();
	}

	public void setCharset(String string) {
		validator.setCharset(string);
	}
	public String getCharset() {
		return  validator.getCharset();
	}

    public void setImplementation(Class<AbstractXmlValidator> clazz) throws IllegalAccessException, InstantiationException {
        validator = clazz.newInstance();
    }

    public boolean isAddNamespaceToSchema() {
        return validator.isAddNamespaceToSchema();
    }

    public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
        validator.setAddNamespaceToSchema(addNamespaceToSchema);
    }

    @Deprecated
	public void setSoapNamespace(String string) {
		soapNamespace = string;
    }

    @Deprecated
	public String getSoapNamespace() {
		return soapNamespace;
	}

	public void setWarn(boolean warn) {
        if (validator instanceof AbstractXmlValidator) {
            ((AbstractXmlValidator) validator).setWarn(warn);
        } else {
            throw new UnsupportedOperationException();
        }
    }

	public void setIgnoreUnknownNamespaces(boolean ignoreUnknownNamespaces) {
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
	}

	public boolean getIgnoreUnknownNamespaces() {
		return validator.getIgnoreUnknownNamespaces();
	}

	public String getSchemasId() {
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			return getNoNamespaceSchemaLocation();
		} else if (StringUtils.isNotEmpty(getSchemaLocation())) {
			return getSchemaLocation();
		}
		return null;
	}

	public List<Schema> getSchemas() throws ConfigurationException {
		List<Schema> schemas = new ArrayList<Schema>();
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			schemas.add(
				new Schema() {
					public InputStream getInputStream() throws IOException {
						return ClassUtils.getResourceURL(getNoNamespaceSchemaLocation()).openStream();
					}
					public Reader getReader() throws IOException {
						return null;
					}
					public String getSystemId() {
						return ClassUtils.getResourceURL(getNoNamespaceSchemaLocation()).toExternalForm();
					}
				}
			);
		} else if (StringUtils.isNotEmpty(getSchemaLocation())) {
			if (isAddNamespaceToSchema()) {
				final Map<String, ByteArrayOutputStream> schemaStreams;
				try {
					Set<XSD> xsds = SchemaUtils.getXsds(getSchemaLocation(), null, true, false);
					xsds = SchemaUtils.getXsdsRecursive(xsds);
					Map<String, Set<XSD>> xsdsGroupedByNamespace =
							SchemaUtils.getXsdsGroupedByNamespace(xsds);
					schemaStreams = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(
							xsdsGroupedByNamespace, null);
				} catch(Exception e) {
					throw new ConfigurationException(getLogPrefix(null) + "could not read schema's", e);
				}
				for (final String namespace : schemaStreams.keySet()) {
					schemas.add(
						new Schema() {
							public InputStream getInputStream() throws IOException {
								return new ByteArrayInputStream(schemaStreams.get(namespace).toByteArray());
							}
							public Reader getReader() throws IOException {
								return null;
							}
							public String getSystemId() {
								return null;
							}
						}
					);
				}
			} else {
				StringTokenizer stringTokenizer = new StringTokenizer(getSchemaLocation());
				while (stringTokenizer.hasMoreTokens()) {
					final String namespace = stringTokenizer.nextToken();
					if (stringTokenizer.hasMoreTokens()) {
						final String location = stringTokenizer.nextToken();
						URL url = ClassUtils.getResourceURL(XmlUtils.class, location);
						if (url != null) {
							schemas.add(
								new Schema() {
									public InputStream getInputStream() throws IOException {
										return ClassUtils.getResourceURL(location).openStream();
									}
									public Reader getReader() throws IOException {
										return null;
									}
									public String getSystemId() {
										return ClassUtils.getResourceURL(location).toExternalForm();
									}
								}
							);
						} else {
							throw new ConfigurationException(getLogPrefix(null) + "could not resolve location [" + location + "] for namespace ["+namespace+"] to URL");
						}
					} else {
						log.warn(getLogPrefix(null) + "no location for namespace ["+namespace+"]");
					}
				}
			}
		}
		return schemas;
	}

	public String getSchemasId(IPipeLineSession session) {
		String schemaSessionKey = getSchemaSessionKey();
		if (schemaSessionKey != null) {
			return schemaSessionKey;
		}
		return null;
	}

	public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException {
		List<Schema> schemas = new ArrayList<Schema>();
		String schemaSessionKey = getSchemaSessionKey();
		if (schemaSessionKey != null) {
			String schemaLocation;
			if (session.containsKey(schemaSessionKey)) {
				schemaLocation = session.get(schemaSessionKey).toString();
			} else {
				throw new PipeRunException(null, getLogPrefix(session) + "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
			}
			final URL url = ClassUtils.getResourceURL(schemaLocation);
			if (url == null) {
				throw new PipeRunException(null, getLogPrefix(session) + "could not find schema at [" + schemaLocation + "]");
			}
			schemas.add(
				new Schema() {
					public InputStream getInputStream() throws IOException {
						return url.openStream();
					}
					public Reader getReader() throws IOException {
						return null;
					}
					public String getSystemId() {
						return url.toExternalForm();
					}
				}
			);
			return schemas;
		}
		return null;
	}
}
