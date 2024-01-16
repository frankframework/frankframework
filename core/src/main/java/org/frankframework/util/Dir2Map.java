/*
Copyright 2020 WeAreFrank!

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.frankframework.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class Dir2Map {
	private File directory;
	private String wildcard = "*.*";
	private boolean showDirectories = false;
	private int maxItems = -1;
	private int fileListSize = 0;
	private List<Map<String, Object>> fileInfoList = new ArrayList<>();

	public Dir2Map(String directory, String wildcard, boolean showDirectories, int maxItems) {
		this(new File(directory), wildcard, showDirectories, maxItems);
	}

	public Dir2Map(File directory, String wildcard, boolean showDirectories, int maxItems) {
		this.directory = directory;
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

	public String getDirectory() {
		return normalizePath(directory);
	}

	private void build() {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File files[] = directory.listFiles(filter);
		if (files != null) {
			Arrays.sort(files, new FileNameComparator());
		}

		this.fileListSize = (files == null ? 0 : files.length);
		int count = fileListSize;

		if (maxItems >= 0 && count > maxItems) {
			count = maxItems;
		}
		if (showDirectories) {
			File parent = directory.getParentFile();
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
	}

	private Map<String, Object> FileInfo(File file) {
		return FileInfo(file, file.getName());
	}

	private Map<String, Object> FileInfo(File file, String displayName) {
		Map<String, Object> fileInfo = new HashMap<>(6);

		fileInfo.put("name", displayName);
		fileInfo.put("path", normalizePath(file));
		fileInfo.put("lastModified", file.lastModified());
		fileInfo.put("type", file.isDirectory() ? "directory" : "file");
		fileInfo.put("size", file.length());
		fileInfo.put("sizeDisplay", Misc.toFileSize(file.length(), true));

		return fileInfo;
	}

	private String normalizePath(File file) {
		return FilenameUtils.normalize(file.getPath(), true); // Do not use canonical path which causes access permission problem for mounted directories
	}
}
