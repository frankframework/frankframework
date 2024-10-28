package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;

@TestMethodOrder(MethodName.class)
public abstract class FileSystemMessageBrowserTest<F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FileSystemMessageBrowser<F, FS> browser;

	protected FS fileSystem;
	protected String messageIdProperty;

	protected abstract FS createFileSystem();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		fileSystem = createFileSystem();
		autowireByName(fileSystem);
		fileSystem.configure();
		fileSystem.open();
	}

	protected String getMessageId(F file) throws FileSystemException {
		return messageIdProperty!=null ? (String)fileSystem.getAdditionalFileProperties(file).get(messageIdProperty) : fileSystem.getName(file);
	}

	@Test
	public void fileSystemBrowserCount() throws Exception {
		String folder = "browserFolder";

		_createFolder(folder);
		createFile(folder, "file1", "inhoud eerste file");
		createFile(folder, "file2", "inhoud tweede file");
		createFile(folder, "file3", "inhoud derde file");

		browser = new FileSystemMessageBrowser(fileSystem, folder, messageIdProperty);

		assertEquals(3,browser.getMessageCount());
	}

	@Test
	public void fileSystemBrowserContainsMessageId() throws Exception {
		String folder = "browserFolder";

		_createFolder(folder);
		createFile(folder, "file1", "inhoud eerste file");
		createFile(folder, "file2", "inhoud tweede file");
		createFile(folder, "file3", "inhoud derde file");
		createFile(null, "otherFile", "inhoud andere file");


		F file1 = fileSystem.toFile(folder, "file1");
		String mid1 = getMessageId(file1);

		F otherfile = fileSystem.toFile(folder, "otherFile");
		String otherMid = getMessageId(otherfile);

		browser = new FileSystemMessageBrowser(fileSystem, folder, messageIdProperty);

		assertTrue(browser.containsMessageId(mid1));
		assertFalse(browser.containsMessageId(otherMid));
	}

	@Test
	public void fileSystemBrowserIteratorTest() throws Exception {
		String folder = "browserFolder";

		_createFolder(folder);
		createFile(folder, "file1", "inhoud eerste file");
		createFile(folder, "file2", "inhoud tweede file");
		createFile(folder, "file3", "inhoud derde file");
		createFile(null, "otherFile", "inhoud andere file");


		F file1 = fileSystem.toFile(folder, "file1");
		String mid1 = getMessageId(file1);
		String name1 = fileSystem.getName(file1);

		F file2 = fileSystem.toFile(folder, "file2");
		String mid2 = getMessageId(file2);
		String name2 = fileSystem.getName(file2);

		F file3 = fileSystem.toFile(folder, "file3");
		String mid3 = getMessageId(file3);
		String name3 = fileSystem.getName(file3);

		browser = new FileSystemMessageBrowser(fileSystem, folder, messageIdProperty);

		Map<String,String> items = new HashMap<>();

		try (IMessageBrowsingIterator iterator = browser.getIterator()) {
			while(iterator.hasNext()) {
				IMessageBrowsingIteratorItem item = iterator.next();
				String storageKey = item.getId();
				RawMessageWrapper<F> rawMessageWrapper = browser.browseMessage(storageKey);
				assertEquals(item.getId(), rawMessageWrapper.getId());
				assertEquals(item.getId(), rawMessageWrapper.getContext().get(PipeLineSession.STORAGE_ID_KEY));
				F file = rawMessageWrapper.getRawMessage();
				items.put(item.getOriginalId(), fileSystem.getName(file));
			}
		}

		assertEquals(3,items.size());
		assertEquals(name1, items.get(mid1));
		assertEquals(name2, items.get(mid2));
		assertEquals(name3, items.get(mid3));
	}
}
