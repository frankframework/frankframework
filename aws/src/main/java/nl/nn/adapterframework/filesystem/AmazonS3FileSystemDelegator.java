/*
   Copyright 2023 WeAreFrank!

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

import nl.nn.adapterframework.doc.ReferTo;

public interface AmazonS3FileSystemDelegator {

	AmazonS3FileSystem getFileSystem();

	@ReferTo(AmazonS3FileSystem.class)
	default void setAccessKey(String accessKey) {
		getFileSystem().setAccessKey(accessKey);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setSecretKey(String secretKey) {
		getFileSystem().setSecretKey(secretKey);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled) {
		getFileSystem().setChunkedEncodingDisabled(chunkedEncodingDisabled);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		getFileSystem().setForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setClientRegion(String clientRegion) {
		getFileSystem().setClientRegion(clientRegion);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setBucketName(String bucketName) {
		getFileSystem().setBucketName(bucketName);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setProxyPort(Integer proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setServiceEndpoint(String serviceEndpoint) {
		getFileSystem().setServiceEndpoint(serviceEndpoint);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setStorageClass(S3StorageClass s3StorageClass) {
		getFileSystem().setStorageClass(s3StorageClass);
	}

	@ReferTo(AmazonS3FileSystem.class)
	default void setMaxConnections(int maxConnections) {
		getFileSystem().setMaxConnections(maxConnections);
	}
}
