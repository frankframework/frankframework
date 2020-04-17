package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

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
        assertEquals("no exception", doPipe(pipe, "no exception", session).getResult());
    }

    @Test
    public void throwsExceptionWithoutMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception: ExceptionPipe under test");
        pipe.setThrowException(true);
        doPipe(pipe, "", session);
    }

    @Test
    public void throwsExceptionWithMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception thrown with a custom message");
        pipe.setThrowException(true);
        doPipe(pipe, "exception thrown with a custom message", session);
    }



}
