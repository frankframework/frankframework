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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

import org.frankframework.util.CredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain.Builder;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AwsUtil {

	private static final boolean REUSE_LAST_PROVIDER = true;

	// S3
	/** com.amazonaws.auth based CredentialProvider */
	public static @Nonnull AWSCredentialsProvider createCredentialProviderChain(@Nullable CredentialFactory cf) {
		List<AWSCredentialsProvider> chain = new ArrayList<>();

		if(cf != null) {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(cf.getUsername(), cf.getPassword());
			chain.add(new AWSStaticCredentialsProvider(awsCreds));
		}

		chain.add(new com.amazonaws.auth.profile.ProfileCredentialsProvider());
		chain.add(new com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper());
		chain.add(new com.amazonaws.auth.InstanceProfileCredentialsProvider(false));

		AWSCredentialsProviderChain cfc = new AWSCredentialsProviderChain(chain);
		cfc.setReuseLastProvider(REUSE_LAST_PROVIDER);
		return cfc;
	}

	// SQS
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

	public static @Nonnull AwsCredentialsProvider getAwsCredentialsProvider(@Nullable CredentialFactory cf) {
		Builder chain = AwsCredentialsProviderChain.builder();

		if(cf != null) {
			chain.addCredentialsProvider(StaticCredentialsProvider.create(getAwsCredentials(cf)));
		}

		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider.create());
		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider.builder().build());
		chain.addCredentialsProvider(software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create());

		chain.reuseLastProviderEnabled(REUSE_LAST_PROVIDER);
		return chain.build();
	}

}
