package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

public class MockFolder extends MockFile {

	public MockFolder(String filename, MockFolder parent) {
		super(filename,parent);
	}

	private Map<String,MockFile> files = new HashMap<String,MockFile>();
	private Map<String,MockFolder> folders = new HashMap<String,MockFolder>();

	public Map<String, MockFile> getFiles() {
		return files;
	}

	public Map<String, MockFolder> getFolders() {
		return folders;
	}

}
