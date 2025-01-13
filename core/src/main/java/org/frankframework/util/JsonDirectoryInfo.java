/*
   Copyright 2020-2024 WeAreFrank!

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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class JsonDirectoryInfo {
	private final File directory;
	private final String wildcard;
	private final boolean showDirectories;
	private final int maxItems;
	private int totalFilesAndFoldersFound;

	public JsonDirectoryInfo(String directory, String wildcard, boolean showDirectories, int maxItems) throws IOException {
		this(new File(directory), wildcard, showDirectories, maxItems);
	}

	public JsonDirectoryInfo(File directory, String wildcard, boolean showDirectories, int maxItems) throws IOException {
		if(!Files.exists(directory.toPath()) || !Files.isDirectory(directory.toPath())) {
			throw new IOException("path ["+directory.getCanonicalPath()+"] does not exist or is not a valid directory");
		}

		this.directory = directory;
		this.wildcard = StringUtils.isEmpty(wildcard) ? "*.*" : wildcard;
		this.showDirectories = showDirectories;
		this.maxItems = Math.max(-1, maxItems);
	}

	private JsonObjectBuilder fileInfo(File file) {
		return fileInfo(file, file.getName());
	}

	private JsonObjectBuilder fileInfo(File file, String displayName) {
		JsonObjectBuilder fileInfo = Json.createObjectBuilder();

		fileInfo.add("name", displayName);
		fileInfo.add("path", normalizePath(file));
		fileInfo.add("lastModified", file.lastModified());

		if(file.isDirectory()) {
			fileInfo.add("type", "directory");
		} else {
			fileInfo.add("type", "file");
			long fileSize = file.length();
			fileInfo.add("size", fileSize);
			fileInfo.add("sizeDisplay", Misc.toFileSize(fileSize, true));
		}

		return fileInfo;
	}

	private String normalizePath(File file) {
		return FilenameUtils.normalize(file.getPath(), true); // Do not use canonical path which causes access permission problem for mounted directories
	}

	private JsonStructure getFileList() {
		JsonArrayBuilder fileInfoList = Json.createArrayBuilder();

		WildCardFilter filter = new WildCardFilter(wildcard);
		File[] files = directory.listFiles(filter);
		if (files != null) {
			Arrays.sort(files, new FileNameComparator());
		}

		this.totalFilesAndFoldersFound = files == null ? 0 : files.length;
		int count = totalFilesAndFoldersFound;

		if (maxItems >= 0 && count > maxItems) {
			count = maxItems;
		}
		if (showDirectories) {
			File parent = directory.getParentFile();
			if (parent != null) {
				fileInfoList.add(fileInfo(parent, ".."));
			}
		}
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory() && !showDirectories) {
				continue;
			}

			fileInfoList.add(fileInfo(file));
		}

		return fileInfoList.build();
	}

	public JsonStructure toJson() {
		JsonObjectBuilder root = Json.createObjectBuilder();
		JsonStructure list = getFileList();
		root.add("count", totalFilesAndFoldersFound);
		root.add("list", list);
		root.add("directory", normalizePath(directory));
		root.add("wildcard", wildcard);

		return root.build();
	}
}
