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
package org.frankframework.aws;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import org.frankframework.util.CredentialFactory;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AwsUtil {

	private static final boolean REUSE_LAST_PROVIDER = true;

	/**
	 * Create a chain of credential providers, for AWS SDK v2.
	 * @param cf credential factory
	 * @return chain of credential providers
	 */
	public static @Nonnull AwsCredentialsProvider createCredentialProviderChain(@Nullable CredentialFactory cf) {
		AwsCredentialsProviderChain.Builder chain = AwsCredentialsProviderChain.builder();
		if (cf != null) {
			chain.addCredentialsProvider(StaticCredentialsProvider.create(getAwsCredentials(cf)));
		}

		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider.create());
		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider.builder().build());
		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.builder()
				.asyncCredentialUpdateEnabled(false)
				.build());

		chain.reuseLastProviderEnabled(REUSE_LAST_PROVIDER);
		return chain.build();
	}

	/** software.amazon.awssdk.auth.credentials based CredentialProvider */
	private static AwsCredentials getAwsCredentials(CredentialFactory cf) {
		return new AwsCredentials() {

			@Override
			public String accessKeyId() {
				return cf.getUsername();
			}

			@Override
			public String secretAccessKey() {
				return cf.getPassword();
			}
		};
	}

}
