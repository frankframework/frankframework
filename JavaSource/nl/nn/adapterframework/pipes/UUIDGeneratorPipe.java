/*
 * $Log: UUIDGeneratorPipe.java,v $
 * Revision 1.6  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 15:01:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/01/12 15:48:39  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2008/12/08 13:12:03  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2008/12/08 13:02:32  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
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
 * If {@link #setType(String) type} is set to <code>alphanumeric</code>, the UUID will not have a fixed length which will be about 42.
 * Only type <code>alphanumeric</code> guarantees a 100% unique identifier, type <code>numeric</code> has a 0.01% chance of exactly the same id in case of multiple calls on the same host within a few milliseconds.  
 * 
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class UUIDGeneratorPipe extends FixedForwardPipe {

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

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
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