package nl.nn.adapterframework.pipes.test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Always throws an error, for testing purposes.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td><i>none</i></td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td><i>none</i>, effectively</td><td>always</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */
public class ExampleErroneousPipe extends AbstractPipe {
/**
 * CounterPipe constructor comment.
 */
public ExampleErroneousPipe() {
	super();
}
/**
 * @throws a PipeRunException immediately.
 */
public PipeRunResult doPipe(Object input) throws PipeRunException{
	throw new PipeRunException(this, "This exception was deliberatly thrown for testing purposes");
}
}
