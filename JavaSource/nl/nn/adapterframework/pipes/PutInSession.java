package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Puts the input in the PipeLineSession, under the key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to store the input in</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 *
 * @see PipeLineSession
 */
public class PutInSession extends FixedForwardPipe {
	public static final String version="$Id: PutInSession.java,v 1.1 2004-02-04 08:36:04 a1909356#db2admin Exp $";
	
    private String sessionKey;
	/**
     * checks wether the proper forward is defined.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();

        if (null== getSessionKey()) {
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has a null value for sessionKey");
        }
    }
public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
	session.put(getSessionKey(), input);
	
	log.debug(getLogPrefix(session)+"stored ["+input.toString()+"] in pipeLineSession under key ["+getSessionKey()+"]");

	return new PipeRunResult(getForward(), input);
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public String getSessionKey() {
	return sessionKey;
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public void setSessionKey(String newSessionKey) {
	sessionKey = newSessionKey;
}
}
