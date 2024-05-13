package org.frankframework.filesystem;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.XmlUtils;

public class ExchangeMailListenerTest extends ExchangeMailListenerTestBase {

	@Override
	protected ExchangeMailListener createExchangeMailListener() {
		return new ExchangeMailListener();
	}

	@Test
	public void testExtractMessageWithAttachments() throws Exception {
		String targetFolder="MessageWithAttachments";
		String recipient=mailaddress_fancy;
		String from=recipient;
		String subject="With Attachements";

		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();
		ExchangeMessageReference rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");
		//assertEquals("name","x",fileSystem.getName(file));

		assertTrue(XmlUtils.isWellFormed(message,"email"));
		TestAssertions.assertXpathValueEquals(recipient, message, "/email/recipients/recipient[@type='to']");
		//TestAssertions.assertXpathValueEquals(from, message, "/email/from");
		//TestAssertions.assertXpathValueEquals(subject, message, "/email/subject");
	}

	@Test
	public void testExtractMessageWithNestedAttachments() throws ListenerException {
		String targetFolder="MessageWithNestedAttachments";
		String recipient=mailaddress_fancy;
		String from=recipient;
		String subject="With Attachements";

		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();
		ExchangeMessageReference rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");
		//assertEquals("name","x",fileSystem.getName(file));

		assertTrue(XmlUtils.isWellFormed(message,"email"));
		//TestAssertions.assertXpathValueEquals(recipient, message, "/email/recipients/recipient[@type='to']");
		//TestAssertions.assertXpathValueEquals(from, message, "/email/from");
		//TestAssertions.assertXpathValueEquals(subject, message, "/email/subject");
	}
	@Test
	@Disabled
	public void moveAndCopyToFolders() {
		String baseFolder=basefolder2;
		String targetFolder="FileWithAttachments";
		String processedFolder = "processedFolder";
		String logFolder = "logFolder";
		String recipient=mailaddress;
//		String subfolder="Basic";
//		String filename = "readFile";
//		String contents = "Tekst om te lezen";

		mailListener.setBaseFolder(baseFolder);
		mailListener.setProcessedFolder(processedFolder);
		mailListener.setLogFolder(logFolder);
		mailListener.setCreateFolders(true);
		configureAndOpen(targetFolder,null);

//		if (!folderContainsMessages(subfolder)) {
//			createFile(null, filename, contents);
//			waitForActionToFinish();
//		}

		Map<String,Object> threadContext=new HashMap<>();
		ExchangeMessageReference rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull("no message found", rawMessage);

		PipeLineResult plr = new PipeLineResult();
		plr.setState(ExitState.SUCCESS);
		plr.setResult(new Message("ResultOfPipeline"));
		plr.setExitCode(200);

		mailListener.afterMessageProcessed(plr, rawMessage, threadContext);


	}

}
