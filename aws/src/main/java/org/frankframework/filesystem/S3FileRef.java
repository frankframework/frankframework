/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.util.StringUtil;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * Object to hold information about an object stored in Amazon S3.
 * Since the S3 API uses different types of response objects the object here is to:
 * <ul>
 * <li>Allow keys to be prefixed with a bucket-name, separated by {@value #BUCKET_OBJECT_SEPARATOR}.</li>
 * <li>Ensure the correct filename (with extension) is used.</li>
 * <li>Ensures correct folder names, using UNIX path delimiters, ending with a {@value #FILE_DELIMITER}.</li>
 * <li>The content-lenght as well as modification-time are stored.</li>
 * <li>Everything can be updated with a simple method to simplify code in implementations.</li>
 * </ul>
 * 
 * @author Niels Meijer
 */
public class S3FileRef {

	private static final String FILE_DELIMITER = "/";
	public static final String BUCKET_OBJECT_SEPARATOR = "|";

	@Nonnull //may be empty
	private final String name;
	@Nullable
	private final String folder;

	private @Getter @Setter Long contentLength = null;
	private @Getter @Setter Date lastModified = null;
	private @Getter @Setter String bucketName;
	private Map<String, String> userMetadata = new HashMap<>();

	private @Getter InputStream objectContent;

	/** Strip folder prefix of filename if present. May not be changed after creation */
	private S3FileRef(@Nonnull String key) {
		int separatorPos = key.indexOf(BUCKET_OBJECT_SEPARATOR);
		final String rawKey;
		if (separatorPos < 0) {
			rawKey = key;
		} else {
			setBucketName(key.substring(0,separatorPos));
			rawKey = key.substring(separatorPos+1);
		}

		String normalized = FilenameUtils.normalize(rawKey, true);
		this.name = FilenameUtils.getName(normalized); //may be an empty string

		String folder = FilenameUtils.getFullPathNoEndSeparator(normalized);
		//crazy hack to always ensure there is a slash at the end
		this.folder = StringUtils.isNotEmpty(folder) ? folder + FILE_DELIMITER : null;
	}

	public S3FileRef(S3ObjectSummary summary) {
		this(summary.getKey());
		setContentLength(summary.getSize());
		setLastModified(summary.getLastModified());
		setBucketName(summary.getBucketName());
	}

	public S3FileRef(String key, String defaultBucketName) {
		this(key);

		if(StringUtils.isEmpty(bucketName)) {
			setBucketName(defaultBucketName);
		}
	}

	public S3FileRef(String filename, String folderName, String bucketName) {
		this(StringUtil.concatStrings(folderName, FILE_DELIMITER, filename), bucketName);
	}

	/** Returns the canonical name inclusive file path when present */
	public String getKey() {
		String prefix = folder != null ? folder : "";
		return prefix + name;
	}

	/** Returns either the file or foldername (suffixed with a slash) */
	public String getName() {
		if(StringUtils.isNotEmpty(name)) { // File: when not empty, return this immediately
			return name;
		}

		// Folder: only take part before last slash
		String removeEndSlash = StringUtils.chop(folder);
		return StringUtils.substringAfterLast(removeEndSlash, '/');
	}

	public void addUserMetadata(String key, String value) {
		userMetadata.put(key, value);
	}

	@Nonnull
	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}

	public void updateObject(S3Object obj) {
		objectContent = obj.getObjectContent();
		updateObject(obj.getObjectMetadata());
	}

	public void updateObject(ObjectMetadata omd) {
		setContentLength(omd.getContentLength());
		setLastModified(omd.getLastModified());
		userMetadata.putAll(omd.getUserMetadata());
	}
}
