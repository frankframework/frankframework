package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class JsonValidatorTest extends PipeTestBase<JsonValidator>{

	@Override
	public JsonValidator createPipe() {
		return new JsonValidator();
	}

	@Test
	public void basic() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/family-compact.json");
		PipeRunResult result = doPipe(input);

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@Test
	public void basicInvalid() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = "{}";
		PipeRunResult result = doPipe(input);

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("The object must have a property whose name is \"members\""));
	}

	@Test
	public void basicNullInput() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = null;
		PipeRunResult result = doPipe(input);

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("The object must have a property whose name is \"members\""));
	}

	@Test
	public void parserError() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		pipe.addForward(new PipeForward("failure", null));
		pipe.addForward(new PipeForward("parserError", null));
		configureAndStartPipe();

		String input = "{asdf";
		PipeRunResult result = doPipe(input);

		assertEquals("parserError", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("[Unexpected char 97 at (line no=1, column no=2, offset=1)]"));
	}

	@Test
	public void basicWithRootElementSpecified() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/family-compact.json");
		PipeRunResult result = pipe.validate(new Message(input), session, "family");

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@Test
	public void overrideRootElement() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Align/FamilyTree/address.json");
		PipeRunResult result = pipe.validate(new Message(input), session, "address");

		assertEquals("success", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());
	}

	@Test
	public void overrideRootElementInvalid() throws Exception {
		pipe.setSchema("/Align/FamilyTree/family-compact-family.jsd");
		pipe.setRoot("address");
		pipe.addForward(new PipeForward("failure", null));
		configureAndStartPipe();

		String input = "{}";
		PipeRunResult result = pipe.validate(new Message(input), session, "address");

		assertEquals("failure", result.getPipeForward().getName());
		assertEquals(input, result.getResult().asString());

		String reason = (String)session.get("failureReason");
		assertThat(reason, containsString("The object must have a property whose name is \"street\""));
	}


}
