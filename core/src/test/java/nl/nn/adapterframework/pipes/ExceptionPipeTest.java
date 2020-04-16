package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;

import java.nio.channels.Pipe;

import static org.junit.Assert.assertEquals;

/**
 * ExceptionPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 5, 2020</pre>
 */

public class ExceptionPipeTest extends PipeTestBase<ExceptionPipe> {

    @Override
    public ExceptionPipe createPipe() {
        return new ExceptionPipe();
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoesntThrowException() throws Exception {
        pipe.setThrowException(false);
        assertEquals("no exception", pipe.doPipe("no exception", session).getResult());
    }

    @Test
    public void throwsExceptionWithoutMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception: ExceptionPipe under test");
        pipe.setThrowException(true);
        pipe.doPipe("", session);
    }

    @Test
    public void throwsExceptionWithMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception thrown with a custom message");
        pipe.setThrowException(true);
        pipe.doPipe("exception thrown with a custom message", session);
    }



}
