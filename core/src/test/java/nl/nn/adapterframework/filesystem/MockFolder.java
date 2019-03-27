package nl.nn.adapterframework.filesystem;

import java.util.HashMap;
import java.util.Map;

public class MockFolder extends MockFile {

	public MockFolder(String filename, MockFolder parent) {
		super(filename,parent);
	}

	private Map<String,MockFile> files = new HashMap<String,MockFile>();

	public Map<String, MockFile> getFiles() {
		return files;
	}

}
