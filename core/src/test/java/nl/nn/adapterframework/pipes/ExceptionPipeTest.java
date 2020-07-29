package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.stream.Message;
import org.junit.Test;

import static com.ibm.icu.impl.Assert.fail;
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
        Message m = new Message("no exception");

        assertEquals(doPipe(pipe, "no exception", session).getResult().asString(), m.asString());

    }

    @Test
    public void throwsExceptionWithoutMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception: ExceptionPipe under test");
        pipe.setThrowException(true);
        doPipe(pipe, "", session);
        fail("this is expected to fail");
    }

    @Test
    public void throwsExceptionWithMessage() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("exception thrown with a custom message");
        pipe.setThrowException(true);
        doPipe(pipe, "exception thrown with a custom message", session);
        fail("this is expected to fail");

    }



}
