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
package nl.nn.adapterframework.filesystem;

import javax.annotation.Nonnull;

import com.amazonaws.services.s3.model.StorageClass;

import lombok.Getter;

/**
 * Define S3 storage class options.
 *
 * @see <a href="https://aws.amazon.com/s3/storage-classes">AWS S3 Storage Classes</a>
 */
public enum S3StorageClass {
	// NB: This is a wrapper around the S3 own enum StorageClass as workaround for a limitation in the Frank!Doc

	STANDARD(StorageClass.Standard),

	REDUCED_REDUNDANCY(StorageClass.ReducedRedundancy),

	STANDARD_IA(StorageClass.StandardInfrequentAccess),

	ONEZONE_IA(StorageClass.OneZoneInfrequentAccess),

	INTELLIGENT_TIERING(StorageClass.IntelligentTiering),

	GLACIER(StorageClass.Glacier),

	DEEP_ARCHIVE(StorageClass.DeepArchive),

	OUTPOSTS(StorageClass.Outposts),

	GLACIER_IR(StorageClass.GlacierInstantRetrieval),

	SNOW(StorageClass.Snow);


	private final @Getter @Nonnull StorageClass storageClass;


	S3StorageClass(StorageClass storageClass) {
		this.storageClass = storageClass;
	}
}
