package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.XmlUtils;

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
		
		Map<String,Object> threadContext=new HashMap<String,Object>();
		EmailMessage rawMessage = mailListener.getRawMessage(threadContext);
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
	public void testExtractMessageWithNestedAttachments() throws Exception {
		String targetFolder="MessageWithNestedAttachments";
		String recipient=mailaddress_fancy;
		String from=recipient;
		String subject="With Attachements";
		
		configureAndOpen(targetFolder,null);
		
		Map<String,Object> threadContext=new HashMap<String,Object>();
		EmailMessage rawMessage = mailListener.getRawMessage(threadContext);
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
	@Ignore
	public void moveAndCopyToFolders() throws Exception {
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
		
		Map<String,Object> threadContext=new HashMap<String,Object>();
		Item rawMessage = (Item)mailListener.getRawMessage(threadContext);
		assertNotNull("no message found", rawMessage);

		PipeLineResult plr = new PipeLineResult();
		plr.setState(PipeLineExit.EXIT_STATE_SUCCESS);
		plr.setResult(new Message("ResultOfPipeline"));
		plr.setExitCode(200);
		
		mailListener.afterMessageProcessed(plr, rawMessage, threadContext);

		
	}

}
