package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.*;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/** 
* IfMultipart Tester. 
* 
* @author <Sina Sen>
* @since <pre>Feb 28, 2020</pre> 
* @version 1.0 
*/ 
public class IfMultipartTest extends PipeTestBase<IfMultipart>{
    private final String thenForward = "then";
    private final String elseForward = "else";


    private MockHttpServletRequest request;

    @Before
    public void before() throws Exception {
        request  = new MockHttpServletRequest();

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
    }

    @Test
    public void testInputNotHTTPRequest() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("Pipe [IfMultipart under test] expected HttpServletRequest as input, got [String]");
        pipe.doPipe("i am a string not a http req", session);
    }

    @Test
    public void testRequestUsesElseForward() throws Exception {
        PipeForward forw = new PipeForward("custom_else", "random/path");
        pipe.registerForward(forw);
        pipe.setElseForwardName("custom_else");
        assertEquals(pipe.doPipe(request, session).getPipeForward().getName().toString(), "custom_else");
    }

    @Test
    public void testRequestUsesThenForward() throws Exception {
        request.setContentType("multipartofx");
        pipe.setThenForwardName("success");
        assertEquals(pipe.doPipe(request, session).getPipeForward().getName().toString(), "success");
    }

    @Test
    public void testRequestContentTypeWrong() throws Exception {
        exception.expect(PipeRunException.class);
        request.setContentType("aamultipartofx");
        pipe.setThenForwardName("success");
        assertEquals(pipe.doPipe(request, session).getPipeForward().getName().toString(), "success");
    }


    @Test
    public void testCannotFindForward() throws Exception {
        exception.expect(PipeRunException.class);
        pipe.setElseForwardName("elsee");
        PipeRunResult res = pipe.doPipe(request, session);
    }



} 
