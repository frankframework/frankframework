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
package nl.nn.adapterframework.aws;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import nl.nn.adapterframework.util.CredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AwsUtil {

	// S3

	public static @Nonnull AWSCredentialsProvider createCredentialProviderChain(@Nullable CredentialFactory cf) {
		List<AWSCredentialsProvider> chain = new ArrayList<>();

		if(cf != null) {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(cf.getUsername(), cf.getPassword());
			chain.add(new AWSStaticCredentialsProvider(awsCreds));
		}

		chain.add(new ProfileCredentialsProvider());
		chain.add(new EC2ContainerCredentialsProviderWrapper());

		return new AWSCredentialsProviderChain(chain);
	}

	// SQS

	public static AwsCredentials getAwsCredentials(String authAlias, String defaultUsername, String defaultPassword) {
		return getAwsCredentials(new CredentialFactory(authAlias, defaultUsername, defaultPassword));
	}

	public static AwsCredentials getAwsCredentials(CredentialFactory cf) {
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

	public static AwsCredentialsProvider getAwsCredentialsProvider(String authAlias, String defaultUsername, String defaultPassword) {
		return StaticCredentialsProvider.create(getAwsCredentials(authAlias, defaultUsername, defaultPassword));
	}

	public static AwsCredentialsProvider getAwsCredentialsProvider(CredentialFactory cf) {
		return StaticCredentialsProvider.create(getAwsCredentials(cf));
	}

}
