/*
 * $Log: ExceptionPipe.java,v $
 * Revision 1.3  2007-07-10 07:30:07  europe\L190409
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

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Pipe that throws an exception, based on the input message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a PipeRunException is thrown. Otherwise the output is only logged as an error</td><td>true</td></tr>
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

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		String message = (String)input;

		if (isThrowException())
			throw new PipeRunException(this, message);
		else {
			log.error(message);
			return new PipeRunResult(getForward(), input);
		}
	}


	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

}