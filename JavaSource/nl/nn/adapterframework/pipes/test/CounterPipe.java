package nl.nn.adapterframework.pipes.test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Counts the number of chars in a String.
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
 * <tr><td>"success"</td><td>always</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */
public class CounterPipe extends AbstractPipe {

public CounterPipe() {
	super();
}
/**
 * Counts the number of chars in a String
 */
public PipeRunResult doPipe(Object input) throws PipeRunException {
    if (!(input instanceof String)) {
        throw new PipeRunException(this, 
            "invalid type, expected String, got " + input.getClass().getName());
    }
    String s = (String) input;
    String r = "" + s.length();
    Object o =  r;
    return new PipeRunResult(findForward("success"),o);

}
}
