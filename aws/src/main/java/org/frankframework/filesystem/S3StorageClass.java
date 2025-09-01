/*
   Copyright 2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import lombok.Getter;
import software.amazon.awssdk.services.s3.model.StorageClass;

/**
 * Define S3 storage class options.
 *
 * @see <a href="https://aws.amazon.com/s3/storage-classes">AWS S3 Storage Classes</a>
 */
public enum S3StorageClass {
	// NB: This is a wrapper around the S3 own enum StorageClass as workaround for a limitation in the Frank!Doc

	STANDARD(StorageClass.STANDARD),

	REDUCED_REDUNDANCY(StorageClass.REDUCED_REDUNDANCY),

	STANDARD_IA(StorageClass.STANDARD_IA),

	ONEZONE_IA(StorageClass.ONEZONE_IA),

	INTELLIGENT_TIERING(StorageClass.INTELLIGENT_TIERING),

	GLACIER(StorageClass.GLACIER),

	DEEP_ARCHIVE(StorageClass.DEEP_ARCHIVE),

	OUTPOSTS(StorageClass.OUTPOSTS),

	GLACIER_IR(StorageClass.GLACIER_IR),

	SNOW(StorageClass.SNOW),

	EXPRESS_ONEZONE(StorageClass.EXPRESS_ONEZONE),

	FSX_OPENZFS(StorageClass.FSX_OPENZFS),

	UNKNOWN_TO_SDK_VERSION(StorageClass.UNKNOWN_TO_SDK_VERSION);


	private final @Getter @Nonnull StorageClass storageClass;


	S3StorageClass(StorageClass storageClass) {
		this.storageClass = storageClass;
	}
}
