package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;

/**
 * Provides provides a base-class for a Pipe that has always the same forward.
 * Ancestor classes should call <code>super.configure()</code> in their <code>configure()</code>-methods.
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
 * <p>$Id: FixedForwardPipe.java,v 1.2 2004-02-04 10:01:56 a1909356#db2admin Exp $</p>
 * @author Gerrit van Brakel
 */
public class FixedForwardPipe extends AbstractPipe {
 	public static final String version="$Id: FixedForwardPipe.java,v 1.2 2004-02-04 10:01:56 a1909356#db2admin Exp $";

    private String forwardName = "success";
    private PipeForward forward;
    /**
     * checks for correct configuration of forward
     */
    public void configure() throws ConfigurationException {
        forward = findForward(forwardName);
        if (forward == null)
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has no forward with name [" + forwardName + "]");
    }
protected PipeForward getForward() {
	return forward;
}
    public String getForwardName() {
        return forwardName;
    }
 	/**
 	 * Sets the name of the <code>forward</code> that is looked up
 	 * upon completion.
 	 */ 
	public void setForwardName(String forwardName) {
        this.forwardName = forwardName;
    }
}
