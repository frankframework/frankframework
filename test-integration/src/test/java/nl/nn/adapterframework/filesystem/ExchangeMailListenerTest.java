package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.util.TestAssertions;
import nl.nn.adapterframework.util.XmlUtils;

public class ExchangeMailListenerTest extends ExchangeMailListenerTestBase {

	@Override
	protected IExchangeMailListener createExchangeMailListener() {
		return new ExchangeMailListenerWrapper();
	}

	@Test
	public void readFileBasicFromSubfolderOfRoot() throws Exception {
		String baseFolder=basefolder2;
		String targetFolder="FileWithAttachments";
		String recipient="gerrit@integrationpartners.nl";
		String from="gerrit@25bis.nl";
		String subject="With Attachements";
//		String subfolder="Basic";
//		String filename = "readFile";
//		String contents = "Tekst om te lezen";
		
		mailListener.setBaseFolder(baseFolder);
		configureAndOpen(targetFolder,null);
		
//		if (!folderContainsMessages(subfolder)) {
//			createFile(null, filename, contents);
//			waitForActionToFinish();
//		}
		
		Map<String,Object> threadContext=new HashMap<String,Object>();
		Item rawMessage = (Item)mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		String message = mailListener.getStringFromRawMessage(rawMessage, threadContext);
		
		System.out.println("message ["+message+"]");
		//assertEquals("name","x",fileSystem.getName(file));

		assertTrue(XmlUtils.isWellFormed(message,"email"));
		TestAssertions.assertXpathValueEquals(recipient, message, "/email/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(from, message, "/email/from");
		TestAssertions.assertXpathValueEquals(subject, message, "/email/subject");
	}

}
