/*
 * $Log: RemoveFromSession.java,v $
 * Revision 1.5  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 15:01:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2009/11/11 10:07:03  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * set attribute preserveInput default to true (instead of false)
 *
 * Revision 1.2  2009/11/09 08:28:02  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * facility to remove multiple keys and facility to use input message
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Removes a key specified by <code>{@link #setSessionKey(String) sessionKey}</code>
 * from the {@link PipeLineSession}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to remove. If this key is empty the input message is interpretted as key. For multiple keys use ',' as delimiter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the result of this pipe will be the same as the input. Otherwise the content of the removed key is returned (and '[null]' when the key is not found)</td><td>true</td></tr>
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
 *
 * @see PipeLineSession
 */

 public class RemoveFromSession  extends FixedForwardPipe {
    private String sessionKey;

	public RemoveFromSession() {
		super.setPreserveInput(true);
	}
    
	/**
     * checks wether the proper forward is defined.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();

	/*
        if (null== getSessionKey()) {
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has a null value for sessionKey");
        }
	*/
    }
/**
 * This is where the action takes place. Pipes may only throw a PipeRunException,
 * to be handled by the caller of this object.
 */
public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
	String result = null;

	String sessionKeys = getSessionKey();
	if (StringUtils.isEmpty(sessionKeys)) {
		sessionKeys = (String)input;
	}
	if (StringUtils.isEmpty(sessionKeys)) {
		log.warn(getLogPrefix(session)+"no key specified");
		result="[null]";
	} else {
		StringTokenizer st = new StringTokenizer(sessionKeys, ",");
		while (st.hasMoreElements()) {
			String sk = st.nextToken();
			Object skResult = session.remove(sk);
			if (skResult==null) {
				log.warn(getLogPrefix(session)+"key ["+sk+"] not found");
				skResult="[null]";
			} else {
				log.debug(getLogPrefix(session) +"key ["+sk+"] removed");
			}
			if (result == null) {
				result = (String)skResult;
			} else {
				result = result + "," + skResult;
			}
		}
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
