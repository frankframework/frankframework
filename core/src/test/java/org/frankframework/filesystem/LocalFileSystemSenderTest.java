package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.LocalFileSystemSender;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemSenderTest extends FileSystemSenderTest<LocalFileSystemSender, Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	public LocalFileSystemSender createFileSystemSender() {
		LocalFileSystemSender result=new LocalFileSystemSender();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}


	@Test
	public void testRename() throws Exception {
		Path folder1 = Paths.get(folder.toAbsolutePath().toString() + "/one");
		Files.createDirectories(folder1);
		File src = new File(folder1+"/aa.txt");
		File dest = new File(folder1+"/bb.txt");
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
		Path folder1 = Paths.get(folder.toAbsolutePath().toString() + "/one");
		Path folder2 = Paths.get(folder.toAbsolutePath().toString() + "/two");
		Files.createDirectories(folder1);
		Files.createDirectories(folder2);
		File src = new File(folder1+"/aa.txt");
		File dest = new File(folder2+"/bb.txt");
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
