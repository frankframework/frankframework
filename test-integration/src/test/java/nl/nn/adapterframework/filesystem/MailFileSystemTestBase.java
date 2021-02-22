package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.PropertyUtil;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.xml.SaxElementBuilder;

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
public abstract class MailFileSystemTestBase<M,A,FS> extends SelfContainedBasicFileSystemTest<M, IMailFileSystem<M,A>>{

	protected String PROPERTY_FILE = "ExchangeMail.properties";
	
	protected String username    = PropertyUtil.getProperty(PROPERTY_FILE, "username");
	protected String password    = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	protected String basefolder1 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder1");
	
	

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
		String expected = "Re: [ibissource/iaf] Ladybug randomly stops report generation (#875)";
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
		Message content = fileSystem.readFile(emailMessage);
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalContents.txt");
		assertEquals(expected.trim(), content.asString().replaceAll("\r\n", "\n").trim());
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
		List<String> adresses = (List<String>)properties.get(IMailFileSystem.TO_RECEPIENTS_KEY);
		String address = adresses.get(0);
		String expected = "ibissource/iaf <iaf@noreply.github.com>";
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
		String expected = "ibissource/iaf <reply+AA72C6GJP3QDZ2ZAJ4NOTZN5QBAH3EVBNHHCM2YM5Q@reply.github.com>";
		assertEquals(expected, bestReplyAddress);
	}

	@Test
	public void testBestReplyAddress2() throws Exception {
		M emailMessage = getFirstFileFromFolder("XmlProblem");
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		String bestReplyAddress = (String)properties.get(IMailFileSystem.BEST_REPLY_ADDRESS_KEY);
		String expected = "\"Brakel, G. van (Gerrit)\" <Gerrit.van.Brakel@nn-group.com>";
		assertEquals(expected, bestReplyAddress);
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
	
	protected M prepareFolderAndGetFirstMessage(String folderName, String sourceFolder) throws Exception {
		if (!fileSystem.folderExists(folderName)) {
			fileSystem.createFolder(folderName);
		}
		M orgItem = getFirstFileFromFolder(folderName);
		if (orgItem == null) {
			M seedItem = getFirstFileFromFolder(sourceFolder);
			orgItem = fileSystem.copyFile(seedItem, folderName, false);
		}
		return orgItem;
	}
	
	@Test
	public void testGetMessageRace() throws Exception {
		String folderName1 = "RaceFolder1";
		String folderName2 = "RaceFolder2";

		M orgItem = prepareFolderAndGetFirstMessage(folderName1, null);
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
