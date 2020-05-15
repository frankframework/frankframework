package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ExceptionPipe Tester.
 *
 * @author <Sina Sen>
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
        assertEquals("String: no exception", doPipe(pipe, "no exception", session).getResult());
    }

    @Test
    public void throwsExceptionWithoutMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception: ExceptionPipe under test");
        pipe.setThrowException(true);
        PipeRunResult res = doPipe(pipe, "", session);
        assertTrue(!res.getPipeForward().getName().isEmpty());
    }

    @Test
    public void throwsExceptionWithMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception thrown with a custom message");
        pipe.setThrowException(true);
        PipeRunResult res  = doPipe(pipe, "exception thrown with a custom message", session);
        assertTrue(!res.getPipeForward().getName().isEmpty());

    }



}
