/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.senders;

import org.frankframework.filesystem.AbstractFileSystemSender;
import org.frankframework.filesystem.AmazonS3FileSystem;
import org.frankframework.filesystem.AmazonS3FileSystemDelegator;
import org.frankframework.filesystem.S3FileRef;
import org.frankframework.filesystem.TypeFilter;

/**
 * Sender to work with the Amazon S3 Filesystem.
 * <p>
 *     In addition to regular parameters for filesystem senders, it is possible
 *     to set custom user-metadata on S3 files by prefixing parameter names with
 *     {@value org.frankframework.filesystem.ISupportsCustomFileAttributes#FILE_ATTRIBUTE_PARAM_PREFIX}.
 *     This prefix will be not be part of the actual metadata property name.
 * </p>
 * <p>
 *     The string value of these parameters will be used as value of the custom metadata attribute.
 * </p>
 */
public class AmazonS3Sender extends AbstractFileSystemSender<S3FileRef, AmazonS3FileSystem> implements AmazonS3FileSystemDelegator {

	public AmazonS3Sender() {
		setFileSystem(new AmazonS3FileSystem());
	}

	public void setTypeFilter(TypeFilter typeFilter) {
		getFileSystem().setTypeFilter(typeFilter);
	}

}
