/*
 * $Log: PutInSession.java,v $
 * Revision 1.7  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2005/10/27 09:47:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added value attribute
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
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
 * <tr><td>{@link #setValue(String) value}</td><td>The value to store the in the <code>PipeLineSession</code>. If not set, the input of the pipe is stored</td><td>&nbsp;</td></tr>
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
public class PutInSession extends FixedForwardPipe {
	public static final String version = "$RCSfile: PutInSession.java,v $ $Revision: 1.7 $ $Date: 2012-06-01 10:52:50 $";
	
    private String sessionKey;
	private String value;
	
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
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		Object v; 
		if (getValue() == null) {
			v = input;
		} else {
			v = value;
		}
		session.put(getSessionKey(), v);
		log.debug(getLogPrefix(session)+"stored ["+v.toString()+"] in pipeLineSession under key ["+getSessionKey()+"]");
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

	/**
	 * The value to store the in the <code>PipeLineSession</code>
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}	
	
}
