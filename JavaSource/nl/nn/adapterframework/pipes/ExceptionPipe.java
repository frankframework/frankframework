/*
 * $Log: ExceptionPipe.java,v $
 * Revision 1.9  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.8  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2010/12/30 09:21:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * improved javadoc
 *
 * Revision 1.5  2007/07/24 08:53:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default noThrow message
 *
 * Revision 1.4  2007/07/24 08:06:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added default message
 *
 * Revision 1.3  2007/07/10 07:30:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.2  2007/06/26 09:35:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cleanup exception message
 *
 * Revision 1.1  2007/05/02 11:20:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of ExceptionPipe
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that throws an exception, based on the input message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a PipeRunException is thrown. Otherwise the output is only logged as an error (and no rollback is performed).</td><td>true</td></tr>
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
 * @author  Gerrit van Brakel
 * @version Id
 */

public class ExceptionPipe extends FixedForwardPipe {

	private boolean throwException = true;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String message = (String)input;
		if (StringUtils.isEmpty(message)) {
			message="exception: "+getName();
		}

		if (isThrowException())
			throw new PipeRunException(this, message);
		else {
			log.error(message);
			return new PipeRunResult(getForward(), message);
		}
	}


	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

}