package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.xml.SaxElementBuilder;

/**
 * Performs a number of test for MailFileSystems.
 *
 * Runs on 'IAF Integration Tests 1' base folder.
 * Subfolders:
 * 	AttachedMessage
 * 	FromAddressProblem
 * 	RaceFolder1
 * 	RaceFolder2
 * 	XmlProblem
 * 	XmlProblem2
 *
 * creates a number of fs_test... folders
 */
@Log4j2
public abstract class MailFileSystemTestBase<M,A,FS extends IMailFileSystem<M, A>> extends SelfContainedBasicFileSystemTest<M, FS>{

	protected String PROPERTY_FILE = "ExchangeMail.properties";

//	protected String username    = PropertyUtil.getProperty(PROPERTY_FILE, "username");
//	protected String password    = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	protected String basefolder1 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder1");
	protected String expectdBestReplyAddress = PropertyUtil.getProperty(PROPERTY_FILE, "bestReplyAddress");



	@Test
	public void fileSystemTestListFileFromInbox() throws Exception {
		fileSystemTestListFile(1, null);
	}

	@Test
	public void fileSystemTestListFileWithXmlProblem() throws Exception {
		fileSystemTestListFile(1, "XmlProblem");
	}

	@Test
	public void testExtractNormalMessage() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormal.xml");
		MatchUtils.assertXmlEquals(expected, xml.toString());
	}

	@Test
	public void testNormalMessageFileSize() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		long actual = fileSystem.getFileSize(emailMessage);
		long expected = 57343;
		assertEquals(expected,actual);
	}

	@Test
	public void testNormalMessageSubject() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		String subject = fileSystem.getSubject(emailMessage);
		String expected = "Re: [frankframework/frankframework] Ladybug randomly stops report generation (#875)";
		assertEquals(expected,subject);
	}

	@Test
	public void testNormalMessageMimeContents() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		Message mimeContent = fileSystem.getMimeContent(emailMessage);
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalMimeContents.txt");
		assertEquals(expected.trim(), mimeContent.asString().replaceAll("\r\n", "\n").trim());
	}

	@Test
	public void testNormalMessageContents() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		Message content = fileSystem.readFile(emailMessage, null);
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalContents.txt");

		expected = expected.replace(">", ">\n");
		String actual = content.asString().replaceAll("\r\n", "\n").replace(">", ">\n");

		assertEquals(expected.trim(), actual.trim());
	}

//	@Test
//	public void testNormalMessageBody() throws Exception {
//		M emailMessage = getFirstFileFromFolder(null);
//		String body = fileSystem.getMessageBody(emailMessage);
//		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalBody.txt");
//		assertEquals(expected.trim(), body.replaceAll("\r\n", "\n").trim());
//	}

	@Test
	public void testToAddress() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		List<String> adresses = (List<String>)properties.get(IMailFileSystem.TO_RECIPIENTS_KEY);
		String address = adresses.get(0);
		String expected = "frankframework/frankframework <iaf@noreply.github.com>";
		assertEquals(expected, address);
	}

	@Test
	public void testFromAddress() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		String address = (String)properties.get(IMailFileSystem.FROM_ADDRESS_KEY);
		String expected = "ricardovh <notifications@github.com>";
		assertEquals(expected, address);
	}

	@Test
	public void testBestReplyAddress1() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		String bestReplyAddress = (String)properties.get(IMailFileSystem.BEST_REPLY_ADDRESS_KEY);
		String expected = "frankframework/frankframework <reply+AA72C6GJP3QDZ2ZAJ4NOTZN5QBAH3EVBNHHCM2YM5Q@reply.github.com>";
		assertEquals(expected, bestReplyAddress);
	}

	@Test
	public void testBestReplyAddress2() throws Exception {
		M emailMessage = getFirstFileFromFolder("XmlProblem");
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		String bestReplyAddress = (String)properties.get(IMailFileSystem.BEST_REPLY_ADDRESS_KEY);
		assertEquals(expectdBestReplyAddress, bestReplyAddress);
	}

//	@Test
//	public void testForward() throws Exception {
//		M emailMessage = getFirstFileFromFolder(null);
//		fileSystem.forwardMail(emailMessage, "xxx&yyy.nl");
//	}


	@Test
	public void testExtractNormalMessageSimple() throws Exception {
		M emailMessage = getFirstFileFromFolder(null);
		SaxElementBuilder xml = new SaxElementBuilder("email");
		MailFileSystemUtils.addEmailInfoSimple(fileSystem, emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalSimple.xml");
		MatchUtils.assertXmlEquals(expected, xml.toString());
	}

	@Test
	public void testExtractProblematicMessage() throws Exception {
		M emailMessage = getFirstFileFromFolder("XmlProblem");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailProblem.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}

	@Test
	public void testExtractProblematicMessage2() throws Exception {
		M emailMessage = getFirstFileFromFolder("XmlProblem2");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailProblem2.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}

	@Test
	public void testExtractMessageWithProblematicAddress() throws Exception {
		M emailMessage = getFirstFileFromFolder("FromAddressProblem");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailFromAddressProblem.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}

	@Test
	public void testExtractMessageWithProblematicAttachement() throws Exception {
		M emailMessage = getFirstFileFromFolder("AttachmentProblem");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailAttachmentProblem.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}

//	@Test
//	public void testExtractMessageWithProblematicAddress2() throws Exception {
//		M emailMessage = getFirstFileFromFolder("FromAddressProblem2");
//		SaxElementBuilder xml = new SaxElementBuilder("email");
//		fileSystem.extractEmail(emailMessage, xml);
//		xml.close();
//		String expected = TestFileUtils.getTestFile("/ExchangeMailFromAddressProblem2.xml");
//		MatchUtils.assertXmlEquals(expected,xml.toString());
//	}
//
//	@Test
//	public void testExtractMessageWithProblematicAddress3() throws Exception {
//		M emailMessage = getFirstFileFromFolder("FromAddressProblem3");
//		SaxElementBuilder xml = new SaxElementBuilder("email");
//		fileSystem.extractEmail(emailMessage, xml);
//		xml.close();
//		String expected = TestFileUtils.getTestFile("/ExchangeMailFromAddressProblem3.xml");
//		MatchUtils.assertXmlEquals(expected,xml.toString());
//	}

	@Test
	public void testExtractMessageWithMessageAttached() throws Exception {
		M emailMessage = getFirstFileFromFolder("AttachedMessage");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailAttachedMessage.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}

	protected M prepareFolderAndGetFirstMessage(String folderName) throws Exception {
		if (!fileSystem.folderExists(folderName)) {
			fileSystem.createFolder(folderName);
		}
		M orgItem = getFirstFileFromFolder(folderName);
		if (orgItem == null) {
			//TODO put message here!
			throw new IllegalStateException("TODO, there is no message!");
		}
		return orgItem;
	}

	@Test
	public void testGetMessageRace() throws Exception {
		String folderName1 = "RaceFolder1";
		String folderName2 = "RaceFolder2";

		M orgItem = prepareFolderAndGetFirstMessage(folderName1);
		System.out.println("Item original ["+fileSystem.getName(orgItem));

		System.out.println("moving item...");
		M movedItem1 = fileSystem.moveFile(orgItem, folderName2, true);
		System.out.println("Item original ["+fileSystem.getName(orgItem));
		System.out.println("Item moved 1  ["+fileSystem.getName(movedItem1));

		System.out.println("tring to move same item again...");
		try {
			M movedItem2 = fileSystem.moveFile(orgItem, folderName2, true);
			System.out.println("Item original ["+fileSystem.getName(orgItem));
			System.out.println("Item moved 1  ["+fileSystem.getName(movedItem1));
			System.out.println("Item moved 1  ["+fileSystem.getName(movedItem2));
			fail("Expected second move to fail");
		} catch (FileSystemException e) {
			log.debug("second move failed as expected", e);
		}
	}



}
