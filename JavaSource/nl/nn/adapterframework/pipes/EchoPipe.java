/*
 * $Log: EchoPipe.java,v $
 * Revision 1.2  2004-10-05 10:50:25  L190409
 * removed unused imports
 *
 * Revision 1.1  2004/08/10 09:19:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Returns simply the input message.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
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
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public class EchoPipe extends FixedForwardPipe {
	public static final String version="$Id: EchoPipe.java,v 1.2 2004-10-05 10:50:25 L190409 Exp $";
	
	public PipeRunResult doPipe (Object input, PipeLineSession session) {
		return new PipeRunResult(getForward(),input);
	}

}
