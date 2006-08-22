/*
 * $Log: SizePipe.java,v $
 * Revision 1.1  2006-08-22 12:56:32  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Returns the number of bytes or characters in the input.
 *
 * @author Jaco de Groot (***@dynasol.nl)
 * @version Id
 *
 */
public class SizePipe extends FixedForwardPipe {
	public static final String version="$RCSfile: SizePipe.java,v $ $Revision: 1.1 $ $Date: 2006-08-22 12:56:32 $";

	/**
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			int size = -1;
			if (input instanceof String) {
				size = ((String)input).length();
			} else if (input instanceof byte[]) {
				size = ((byte[])input).length;
			}
			return new PipeRunResult(getForward(), "" + size);
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e);
		}
	}

}
