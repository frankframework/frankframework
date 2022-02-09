package nl.nn.adapterframework.filesystem;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.testutil.PropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.nio.file.DirectoryStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class ExchangeFileSystemRuntimeParametersTest  extends MailFileSystemTestBase<EmailMessage, Attachment, ExchangeFileSystem>{

	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";

	private String url         = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
	private String mailaddress2 = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress2");
	private String accessToken = PropertyUtil.getProperty(PROPERTY_FILE, "accessToken");
//	private String basefolder2 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder2");
//	private String basefolder3 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder3");

	private final String SEPARATOR = "|";


	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";

	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		if (StringUtils.isNotEmpty(url)) fileSystem.setUrl(url);
		fileSystem.setAccessToken(accessToken);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		return fileSystem;
	}

	@Test
	public void testFolders() throws Exception {
		testFolders(constructFolderName(mailaddress, "Test folder A"));
	}

	@Test
	public void testFiles() throws Exception {
		testFiles(constructFolderName(mailaddress, "Test folder A"),
			constructFolderName(mailaddress, "Test folder B"), constructFolderName(mailaddress,"messageFolder"));
	}

	@Test
	public void testCopyFileAcrossMailboxes() throws Exception {
		String folderNameA = constructFolderName(mailaddress, "messageFolder");
		String folderNameB = constructFolderName(mailaddress2, "Infected Items");

		try(DirectoryStream<EmailMessage> ds = fileSystem.listFiles(folderNameA)) {
			Iterator<EmailMessage> it = ds.iterator();
			assertTrue("there must be at least one messsage in the sourceOfMessages_folder ["+folderNameA+"]", it!=null && it.hasNext());

			EmailMessage sourceFile = it.next();
			assertTrue("file retrieved from folder should exist", fileSystem.exists(sourceFile));

			EmailMessage destFile1 = fileSystem.copyFile(sourceFile, folderNameB, true);
			assertTrue("source file should still exist after copy", fileSystem.exists(sourceFile));

			assertFalse("Destination file cannot have the same id as original file", sourceFile.getId().getUniqueId().equalsIgnoreCase(destFile1.getId().getUniqueId()));
			//displayFile(destFile1);
			assertNotNull("destination file should be not null after copy", destFile1);
			assertTrue("destination file should exist after copy", fileSystem.exists(destFile1));
			//assertTrue("name of destination file should exist in folder after copy", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		}
	}

	private String constructFolderName(String mailbox, String folderName){
		return mailbox + SEPARATOR + folderName;
	}
}
