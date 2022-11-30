package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.LocalFileSystemSender;
import nl.nn.adapterframework.stream.Message;

public class LocalFileSystemSenderTest extends FileSystemSenderTest<LocalFileSystemSender, Path, LocalFileSystem>{

	public TemporaryFolder folder;

	@Override
	public LocalFileSystemSender createFileSystemSender() {
		LocalFileSystemSender result=new LocalFileSystemSender();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}


	@Test
	public void testRename() throws Exception {
		File folder1 = folder.newFolder("one");
		File src = new File(folder1.getPath()+"/aa.txt");
		File dest = new File(folder1.getPath()+"/bb.txt");
		assertFalse(dest.exists());

		LocalFileSystemSender sender = new LocalFileSystemSender();
		sender.setAction(FileSystemAction.RENAME);
		sender.addParameter(new Parameter("filename", src.getPath()));

		sender.addParameter(new Parameter("destination", dest.getPath()));
		sender.setNumberOfBackups(1);
		autowireByName(sender);
		sender.configure();
		sender.open();

		try (FileOutputStream fout = new FileOutputStream(src)) {
			fout.write("tja".getBytes());
		}

		Message result = sender.sendMessageOrThrow(Message.nullMessage(), null);

		assertEquals("bb.txt", result.asString());
		assertTrue(dest.exists());

	}

	@Test
	public void testRenameToOtherFolder() throws Exception {
		File folder1 = folder.newFolder("one");
		File folder2 = folder.newFolder("two");
		File src = new File(folder1.getPath()+"/aa.txt");
		File dest = new File(folder2.getPath()+"/bb.txt");
		assertFalse(dest.exists());

		LocalFileSystemSender sender = new LocalFileSystemSender();
		autowireByName(sender);
		sender.setAction(FileSystemAction.RENAME);
		sender.addParameter(new Parameter("filename", src.getPath()));

		sender.addParameter(new Parameter("destination", dest.getPath()));
		sender.setNumberOfBackups(1);
		sender.configure();
		sender.open();

		try (FileOutputStream fout = new FileOutputStream(src)) {
			fout.write("tja".getBytes());
		}
		Message result = sender.sendMessageOrThrow(Message.nullMessage(), null);

		assertEquals("bb.txt", result.asString());
		assertTrue(dest.exists());
	}
}
