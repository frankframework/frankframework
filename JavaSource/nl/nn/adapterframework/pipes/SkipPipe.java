/*
 * $Log: SkipPipe.java,v $
 * Revision 1.1  2006-08-22 12:56:32  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Skip a number of bytes or characters from the input. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSkip(int) skip}</td><td>number of bytes (for byte array input) or characters (for String input) to skip. An empty byte array or String is returned when skip is larger then the length of the input</td><td>0</td></tr>
 * </table>
 * </p>
 * 
 * @author Jaco de Groot (***@dynasol.nl)
 * @version Id
 *
 */
public class SkipPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: SkipPipe.java,v $ $Revision: 1.1 $ $Date: 2006-08-22 12:56:32 $";
	protected int skip = 0;
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			Object result = "SkipPipe doesn't work for this type of object";
			if (input instanceof String) {
				String stringInput = (String)input;
				if (skip > stringInput.length()) {
					result = "";
				} else {
					result = stringInput.substring(skip);
				}
			} else if (input instanceof byte[]) {
				byte[] bytesInput = (byte[])input;
				byte[] bytesResult;
				if (skip > bytesInput.length) {
					bytesResult = new byte[0];
				} else {
					bytesResult = new byte[bytesInput.length - skip];
					for (int i = 0; i < bytesResult.length; i++) {
						bytesResult[i] = bytesInput[skip + i];
					}
				}
				result = bytesResult;
			}
			return new PipeRunResult(getForward(), result);
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e); 
		}
	}

	/**
	 * @param skip  the number of bytes to skip
	 */
	public void setSkip(int skip) {
		this.skip = skip;
	}

}
