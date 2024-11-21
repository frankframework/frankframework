package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.core.ProcessState;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.MessageBrowsingFilter;

@Log4j2
public abstract class ExchangeMailListenerTestBase extends ExchangeFileSystemTest {
	private final String PROPERTY_FILE = "ExchangeMailListener.properties";

	private final String ndrMessageId = PropertyUtil.getProperty(PROPERTY_FILE, "ndrMessageId");
	private final String mainFrom = PropertyUtil.getProperty(PROPERTY_FILE, "mainFrom");

	protected String mailaddress;
	protected String mailaddress_fancy;
//	protected String accessToken;
	protected String baseurl     = "https://outlook.office365.com/EWS/Exchange.asmx"; // leave empty to use autodiscovery

	protected String recipient;

	private final String testProperties = "ExchangeMail.properties";

	protected String basefolder1;
//	protected String basefolder2;

	private String senderSmtpHost;
	private int senderSmtpPort;
	private boolean senderSsl;
	private String senderUserId;
	private String senderPassword;
//	private String sendGridApiKey;

//	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";

	protected ExchangeMailListener mailListener;

//	@Override
//	protected IFileSystemTestHelper getFileSystemTestHelper() {
//		return new MailSendingTestHelper(mailaddress,senderSmtpHost,senderSmtpPort, senderSsl, senderUserId, senderPassword);
//	}

	@Override
	public void setUp() throws FileSystemException, ConfigurationException, IOException {
		Properties properties=new Properties();
		properties.load(ClassLoaderUtils.getResourceURL(testProperties).openStream());
		mailaddress = properties.getProperty("mailaddress");
		mailaddress_fancy = properties.getProperty("mailaddress.fancy");
		recipient=  properties.getProperty("recipient");
		basefolder1 = properties.getProperty("basefolder1");
//		basefolder2 = properties.getProperty("basefolder2");
		senderSmtpHost = properties.getProperty("senderSmtpHost");
		senderSmtpPort = Integer.parseInt(properties.getProperty("senderSmtpPort"));
		senderSsl      = Boolean.parseBoolean(properties.getProperty("mailaddress"));
		senderUserId   = properties.getProperty("senderUserId");
		senderPassword = properties.getProperty("senderPassword");
		super.setUp();
		mailListener=createExchangeMailListener();
		mailListener.setMailAddress(mailaddress);
		mailListener.setMailAddress(mailaddress);
		mailListener.setBaseFolder(basefolder1);
//		mailListener.setClientId(client_id);
//		mailListener.setClientSecret(client_secr);
//		mailListener.setTenantId(tenantId);
		mailListener.setUrl(baseurl);
		mailListener.setBaseFolder(basefolder1);
	}

	@AfterEach
	@Override
	public void tearDown() {
		if (mailListener!=null) {
			mailListener.stop();
		}
		super.tearDown();
	}


	public void configureAndOpen(String folder, String filter) throws ConfigurationException, ListenerException {
		if (folder!=null) mailListener.setInputFolder(folder);
		if (filter!=null) mailListener.setFilter(filter);
		mailListener.configure();
		mailListener.start();
	}

	protected abstract ExchangeMailListener createExchangeMailListener();


//	public boolean folderContainsMessages(String subfolder) throws FileSystemException {
//		Iterator<Item> fileIt = fileSystem.listFiles(subfolder);
//		return fileIt!=null && fileIt.hasNext();
//	}
//
//	public void displayItemSummary(Item item) throws FileSystemException, IOException {
//		Map<String,Object> properties=fileSystem.getAdditionalFileProperties(item);
//		System.out.println("from ["+properties.get("from")+"] subject ["+properties.get("subject")+"]");
//	}
//
//	public void displayItem(Item item) throws FileSystemException, IOException {
//		System.out.println("item ["+ToStringBuilder.reflectionToString(item,ToStringStyle.MULTI_LINE_STYLE)+"]");
//		String contents = Misc.streamToString(fileSystem.readFile(item));
//		//System.out.println("message contents:\n"+contents+"\n-------------------------------\n");
//		Map<String,Object> properties=fileSystem.getAdditionalFileProperties(item);
//		if (properties==null) {
//			System.out.println("no message properties");
//		} else {
//			System.out.println("-- message properties --:");
//			for(Entry<String,Object> entry:properties.entrySet()) {
//				Object value=entry.getValue();
//				if (value instanceof Map) {
//					for(Entry<String,Object> subentry:((Map<String,Object>)value).entrySet()) {
//						System.out.println("property Map ["+entry.getKey()+"] ["+subentry.getKey()+"]=["+subentry.getValue()+"]");
//					}
//				} else {
//					System.out.println("property ["+entry.getValue().getClass().getName()+"] ["+entry.getKey()+"]=["+entry.getValue()+"]");
//				}
//			}
//		}
//
//	}
//
//	public void displayAttachment(Attachment attachment) throws Exception {
//		Map<String,Object> properties=fileSystem.getAdditionalAttachmentProperties(attachment);
//		System.out.println("-- attachment ("+attachment.getClass().getName()+") --:");
//		if (properties==null) {
//			System.out.println("-- no attachment properties --");
//		} else {
//			System.out.println("-- attachment properties --:");
//			for(Entry<String,Object> entry:properties.entrySet()) {
//				Object value=entry.getValue();
//				if (value instanceof Map) {
//					for(Entry<String,Object> subentry:((Map<String,Object>)value).entrySet()) {
//						System.out.println("property Map ["+entry.getKey()+"] ["+subentry.getKey()+"]=["+subentry.getValue()+"]");
//					}
//				} else {
//					System.out.println("property ["+(entry.getValue()==null?"null":entry.getValue().getClass().getName())+"] ["+entry.getKey()+"]=["+entry.getValue()+"]");
//				}
//			}
//		}
//		System.out.println("-- attachment payload --:");
//		if (attachment instanceof ItemAttachment) {
//			ItemAttachment itemAttachment=(ItemAttachment)attachment;
//			itemAttachment.load();
//			Item attachmentItem = itemAttachment.getItem();
//			System.out.println("ItemAttachment.item ["+ToStringBuilder.reflectionToString(attachmentItem,ToStringStyle.MULTI_LINE_STYLE)+"]");
//
//			ExtendedPropertyCollection extendedProperties =attachmentItem.getExtendedProperties();
//			for (Iterator<ExtendedProperty> it=extendedProperties.iterator();it.hasNext();) {
//				ExtendedProperty ep = it.next();
//				System.out.println("ExtendedProperty ["+ToStringBuilder.reflectionToString(ep,ToStringStyle.MULTI_LINE_STYLE)+"]");
//			}
//
////			InternetMessageHeaderCollection internetMessageHeaders =attachmentItem.getInternetMessageHeaders();
////			for (Iterator<InternetMessageHeader> it=internetMessageHeaders.iterator();it.hasNext();) {
////				InternetMessageHeader imh = it.next();
////				System.out.println("InternetMessageHeader ["+imh.getName()+"]=["+imh.getValue()+"]");
//////				System.out.println("InternetMessageHeader ["+ToStringBuilder.reflectionToString(imh,ToStringStyle.MULTI_LINE_STYLE)+"]");
////			}
//			String body =MessageBody.getStringFromMessageBody(attachmentItem.getBody());
//			System.out.println("ItemAttachment.item.body ["+body+"]");
//			System.out.println("attachmentItem.getHasAttachments ["+attachmentItem.getHasAttachments()+"]");
//
//			AttachmentCollection attachments = attachmentItem.getAttachments();
//			if (attachments!=null) {
//				for (Iterator<Attachment> it=attachments.iterator(); it.hasNext();) {
//					Attachment subAttachment = it.next();
//					displayAttachment(subAttachment);
//				}
//
//			}
//		}
//		if (attachment instanceof FileAttachment) {
//			FileAttachment fileAttachment=(FileAttachment)attachment;
//			fileAttachment.load();
//			System.out.println("fileAttachment.contentType ["+fileAttachment.getContentType()+"]");
//			System.out.println("fileAttachment.name ["+fileAttachment.getName()+"]");
//			System.out.println("fileAttachment.filename ["+fileAttachment.getFileName()+"]");
//			//System.out.println("fileAttachment ["+ToStringBuilder.reflectionToString(fileAttachment,ToStringStyle.MULTI_LINE_STYLE)+"]");
//
//			System.out.println(new String(fileAttachment.getContent(),"UTF-8"));
//		}
//
////		System.out.println(Misc.streamToString(fileSystem.readAttachment(attachment)));
////		System.out.println(ToStringBuilder.reflectionToString(attachment));
//
//	}

//	@Test
//	public void listFiles() throws Exception {
//		String subfolder="Basic";
//		String filename = "readFile";
//		String contents = "Tekst om te lezen";
//		configureAndOpen(null,null);
//
//		Iterator<Item> fileIt = fileSystem.listFiles(subfolder);
//		assertNotNull(fileIt);
//		while(fileIt.hasNext()) {
//			Item item=fileIt.next();
//			displayItemSummary(item);
//		}
//	}
//
//	public Item findMessageBySubject(String folder, String targetSubject) throws FileSystemException, IOException {
//		log.debug("searching in folder ["+folder+"] for message with subject ["+targetSubject+"]");
//		Iterator<Item> fileIt = fileSystem.listFiles(folder);
//		if (fileIt==null || !fileIt.hasNext()) {
//			log.debug("no files found in folder ["+folder+"]");
//			return null;
//		}
//		while(fileIt.hasNext()) {
//			Item item=fileIt.next();
//			Map<String, Object> properties=fileSystem.getAdditionalFileProperties(item);
//			String subject=(String)properties.get("subject");
//			log.debug("found message with subject ["+subject+"]");
//			if (properties!=null && subject.equals(targetSubject)) {
//				return item;
//			}
//			//displayItem(item);
//		}
//		log.debug("message with subject ["+targetSubject+"] not found in folder ["+folder+"]");
//		return null;
//	}
//

	@Test
	@Disabled("skip NDR filter for now")
	public void readFileBounce1() throws Exception {
		String targetFolder="Onbestelbaar 1";
		String originalRecipient="onbestelbaar@weetikwaarwelniet.nl";
		String originalFrom="";
		String originalSubject="onbestelbaar met attachments";
		String originalMessageId="<AM0PR02MB3732B19ECFCCFA4DF3499604AAEE0@AM0PR02MB3732.eurprd02.prod.outlook.com>";
		int    originalAttachmentCount=1;

		String mainRecipient=originalRecipient;
		String mainSubject="Onbestelbaar: "+originalSubject;
		String expectedAttachmentName="onbestelbaar met attachments";

		mailListener.setFilter("NDR");
		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertNotNull(rawMessage.getRawMessage().getMessage());
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");

		TestAssertions.assertXpathValueEquals(mainRecipient, 			message, "/email/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(mainFrom,      			message, "/email/from");
		TestAssertions.assertXpathValueEquals(mainSubject,   			message, "/email/subject");

		TestAssertions.assertXpathValueEquals(1,  						message, "count(/email/attachments/attachment)");
		TestAssertions.assertXpathValueEquals(expectedAttachmentName,   message, "/email/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalRecipient, 		message, "/email/attachments/attachment/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(originalFrom,      		message, "/email/attachments/attachment/from");
		TestAssertions.assertXpathValueEquals(originalSubject,  		message, "/email/attachments/attachment/subject");

		TestAssertions.assertXpathValueEquals(originalAttachmentCount,  message, "count(/email/attachments/attachment/attachments/attachment)");

		TestAssertions.assertXpathValueEquals(originalMessageId,      	message, "/email/attachments/attachment/headers/header[@name='Message-ID']");
		TestAssertions.assertXpathValueEquals(originalSubject,      	message, "/email/attachments/attachment/headers/header[@name='Subject']");
	}

	@Test
	@Disabled("skip NDR filter for now")
	public void readFileBounce2() throws Exception {
		String targetFolder="Bounce 2";
		String originalRecipient="";
		String originalFrom="";
		String originalSubject="";
		String originalReturnPath="<>";
		String originalMessageId="<"+ndrMessageId+">";
		String xEnvironment="TST";
		String xCorrelationId="";
		int    originalAttachmentCount=0;

		String mainRecipient=originalRecipient;
		String mainSubject="Onbestelbaar: "+originalSubject;
		String expectedAttachmentName="Undelivered Message";

		mailListener.setFilter("NDR");
		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertNotNull(rawMessage.getRawMessage().getMessage());
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");

		TestAssertions.assertXpathValueEquals(mainRecipient, 			message, "/email/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(mainFrom,      			message, "/email/from");
		TestAssertions.assertXpathValueEquals(mainSubject,   			message, "/email/subject");

		TestAssertions.assertXpathValueEquals(1,  						message, "count(/email/attachments/attachment)");
		TestAssertions.assertXpathValueEquals(expectedAttachmentName,   message, "/email/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalRecipient, 		message, "/email/attachments/attachment/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(originalFrom,      		message, "/email/attachments/attachment/from");
		TestAssertions.assertXpathValueEquals(originalSubject,  		message, "/email/attachments/attachment/subject");

		TestAssertions.assertXpathValueEquals(originalAttachmentCount,  message, "count(/email/attachments/attachment/attachments/attachment)");
		//TestAssertions.assertXpathValueEquals(originalAttachmentName,   message, "/email/attachments/attachment/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalReturnPath,      	message, "/email/attachments/attachment/headers/header[@name='Return-Path']");
		TestAssertions.assertXpathValueEquals(originalMessageId,      	message, "/email/attachments/attachment/headers/header[@name='Message-ID']");
		TestAssertions.assertXpathValueEquals(originalSubject,      	message, "/email/attachments/attachment/headers/header[@name='Subject']");

		TestAssertions.assertXpathValueEquals(xEnvironment,      		message, "/email/attachments/attachment/headers/header[@name='x-Environment']");
		TestAssertions.assertXpathValueEquals(xCorrelationId,      		message, "/email/attachments/attachment/headers/header[@name='x-CorrelationId']");
	}

	@Test
	@Disabled("skip NDR filter for now")
	public void readFileBounce3WithAttachmentInOriginalMail() throws Exception {
		String targetFolder="Bounce 3";
		String originalRecipient="";
		String originalFrom="";
		String originalSubject="Invoice 123";
		String originalReturnPath="<>";
		String originalMessageId="<"+ndrMessageId+">";
		String xEnvironment="TST";
		String xCorrelationId="ID:EMS.C7F75BA93B09872C7C:67234";
		int originalAttachmentCount=1;
		String originalAttachmentName="Invoice_1800000078.pdf";

		String mainRecipient=originalRecipient;
		String mainSubject="Onbestelbaar: "+originalSubject;
		String expectedAttachmentName="Undelivered Message";

		mailListener.setFilter("NDR");
		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertNotNull(rawMessage.getRawMessage().getMessage());
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");

		TestAssertions.assertXpathValueEquals(mainRecipient, 			message, "/email/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(mainFrom,      			message, "/email/from");
		TestAssertions.assertXpathValueEquals(mainSubject,   			message, "/email/subject");

		TestAssertions.assertXpathValueEquals(1,  						message, "count(/email/attachments/attachment)");
		TestAssertions.assertXpathValueEquals(expectedAttachmentName,   message, "/email/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalRecipient, 		message, "/email/attachments/attachment/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(originalFrom,      		message, "/email/attachments/attachment/from");
		TestAssertions.assertXpathValueEquals(originalSubject,  		message, "/email/attachments/attachment/subject");

		TestAssertions.assertXpathValueEquals(originalAttachmentCount,  message, "count(/email/attachments/attachment/attachments/attachment)");
		TestAssertions.assertXpathValueEquals(originalAttachmentName,   message, "/email/attachments/attachment/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalReturnPath,      	message, "/email/attachments/attachment/headers/header[@name='Return-Path']");
		TestAssertions.assertXpathValueEquals(originalMessageId,      	message, "/email/attachments/attachment/headers/header[@name='Message-ID']");
		TestAssertions.assertXpathValueEquals(originalSubject,      	message, "/email/attachments/attachment/headers/header[@name='Subject']");

		TestAssertions.assertXpathValueEquals(xEnvironment,      		message, "/email/attachments/attachment/headers/header[@name='x-Environment']");
		TestAssertions.assertXpathValueEquals(xCorrelationId,      		message, "/email/attachments/attachment/headers/header[@name='x-CorrelationId']");
	}

	@Test
	@Disabled("skip NDR filter for now")
	public void readFileBounce4() throws Exception {
		String targetFolder="Bounce 4";
		String originalRecipient="";
		String originalFrom="";
		String originalSubject="Factuur 23";
		String originalReturnPath="<>";
		String originalMessageId="<"+ndrMessageId+">";
		String xEnvironment="TST";
		String xCorrelationId="ID:EMS.19DA5B995492D6613E2:15033";
		int originalAttachmentCount=0;
		//String originalAttachmentName="Invoice_1800000045.pdf";

		String mainRecipient=originalRecipient;
		String mainSubject="Onbestelbaar: "+originalSubject;
		String expectedAttachmentName="Undelivered Message";

		mailListener.setFilter("NDR");
		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> rawMessage = mailListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertNotNull(rawMessage.getRawMessage().getMessage());
		String message = mailListener.extractMessage(rawMessage, threadContext).asString();

		System.out.println("message ["+message+"]");

		TestAssertions.assertXpathValueEquals(mainRecipient, 			message, "/email/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(mainFrom,      			message, "/email/from");
		TestAssertions.assertXpathValueEquals(mainSubject,   			message, "/email/subject");

		TestAssertions.assertXpathValueEquals(1,  						message, "count(/email/attachments/attachment)");
		TestAssertions.assertXpathValueEquals(expectedAttachmentName,   message, "/email/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalRecipient, 		message, "/email/attachments/attachment/recipients/recipient[@type='to']");
		TestAssertions.assertXpathValueEquals(originalFrom,      		message, "/email/attachments/attachment/from");
		TestAssertions.assertXpathValueEquals(originalSubject,  		message, "/email/attachments/attachment/subject");

		TestAssertions.assertXpathValueEquals(originalAttachmentCount,  message, "count(/email/attachments/attachment/attachments/attachment)");
		//TestAssertions.assertXpathValueEquals(originalAttachmentName,   message, "/email/attachments/attachment/attachments/attachment/@name");

		TestAssertions.assertXpathValueEquals(originalReturnPath,      	message, "/email/attachments/attachment/headers/header[@name='Return-Path']");
		TestAssertions.assertXpathValueEquals(originalMessageId,      	message, "/email/attachments/attachment/headers/header[@name='Message-ID']");
		TestAssertions.assertXpathValueEquals(originalSubject,      	message, "/email/attachments/attachment/headers/header[@name='Subject']");

		TestAssertions.assertXpathValueEquals(xEnvironment,      		message, "/email/attachments/attachment/headers/header[@name='x-Environment']");
		TestAssertions.assertXpathValueEquals(xCorrelationId,      		message, "/email/attachments/attachment/headers/header[@name='x-CorrelationId']");
	}

	@Test
	public void readEmptyFolder() throws Exception {
		String targetFolder="Empty";
		mailListener.setCreateFolders(true);
		configureAndOpen(targetFolder,null);

		Map<String,Object> threadContext=new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> rawMessage = mailListener.getRawMessage(threadContext);
		assertNull(rawMessage);
	}


	//	@Test
//	public void readFileBounceWithAttachments() throws Exception {
//		String targetFolder="Bounce";
//		String targetSubject="Onbestelbaar: onbestelbaar met attachments";
//		configureAndOpen(basefolder2,"NDR");
//
//		Item item = findMessageBySubject(targetFolder,targetSubject);
//		assertNotNull(item);
//		displayItemSummary(item);
//
//		Iterator<Attachment> attachments = fileSystem.listAttachments(item);
//		assertNotNull(attachments);
//		assertTrue("Expected message to contain attachment",attachments.hasNext());
//
//		while(attachments.hasNext()) {
//			Attachment a = attachments.next();
//			displayAttachment(a);
//		}
//	}
//
//	@Test
//	public void readFileWithAttachments() throws Exception {
//		String subfolder="Bounce";
//		String filename = "readFile";
//		String contents = "Tekst om te lezen";
//		configureAndOpen(basefolder2,null);
//
////		if (!folderContainsMessages(subfolder)) {
////			createFile(null, filename, contents);
////			waitForActionToFinish();
////		}
//
//		Iterator<Item> fileIt = fileSystem.listFiles(subfolder);
//		assertNotNull(fileIt);
//		assertTrue(fileIt.hasNext());
//
//		Item file = fileIt.next();
//		displayItem(file);
//
//		Iterator<Attachment> attachments = fileSystem.listAttachments(file);
//		assertNotNull(attachments);
//		assertTrue("Expected message to contain attachment",attachments.hasNext());
//
//		Attachment a = attachments.next();
//
//		assertNotNull(a);
//		//assertEquals("x",fileSystem.getAttachmentName(a));
////		assertEquals("text/plain",fileSystem.getAttachmentContentType(a));
//		//assertEquals("x",fileSystem.getAttachmentFileName(a));
////		assertEquals(320,fileSystem.getAttachmentSize(a));
//
//		displayAttachment(a);
//
//		String attachmentContents=Misc.streamToString(fileSystem.readAttachment(a));
//		System.out.println("attachmentContents ["+attachmentContents+"]");
//
//
//	}


//	@Test
//	public void basicFileSystemTestRead() throws Exception {
//		String filename = "readFile";
//		String contents = "Tekst om te lezen";
//
//		fileSystem.configure();
//		fileSystem.open();
//
//		createFile(null, filename, contents);
//		waitForActionToFinish();
//		// test
//		//existsCheck(filename);
//
//		Item file = fileSystem.toFile(filename);
//		// test
//		testReadFile(file, contents);
//	}




//
//	@Test
//	public void fileSystemTestListFile() throws Exception {
//		fileSystemTestListFile(2);
//	}
//
//	@Test
//	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
//		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
//	}

	@Test
	public void testMessageBrowserOpen() throws Exception {
		mailListener.setProcessedFolder("Done");
		mailListener.configure();
		mailListener.start();

		IMessageBrowser<ExchangeMessageReference> browser = mailListener.getMessageBrowser(ProcessState.DONE);
		assertNotNull(browser);

		int messageCount = browser.getMessageCount();
		assertThat(messageCount, greaterThanOrEqualTo(0));

		List<String> itemIds = new ArrayList<>();
		try (IMessageBrowsingIterator iterator = browser.getIterator()) {

			while (iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {

					String id = item.getId();
					log.debug("ID ["+id+"]");
					itemIds.add(id);
				}
			}
		}
		for (String id:itemIds) {
			RawMessageWrapper<ExchangeMessageReference> email = browser.browseMessage(id);
			Message message = mailListener.getFileSystem().readFile(email.getRawMessage(), null);
			log.debug("Id: "+id+"\nEmail: "+message.asString());
		}
		assertEquals(itemIds.size(), messageCount);
	}

	@Test // this happens when the receiver/listener is closed, but its storages are browsed
	public void testMessageBrowserClosed() throws Exception {
		mailListener.setProcessedFolder("Done");
		mailListener.configure();
		mailListener.start();
		mailListener.stop();

		IMessageBrowser<ExchangeMessageReference> browser = mailListener.getMessageBrowser(ProcessState.DONE);
		assertNotNull(browser);

		assertEquals(-1, browser.getMessageCount());

		List<String> itemIds = new ArrayList<>();
		try (IMessageBrowsingIterator iterator = browser.getIterator()) {

			while (iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {

					String id = item.getId();
					log.debug("ID ["+id+"]");
					itemIds.add(id);
				}
			}
		}
		for (String id:itemIds) {
			RawMessageWrapper<ExchangeMessageReference> email = browser.browseMessage(id);
			Message message = mailListener.getFileSystem().readFile(email.getRawMessage(), null);
			log.debug("Id: "+id+"\nEmail: "+message.asString());
		}
	}

	@Test
	public void testMessageBrowserFilterTest() throws Exception {
		mailListener.setProcessedFolder("Done");
		mailListener.configure();
		mailListener.start();

		IMessageBrowser<ExchangeMessageReference> browser = mailListener.getMessageBrowser(ProcessState.DONE);
		assertNotNull(browser);

		MessageBrowsingFilter filter = new MessageBrowsingFilter();
		filter.setMessageMask("aaa", browser, mailListener);

		try (IMessageBrowsingIterator iterator = browser.getIterator()) {

			while (iterator.hasNext()) {
				try (IMessageBrowsingIteratorItem item = iterator.next()) {

					boolean filterResult = filter.matchAny(item);
					log.debug("ID ["+item.getId()+"] match ["+filterResult+"]");
				}
			}
		}
	}

}
