/*
 * $Log: FileSender.java,v $
 * Revision 1.1  2012-10-05 15:45:31  m00f069
 * Introduced FileSender which is similar to FilePipe but can be used as a Sender (making is possible to have a MessageLog)
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileHandler;

/**
 * <p>See {@link FileHandler}</p>
 * 
 * @author Jaco de Groot
 */
public class FileSender extends FileHandler implements ISenderWithParameters {
	private String name;

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		try {
			return handle(message, prc.getSession());
		} catch(Exception e) {
			throw new SenderException(e); 
		}
	}

	public String sendMessage(String correlationID, String message)
			throws SenderException, TimeOutException {
		throw new SenderException("FileSender cannot be used without a session"); 
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public void addParameter(Parameter p) {
	}

}
