package nl.nn.adapterframework.pipes.test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;

public class TestPipe extends AbstractPipe {
/**
 * CounterPipe constructor comment.
 * <p><b>Forwards:</b><br/>
 *  "success" when everything went ok.
 * </p>
 */
public TestPipe() {
  super();
}
/**
 * doPipe method comment.
 * @param input The input for this Pipe, usually the message or result of previous pipes
 * @return PipeRunResult
 */
public PipeRunResult doPipe(Object input) throws PipeRunException{
    log.debug("in "+getName());
    String answer=input.toString();
    answer+=" was in "+getName();

    log.debug( "input is now:"+input)  ;

  return  new PipeRunResult(findForward("success"), answer);
}
}
