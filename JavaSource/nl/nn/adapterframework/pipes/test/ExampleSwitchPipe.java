package nl.nn.adapterframework.pipes.test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Example of a Pipe returning different exitStates.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMaxValue(int) maxValue}</td><td>the number of times doPipe has to have been called before returning 'greater' instead of 'success'</td><td>4</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>number of messages processed less or equal than {@link #setMaxValue(int) maxValue}</td></tr>
 * <tr><td>"greater"</td><td>number of messages processed greater than {@link #setMaxValue(int) maxValue}</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */

public class ExampleSwitchPipe extends AbstractPipe {
    static int counter=1;
    int maxValue=4;
    public ExampleSwitchPipe() {
  super();
}
/**
 * return result and message about then number of times method has been called.
 */
public PipeRunResult doPipe(Object input) throws PipeRunException{
    String result=(String)input;
    result+="in SwitchPipe for "+counter+"time, max "+maxValue;
    if (counter >maxValue) {
        return new PipeRunResult(findForward("greater"), result);

    }
    counter +=1;
  return new PipeRunResult(findForward("success"), result);
}
/**
 * set the number of times doPipe has to have been called before returning 'greater' instead of 'success'
 */
public void setMaxValue(int value){
    maxValue=value;
}
}
