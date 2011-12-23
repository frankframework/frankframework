/*
 * $Log: SoapWrapperPipe.java,v $
 * Revision 1.6  2011-12-23 16:02:40  europe\m168309
 * added soapBodyStyleSheet attribute
 *
 * Revision 1.5  2011/12/15 10:52:11  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added soapHeaderStyleSheet, removeOutputNamespaces and outputNamespace attribute
 *
 * Revision 1.4  2011/11/30 13:52:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/09/23 11:33:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attributes encodingStyle and serviceNamespace
 *
 * Revision 1.1  2011/09/14 14:14:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe to wrap or unwrap a message from/into a SOAP Envelope.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>wrap</code> or <code>unwrap</code></td><td>wrap</td></tr>
 * <tr><td>{@link #setSoapHeaderSessionKey(String) soapHeaderSessionKey}</td><td>
 * <table> 
 * <tr><td><code>direction=unwrap</code></td><td>name of the session key to store the content of the SOAP Header from the request in</td></tr>
 * <tr><td><code>direction=wrap</code></td><td>name of the session key to retrieve the content of the SOAP Header for the response from. If the attribute soapHeaderStyleSheet is not empty, the attribute soapHeaderStyleSheet precedes this attribute</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEncodingStyle(String) encodingStyle}</td><td>the encodingStyle to be set in the SOAP Header</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceNamespace(String) serviceNamespace}</td><td>the namespace of the message sent. Identifies the service to be called. May be overriden by an actual namespace setting in the message to be sent</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapHeaderStyleSheet(String) soapHeaderStyleSheet}</td><td>(only used when <code>direction=wrap</code>) stylesheet to create the content of the SOAP Header. As input for this stylesheet a dummy xml string is used. Note: outputType=<code>xml</code> and xslt2=<code>true</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapBodyStyleSheet(String) soapBodyStyleSheet}</td><td>(only used when <code>direction=wrap</code>) stylesheet to apply to the input message. Note: outputType=<code>xml</code> and xslt2=<code>true</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveOutputNamespaces(boolean) removeOutputNamespaces}</td><td>(only used when <code>direction=unwrap</code>) when <code>true</code>, namespaces (and prefixes) in the content of the SOAP Body are removed</td><td>false</td></tr>
 * <tr><td>{@link #setOutputNamespace(String) outputNamespace}</td><td>(only used when <code>direction=wrap</code>) when not empty, this namespace is added to the root element in the SOAP Body</td><td>&nbsp;</td></tr>
 * <table> 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class SoapWrapperPipe extends FixedForwardPipe {
	private String direction = "wrap";
	private String soapHeaderSessionKey = null;
	private String encodingStyle = null;
	private String serviceNamespace = null;
	private String soapHeaderStyleSheet = null;
	private String soapBodyStyleSheet = null;
	private boolean removeOutputNamespaces = false;
	private String outputNamespace = null;

	private SoapWrapper soapWrapper = null;

	private TransformerPool soapHeaderTp = null;
	private TransformerPool soapBodyTp = null;
	private TransformerPool removeOutputNamespacesTp = null;
	private TransformerPool outputNamespaceTp = null;

	public void configure() throws ConfigurationException {
		super.configure();
		soapWrapper = SoapWrapper.getInstance();

		if (StringUtils.isNotEmpty(getSoapHeaderStyleSheet())) {
			soapHeaderTp = TransformerPool.configureTransformer0(getLogPrefix(null), null, null, getSoapHeaderStyleSheet(), "xml", false, getParameterList(), true);
		}
		if (StringUtils.isNotEmpty(getSoapBodyStyleSheet())) {
			soapBodyTp = TransformerPool.configureTransformer0(getLogPrefix(null), null, null, getSoapBodyStyleSheet(), "xml", false, getParameterList(), true);
		}
		try {
			if (isRemoveOutputNamespaces()) {
				String removeOutputNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true, false);
				removeOutputNamespacesTp = new TransformerPool(removeOutputNamespaces_xslt);
			}
			if (StringUtils.isNotEmpty(getOutputNamespace())) {
				String outputNamespace_xslt = XmlUtils.makeAddRootNamespaceXslt(getOutputNamespace(), true, false);
				outputNamespaceTp = new TransformerPool(outputNamespace_xslt);
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null) + "cannot create transformer", e);
		}
	}

	public void start() throws PipeStartException {
		super.start();
		if (soapHeaderTp != null) {
			try {
				soapHeaderTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start SOAP Header TransformerPool", e);
			}
		}
		if (soapBodyTp != null) {
			try {
				soapBodyTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start SOAP Body TransformerPool", e);
			}
		}
		if (removeOutputNamespacesTp != null) {
			try {
				removeOutputNamespacesTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Remove Output Namespaces TransformerPool", e);
			}
		}
		if (outputNamespaceTp != null) {
			try {
				outputNamespaceTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Output Namespace TransformerPool", e);
			}
		}
	}
	
	public void stop() {
		super.stop();
		if (soapHeaderTp != null) {
			soapHeaderTp.close();
		}
		if (soapBodyTp != null) {
			soapBodyTp.close();
		}
		if (removeOutputNamespacesTp != null) {
			removeOutputNamespacesTp.close();
		}
		if (outputNamespaceTp != null) {
			outputNamespaceTp.close();
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				String soapHeader = null;
				if (soapHeaderTp != null) {
					ParameterResolutionContext prc = new ParameterResolutionContext("<dummy/>", session);
					Map parameterValues = null;
					if (getParameterList()!=null) {
						parameterValues = prc.getValueMap(getParameterList());
					}
					soapHeader = soapHeaderTp.transform(prc.getInputSource(), parameterValues); 

				} else {
					if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
						soapHeader = (String) session.get(getSoapHeaderSessionKey());
					}
				}

				String payload;
				if (outputNamespaceTp != null) {
					payload = outputNamespaceTp.transform(input.toString(), null, true);
				} else {
					payload = input.toString();
				}
				if (soapBodyTp != null) {
					ParameterResolutionContext prc = new ParameterResolutionContext(payload, session);
					Map parameterValues = null;
					if (getParameterList()!=null) {
						parameterValues = prc.getValueMap(getParameterList());
					}
					payload = soapBodyTp.transform(prc.getInputSource(), parameterValues); 
				}
				
				result = wrapMessage(payload, soapHeader);
			} else {
				result = unwrapMessage(input.toString());
				if (StringUtils.isEmpty(result)) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body is empty or message is not a SOAP Message");
				}
				if (soapWrapper.getFaultCount(input.toString()) > 0) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body contains SOAP Fault");
				}
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					String soapHeader = soapWrapper.getHeader(input.toString());
					session.put(getSoapHeaderSessionKey(), soapHeader);
				}
				if (removeOutputNamespacesTp != null) {
					result = removeOutputNamespacesTp.transform(result, null, true);
				}
			}
		} catch (Throwable t) {
			throw new PipeRunException(this, getLogPrefix(session) + " Unexpected exception during (un)wrapping ", t);
		}
		return new PipeRunResult(getForward(), result);
	}

	protected String unwrapMessage(String messageText) throws DomBuilderException, TransformerException, IOException {
		return soapWrapper.getBody(messageText);
	}

	protected String wrapMessage(String message, String soapHeader) throws DomBuilderException, TransformerException, IOException {
		return soapWrapper.putInEnvelope(message, getEncodingStyle(), getServiceNamespace(), soapHeader);
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String string) {
		direction = string;
	}

	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}

	public void setEncodingStyle(String string) {
		encodingStyle = string;
	}
	public String getEncodingStyle() {
		return encodingStyle;
	}

	public void setServiceNamespace(String string) {
		serviceNamespace = string;
	}
	public String getServiceNamespace() {
		return serviceNamespace;
	}

	public void setSoapHeaderStyleSheet(String string){
		this.soapHeaderStyleSheet = string;
	}
	public String getSoapHeaderStyleSheet() {
		return soapHeaderStyleSheet;
	}

	public void setSoapBodyStyleSheet(String string){
		this.soapBodyStyleSheet = string;
	}
	public String getSoapBodyStyleSheet() {
		return soapBodyStyleSheet;
	}

	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}
	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	public void setOutputNamespace(String string) {
		outputNamespace = string;
	}
	public String getOutputNamespace() {
		return outputNamespace;
	}
}