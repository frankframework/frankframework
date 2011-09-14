/*
 * $Log: SoapWrapperPipe.java,v $
 * Revision 1.1  2011-09-14 14:14:01  europe\m168309
 * first version
 *
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.DomBuilderException;

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
 * <tr><td><code>direction=unwrap</code></td><td>name of the session key to store the SOAP header from the request in</td></tr>
 * <tr><td><code>direction=wrap</code></td><td>name of the session key to retrieve the SOAP header for the response from</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
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
	private String soapHeaderSessionKey;

	private SoapWrapper soapWrapper = null;

	public void configure() throws ConfigurationException {
		super.configure();
		soapWrapper = SoapWrapper.getInstance();
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				String soapHeader = null;
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					soapHeader = (String) session.get(getSoapHeaderSessionKey());
				}
				result = wrapMessage(input.toString(), soapHeader);
			} else {
				result = unwrapMessage(input.toString());
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					String soapHeader = soapWrapper.getHeader(input.toString());
					session.put(getSoapHeaderSessionKey(), soapHeader);
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
		return soapWrapper.putInEnvelope(message, null, null, soapHeader);
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}

	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}
}