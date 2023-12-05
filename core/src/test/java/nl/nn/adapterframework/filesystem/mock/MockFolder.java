package nl.nn.adapterframework.filesystem.mock;

import java.util.HashMap;
import java.util.Map;

public class MockFolder extends MockFile {

	private Map<String,MockFile> files = new HashMap<String,MockFile>();        // do not use LinkedHashMap, do not rely on insertion order
	private Map<String,MockFolder> folders = new HashMap<String,MockFolder>();  // do not use LinkedHashMap, do not rely on insertion order

	public MockFolder(String filename, MockFolder parent) {
		super(filename,parent);
	}

	public Map<String, MockFile> getFiles() {
		return files;
	}

	public Map<String, MockFolder> getFolders() {
		return folders;
	}

}
