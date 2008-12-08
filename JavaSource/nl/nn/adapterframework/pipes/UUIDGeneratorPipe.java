/*
 * $Log: UUIDGeneratorPipe.java,v $
 * Revision 1.1  2008-12-08 13:02:32  m168309
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

/**
 * Pipe that generates an UUID (Universally Unique Identifier).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>either <code>alphanumeric</code> or <code>numeric</code></td><td>alphanumeric</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * If {@link #setType(String) type} is set to <code>numeric</code>, a UUID with fixed length 31 will be generated.
 * If {@link #setType(String) type} is set to <code>alphanumeric</code>, the UUID will not have a fixed length and will be about 42.
 * 
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class UUIDGeneratorPipe extends FixedForwardPipe {
	public static final String version = "$Id: UUIDGeneratorPipe.java,v 1.1 2008-12-08 13:02:32 m168309 Exp $";

	private String type = "alphanumeric";

	public void configure() throws ConfigurationException {
		super.configure();
		String uType = getType();
		if (uType == null) {
			throw new ConfigurationException(
				getLogPrefix(null) + "type must be set");
		}
		if (!uType.equalsIgnoreCase("alphanumeric")
			&& !uType.equalsIgnoreCase("numeric")) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "illegal value for type ["
					+ uType
					+ "], must be 'alphanumeric' or 'numeric'");
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		String result = null;
		if ("alphanumeric".equalsIgnoreCase(getType())) {
			result = Misc.createUUID();
		} else {
			result = Misc.createNumericUUID();
		}

		return new PipeRunResult(getForward(), result);
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}