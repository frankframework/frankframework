package nl.nn.adapterframework.pipes.test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;

/**
 * Delays the current thread for a specified period of time, which
 * defaults to 3000 milliseconds.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSleepDelay(long) sleepDelay}</td><td>the time in milliseconds the Pipe will wait before continuing</td><td>3000</td></tr>
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
public class DelayPipe extends AbstractPipe {
    private long sleepDelay = 3000; //defaults to 3000 milliseconds

    public DelayPipe() {
        super();
    }
    /**
     * waits {@link #setSleepDelay(long) sleepDelay} seconds before returning the inputmessage
     */
    public PipeRunResult doPipe(Object input) throws PipeRunException {
        log.info(
            "Pipe [" + this.getName() + "] will sleep for " + sleepDelay + " milliseconds");
        try {
            Thread.currentThread().sleep(sleepDelay);
        } catch (InterruptedException e) {
            throw new PipeRunException(this, "Interrupt caught", e);
        }

        return new PipeRunResult(findForward("success"), input);
    }
    /**
     * sets the time in milliseconds the Pipe will wait before continuing.
     *
     * defaults to 3000 ms.
     */
    public void setSleepDelay(long delay) {
        sleepDelay = delay;
    }
}
