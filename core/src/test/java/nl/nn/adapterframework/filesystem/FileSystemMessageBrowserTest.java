package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class FileSystemMessageBrowserTest<F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FileSystemMessageBrowser<F, FS> browser;

	protected FS fileSystem;
	protected String messageIdProperty;

	protected abstract FS createFileSystem();

	@Override
	@Before
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
				F file = browser.browseMessage(storageKey);
				items.put(item.getOriginalId(), fileSystem.getName(file));
			}
		}

		assertEquals(3,items.size());
		assertEquals(name1, items.get(mid1));
		assertEquals(name2, items.get(mid2));
		assertEquals(name3, items.get(mid3));
	}
}