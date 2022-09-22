package nl.nn.adapterframework.filesystem;

import java.util.LinkedHashMap;
import java.util.Map;

public class MockFolder extends MockFile {

	private Map<String,MockFile> files = new LinkedHashMap<String,MockFile>();
	private Map<String,MockFolder> folders = new LinkedHashMap<String,MockFolder>();

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
