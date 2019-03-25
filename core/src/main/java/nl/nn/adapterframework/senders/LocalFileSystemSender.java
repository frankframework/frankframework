package nl.nn.adapterframework.senders;

import java.io.File;

import nl.nn.adapterframework.filesystem.LocalFileSystem;

public class LocalFileSystemSender extends FileSystemSender<File, LocalFileSystem>{

	public LocalFileSystemSender() {
		setFileSystem(new LocalFileSystem());
	}
	
	public void setDirectory(String directory) {
		getFileSystem().setDirectory(directory);
	}
	
	public void setIsForce(boolean force) {
		getFileSystem().setIsForce(force);
	}
}
