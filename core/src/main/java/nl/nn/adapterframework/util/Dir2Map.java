package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dir2Map {
	private String directory;
	private String wildcard = "*.*";
	private boolean sizeFormat = true;
	private boolean showDirectories = false;
	private int maxItems = -1;
	private int fileListSize = 0;
	private List<Map<String, Object>> fileInfoList = new ArrayList<Map<String, Object>>();

	public Dir2Map(String directory) {
		this.directory = directory;
		build();
	}

	public Dir2Map(String directory, boolean sizeFormat) {
		this.directory = directory;
		this.sizeFormat = sizeFormat;
		build();
	}

	public Dir2Map(String directory, boolean sizeFormat, String wildcard) {
		this.directory = directory;
		this.sizeFormat = sizeFormat;
		this.wildcard = wildcard;
		build();
	}

	public Dir2Map(String directory, boolean sizeFormat, String wildcard, boolean showDirectories) {
		this.directory = directory;
		this.sizeFormat = sizeFormat;
		this.wildcard = wildcard;
		this.showDirectories = showDirectories;
		build();
	}

	public Dir2Map(String directory, boolean sizeFormat, String wildcard, boolean showDirectories, int maxItems) {
		this.directory = directory;
		this.sizeFormat = sizeFormat;
		this.wildcard = wildcard;
		this.showDirectories = showDirectories;
		this.maxItems = maxItems;
		build();
	}

	public int size() {
		return fileListSize;
	}

	public List<Map<String, Object>> getList() {
		return fileInfoList;
	}

	private void build() {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(directory);
		File files[] = dir.listFiles(filter);
		if (files != null) {
			Arrays.sort(files, new FileNameComparator());
		}

		int count = (files == null ? 0 : files.length);

		if (maxItems >= 0 && count > maxItems) {
			count = maxItems;
		}
		if (showDirectories) {
			File parent = dir.getParentFile();
			if (parent != null) {
				fileInfoList.add(FileInfo(parent, ".."));
			}
		}
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory() && !showDirectories) {
				continue;
			}

			fileInfoList.add(FileInfo(file));
		}
		this.fileListSize = count;
	}

	private Map<String, Object> FileInfo(File file) {
		return FileInfo(file, file.getName());
	}

	private Map<String, Object> FileInfo(File file, String displayName) {
		Map<String, Object> fileInfo = new HashMap<String, Object>(5);

		fileInfo.put("name", displayName);
		try {
			fileInfo.put("path", file.getCanonicalPath());
		}
		catch (IOException e) {
			fileInfo.put("path", file.getPath());
		}
		fileInfo.put("lastModified", file.lastModified());
		fileInfo.put("type", file.isDirectory() ? "directory" : "file");
		fileInfo.put("size", (sizeFormat) ? Misc.toFileSize(file.length(), true) : file.length());

		return fileInfo;
	}
}
