package org.frankframework.filesystem.mock;

import java.util.HashMap;
import java.util.Map;

public class MockFolder extends MockFile {

	private final Map<String, MockFile> files = new HashMap<>();        // do not use LinkedHashMap, do not rely on insertion order
	private final Map<String, MockFolder> folders = new HashMap<>();  // do not use LinkedHashMap, do not rely on insertion order

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
