package nl.nn.adapterframework.core;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The PipeRunResult is a type to store both the result of the processing of a message
 * in {@link IPipe#doPipe(Object, PipeLineSession) doPipe()} as well as the exitState.
 * <br/>
 * <b>Responsibility:</b><br/>
 * <ul><li>keeper of the result of the execution of a <code>Pipe</code></li>
 *     <li>keeper of the forward to be returned to the <code>PipeLine</code></li>
 * </ul><br/>
 * <code>Pipe</code>s return a <code>PipeRunResult</code> with the information
 * as above.
 * @see PipeForward
 * @see nl.nn.adapterframework.pipes.AbstractPipe#doPipe
 * @see nl.nn.adapterframework.pipes.AbstractPipe#findForward
 * @author Johan Verrips
 */
public class PipeRunResult {
	public static final String version="$Id: PipeRunResult.java,v 1.1 2004-02-04 08:36:12 a1909356#db2admin Exp $";

    private PipeForward pipeForward;
    private Object result;
    public PipeRunResult() {
        super();
    }
    public PipeRunResult(PipeForward forward, Object result){
        this.pipeForward=forward;
        this.result=result;
    }
    public PipeForward getPipeForward() {
        return pipeForward;
    }
    public Object getResult() {
        return result;
    }
    public void setPipeForward(PipeForward pipeForward) {
        this.pipeForward = pipeForward;
    }
    public void setResult(Object result) {
        this.result = result;
    }
    /**
     * uses reflection to create the ToString
     */
    public String toString(){
      return ToStringBuilder.reflectionToString(this);
    }
}
