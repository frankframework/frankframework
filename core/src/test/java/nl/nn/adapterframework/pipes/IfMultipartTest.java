package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;


import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**

 * IfMultipart Tester.
 *
 * @author <Sina Sen>
 */
public class IfMultipartTest extends PipeTestBase<IfMultipart> {

    private MockHttpServletRequest request;

    @Before
    public void before() throws Exception {
        request = new MockHttpServletRequest();
        MockitoAnnotations.initMocks(this);
    }

    @Override
    public IfMultipart createPipe() {
        return new IfMultipart();
    }

    @Test
    public void testInputNullElseForwardNull() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("Pipe [IfMultipart under test] cannot find forward or pipe named [null]");
        pipe.setElseForwardName(null);
        pipe.doPipe(null, session);
        fail("this is expected to fail");
    }

    @Test
    public void testInputNotHTTPRequest() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("Pipe [IfMultipart under test] expected HttpServletRequest as input, got [Message]");
        doPipe(pipe, "i am a string not a http req", session);
        fail("this is expected to fail");
    }

    @Test
    public void testRequestUsesElseForward() throws Exception {
        PipeForward forw = new PipeForward("custom_else", "random/path");
        pipe.registerForward(forw);
        pipe.setElseForwardName("custom_else");
        PipeRunResult res = doPipe(pipe, request, session);
        PipeForward forward = res.getPipeForward();
        assertEquals("custom_else", forward.getName());
    }

    @Test
    public void testRequestUsesThenForward() throws Exception {
        request.setContentType("multipartofx");
        pipe.setThenForwardName("success");
        PipeRunResult res = doPipe(pipe, request, session);
        PipeForward forward = res.getPipeForward();
        assertEquals("success", forward.getName());
    }

    @Test
    public void testRequestContentTypeWrong() throws Exception {
        exception.expect(PipeRunException.class);
        request.setContentType("aamultipartofx");
        pipe.setThenForwardName("success");
        doPipe(pipe, request, session);
        fail("this is expected to fail");
    }


    @Test
    public void testCannotFindForward() throws Exception {
        exception.expect(PipeRunException.class);
        pipe.setElseForwardName("elsee");
        doPipe(pipe, request, session);
        fail("this is expected to fail");
    }
}
