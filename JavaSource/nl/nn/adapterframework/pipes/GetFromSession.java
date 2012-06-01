package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Gets the contents of the {@link PipeLineSession} by a key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to retrieve the output message from</td><td>&nbsp;</td></tr>
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
 * @author Johan Verrips
 *
 * @see PipeLineSession
 */

 public class GetFromSession  extends FixedForwardPipe {

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
/**
 * This is where the action takes place. Pipes may only throw a PipeRunException,
 * to be handled by the caller of this object.
 */
public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
	Object result=session.get(getSessionKey());
	
	if (result==null) {
		log.warn(getLogPrefix(session)+"got null value from session under key ["+getSessionKey()+"]");
	} else {
		log.debug(getLogPrefix(session) +"got ["+result.toString()+"] from pipeLineSession under key ["+getSessionKey()+"]");
	}
	
	return new PipeRunResult(getForward(), result);
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * {@link PipeLineSession}
 */
public String getSessionKey() {
	return sessionKey;
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.PipeLineSession
 * 
 * @param newSessionKey String
 */
public void setSessionKey(String newSessionKey) {
	sessionKey = newSessionKey;
}
}
