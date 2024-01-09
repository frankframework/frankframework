package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.senders.EchoSender;
import org.frankframework.senders.SenderSeries;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ForEachAttachmentPipeTest<P extends ForEachAttachmentPipe<F, A, FS>, F, A, FS extends IWithAttachments<F,A>> extends HelperedFileSystemTestBase {

	protected P pipe;

	public abstract P createForEachAttachmentPipe();

	protected IFileSystemWithAttachmentsTestHelper<A> getHelper() {
		return (IFileSystemWithAttachmentsTestHelper<A>)helper;
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		pipe = createForEachAttachmentPipe();
		autowireByName(pipe);
		pipe.registerForward(new PipeForward("success",null));
		SenderSeries series = new SenderSeries();
		series.registerSender(new EchoSender());
		pipe.setSender(series);
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		if (pipe!=null) {
			pipe.stop();
		}
		super.tearDown();
	}

	@Test
	public void forEachAttachmentPipeTestConfigure() throws Exception {
		pipe.configure();
	}

	@Test
	public void forEachAttachmentPipeTestOpen() throws Exception {
		pipe.configure();
		pipe.start();
	}

	@Test
	public void forEachAttachmentPipeTestBasics() throws Exception {
		String filename = "testAttachmentBasics" + FILE1;
		String attachmentName="testAttachmentName";
		String attachmentFileName="testAttachmentFileName";
		String attachmentContentType="testAttachmentContentType";
		String attachmentContents="attachmentContents";
		byte[] attachmentContentsBytes=attachmentContents.getBytes("UTF-8");
		String propname1="propname1";
		String propname2="propname2";
		String propvalue1="propvalue1";
		String propvalue2="propvalue2";

		String expected="<results>\n"+
							"<result item=\"1\">\n"+
							"<attachment name=\"testAttachmentName\" filename=\"testAttachmentFileName\" contentType=\"testAttachmentContentType\" size=\"18\">\n"+
							"	<properties>\n"+
							"		<property name=\"propname1\">propvalue1</property>\n"+
							"		<property name=\"propname2\">propvalue2</property>\n"+
							"	</properties>\n"+
							"</attachment>\n"+
							"</result>\n"+
						"</results>";

		pipe.configure();
		pipe.start();

		createFile(null, filename, "tja");
		A attachment = getHelper().createAttachment(attachmentName, attachmentFileName, attachmentContentType, attachmentContentsBytes);
		getHelper().setProperty(attachment, propname1, propvalue1);
		getHelper().setProperty(attachment, propname2, propvalue2);
		getHelper().addAttachment(null, filename, attachment);
		waitForActionToFinish();

		PipeLineSession session = new PipeLineSession();


		// test
		Message message= new Message(filename);
		PipeRunResult prr = pipe.doPipe(message, session);
		assertNotNull(prr);
		String actual = Message.asString(prr.getResult());
		System.out.println(actual);
		assertEquals(expected, actual);
	}


}
