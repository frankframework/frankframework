package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * 
 * IfMultipart Tester.
 *
 * @author <Sina Sen>
 */
public class IfMultipartTest extends PipeTestBase<IfMultipart> {

	private MockHttpServletRequest request;

	@Before
	public void before() {
		request = new MockHttpServletRequest();
		MockitoAnnotations.initMocks(this);
	}

	@Override
	public IfMultipart createPipe() {
		return new IfMultipart();
	}

	@Test
	public void testInputNullElseForwardNull() throws Exception {
		pipe.setElseForwardName(null);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, null, session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [null]"));
	}

	@Test
	public void testInputNotHTTPRequest() throws Exception {
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "i am a string not a http req", session));
		assertThat(e.getMessage(), Matchers.endsWith("expected HttpServletRequest as input, got [Message]"));
	}

	@Test
	public void testRequestUsesElseForward() throws Exception {
		PipeForward forw = new PipeForward("custom_else", "random/path");
		pipe.registerForward(forw);
		pipe.setElseForwardName("custom_else");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, request, session);
		PipeForward forward = res.getPipeForward();
		assertEquals("custom_else", forward.getName());
	}

	@Test
	public void testRequestUsesThenForward() throws Exception {
		request.setContentType("multipartofx");
		pipe.setThenForwardName("success");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, request, session);
		PipeForward forward = res.getPipeForward();
		assertEquals("success", forward.getName());
	}

	@Test
	public void testRequestContentTypeWrong() throws Exception {
		request.setContentType("aamultipartofx");
		pipe.setThenForwardName("success");
		configureAndStartPipe();

		assertThrows(PipeRunException.class, ()->doPipe(pipe, request, session));
	}

	@Test
	public void testCannotFindForward() throws Exception {
		pipe.setElseForwardName("elsee");
		configureAndStartPipe();

		assertThrows(PipeRunException.class, ()->doPipe(pipe, request, session));
	}
}
