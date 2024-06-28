package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class FileSystemWithAttachmentsTest<F, A, FS extends IMailFileSystem<F,A>> extends HelperedBasicFileSystemTest<F,FS> {

	protected IFileSystemWithAttachmentsTestHelper<A> getHelper() {
		return (IFileSystemWithAttachmentsTestHelper<A>)helper;
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		autowireByName(fileSystem);
	}

	@Test
	public void fileSystemWithAttachmentsTestList() throws Exception {
		String filename = "testAttachmentBasics" + FILE1;
		String attachmentName="testAttachmentName";
		String attachmentFileName="testAttachmentFileName";
		String attachmentContentType="testAttachmentContentType";
		String attachmentContents="attachmentContents";
		byte[] attachmentContentsBytes=attachmentContents.getBytes(StandardCharsets.UTF_8);

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "tja");
		A attachment = getHelper().createAttachment(attachmentName, attachmentFileName, attachmentContentType, attachmentContentsBytes);
		getHelper().addAttachment(null, filename, attachment);
		waitForActionToFinish();

		// test
		F f = fileSystem.toFile(filename);
		assertTrue(fileSystem.exists(f), "Expected file[" + filename + "] to be present");

		Iterator<A> attachmentIterator = fileSystem.listAttachments(f);
		assertNotNull(attachmentIterator);
		assertTrue(attachmentIterator.hasNext());
		A attachmentRetrieved = attachmentIterator.next();
		assertFalse(attachmentIterator.hasNext());
		assertNotNull(attachmentRetrieved);

		assertEquals(attachmentName,fileSystem.getAttachmentName(attachmentRetrieved));
		assertEquals(attachmentFileName,fileSystem.getAttachmentFileName(attachmentRetrieved));
		assertEquals(attachmentContentType,fileSystem.getAttachmentContentType(attachmentRetrieved));

		assertEquals(attachmentContents, fileSystem.readAttachment(attachmentRetrieved).asString());
	}

	@Test
	public void fileSystemWithAttachmentsTestListProperties() throws Exception {
		String filename = "testAttachmentBasics" + FILE1;
		String attachmentName="testAttachmentName";
		String attachmentFileName="testAttachmentFileName";
		String attachmentContentType="testAttachmentContentType";
		String attachmentContents="attachmentContents";
		byte[] attachmentContentsBytes=attachmentContents.getBytes(StandardCharsets.UTF_8);
		String propname1="propname1";
		String propname2="propname2";
		String propvalue1="propvalue1";
		String propvalue2="propvalue2";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "tja");
		A attachment = getHelper().createAttachment(attachmentName, attachmentFileName, attachmentContentType, attachmentContentsBytes);
		getHelper().setProperty(attachment, propname1, propvalue1);
		getHelper().setProperty(attachment, propname2, propvalue2);
		getHelper().addAttachment(null, filename, attachment);
		waitForActionToFinish();

		// test
		F f = fileSystem.toFile(filename);
		assertTrue(fileSystem.exists(f), "Expected file[" + filename + "] to be present");

		Iterator<A> attachmentIterator = fileSystem.listAttachments(f);
		assertNotNull(attachmentIterator);
		assertTrue(attachmentIterator.hasNext());
		A attachmentRetrieved = attachmentIterator.next();
		assertFalse(attachmentIterator.hasNext());
		assertNotNull(attachmentRetrieved);

		assertEquals(attachmentName,fileSystem.getAttachmentName(attachmentRetrieved));
		assertEquals(attachmentFileName,fileSystem.getAttachmentFileName(attachmentRetrieved));
		assertEquals(attachmentContentType,fileSystem.getAttachmentContentType(attachmentRetrieved));

		assertEquals(attachmentContents, fileSystem.readAttachment(attachmentRetrieved).asString());

		Map<String,Object> retrievedProperties = fileSystem.getAdditionalAttachmentProperties(attachmentRetrieved);
		assertNotNull(retrievedProperties);
		assertEquals(propvalue1,retrievedProperties.get(propname1));
		assertEquals(propvalue2,retrievedProperties.get(propname2));
	}


}
