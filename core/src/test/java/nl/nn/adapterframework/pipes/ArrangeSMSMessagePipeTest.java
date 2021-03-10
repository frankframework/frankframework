package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;


/**
 * ArrangeSMSMessagePipe Tester.
 *
 * @author Gerrit van Brakel
 */
public class ArrangeSMSMessagePipeTest extends PipeTestBase<ArrangeSMSMessagePipe> {

	@Override
	public ArrangeSMSMessagePipe createPipe() {
		return new ArrangeSMSMessagePipe();
	}


	@Test
	public void testConfigure() throws Exception {
		pipe.configure();
	}

	@Test
	public void testShortMessage() throws Exception {
		pipe.configure();
		pipe.start();
		
		String message ="This is a short message that can be sent in a single SMS message";
		String expected = "<rootTag><result>This is a short message that can be sent in a single SMS message</result></rootTag>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageSoft() throws Exception {
		pipe.setIsSoft(true);
		pipe.configure();
		pipe.start();
		
		String messagepart1 ="This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen";
		String messagepart2 ="eighteen nineteen twenty";
		String message= messagepart1+ " " +messagepart2;
		String expected = "<rootTag><result>"+messagepart1+"</result><result>"+messagepart2+"</result></rootTag>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testLongMessageHard() throws Exception {
		pipe.configure();
		pipe.start();
		
		String messagepart1 ="This is a long message that that will be split up one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eig";
		String messagepart2 ="hteen nineteen twenty";
		String message= messagepart1 +messagepart2;
		String expected = "<rootTag><result>"+messagepart1+"</result><result>"+messagepart2+"</result></rootTag>";
		PipeRunResult prr = doPipe(message);
		assertEquals(expected, prr.getResult().asString());
	}

}
