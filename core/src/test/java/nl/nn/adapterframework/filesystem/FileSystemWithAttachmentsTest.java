package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.util.Misc;

public abstract class FileSystemWithAttachmentsTest<F, A, FS extends IWithAttachments<F,A>> extends HelperedBasicFileSystemTest<F,FS> {

	protected IFileSystemWithAttachmentsTestHelper<A> getHelper() {
		return (IFileSystemWithAttachmentsTestHelper<A>)helper;
	}
	
	@Test
	public void fileSystemWithAttachmentsTestList() throws Exception {
		String filename = "testAttachmentBasics" + FILE1;
		String attachmentName="testAttachmentName";
		String attachmentFileName="testAttachmentFileName";
		String attachmentContentType="testAttachmentContentType";
		String attachmentContents="attachmentContents";
		byte[] attachmentContentsBytes=attachmentContents.getBytes("UTF-8");
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "tja");
		A attachment = getHelper().createAttachment(attachmentName, attachmentFileName, attachmentContentType, attachmentContentsBytes);
		getHelper().addAttachment(null, filename, attachment);
		waitForActionToFinish();
		
		// test
		F f = fileSystem.toFile(filename);
		assertTrue("Expected file[" + filename + "] to be present", fileSystem.exists(f));
		
		Iterator<A> attachmentIterator = fileSystem.listAttachments(f);
		assertNotNull(attachmentIterator);
		assertTrue(attachmentIterator.hasNext());
		A attachmentRetrieved = attachmentIterator.next();
		assertFalse(attachmentIterator.hasNext());
		assertNotNull(attachmentRetrieved);
		
		assertEquals(attachmentName,fileSystem.getAttachmentName(attachmentRetrieved));
		assertEquals(attachmentFileName,fileSystem.getAttachmentFileName(attachmentRetrieved));
		assertEquals(attachmentContentType,fileSystem.getAttachmentContentType(attachmentRetrieved));
		
		assertEquals(attachmentContents,Misc.streamToString(fileSystem.readAttachment(attachmentRetrieved)));
	}

	@Test
	public void fileSystemWithAttachmentsTestListProperties() throws Exception {
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
		assertTrue("Expected file[" + filename + "] to be present", fileSystem.exists(f));
		
		Iterator<A> attachmentIterator = fileSystem.listAttachments(f);
		assertNotNull(attachmentIterator);
		assertTrue(attachmentIterator.hasNext());
		A attachmentRetrieved = attachmentIterator.next();
		assertFalse(attachmentIterator.hasNext());
		assertNotNull(attachmentRetrieved);
		
		assertEquals(attachmentName,fileSystem.getAttachmentName(attachmentRetrieved));
		assertEquals(attachmentFileName,fileSystem.getAttachmentFileName(attachmentRetrieved));
		assertEquals(attachmentContentType,fileSystem.getAttachmentContentType(attachmentRetrieved));
		
		assertEquals(attachmentContents,Misc.streamToString(fileSystem.readAttachment(attachmentRetrieved)));
		
		Map<String,Object> retrievedProperties = fileSystem.getAdditionalAttachmentProperties(attachmentRetrieved);
		assertNotNull(retrievedProperties);
		assertEquals(propvalue1,retrievedProperties.get(propname1));
		assertEquals(propvalue2,retrievedProperties.get(propname2));
	}


}
