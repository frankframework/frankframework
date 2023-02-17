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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;

import nl.nn.adapterframework.util.CredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AwsUtil {

	public static AWSCredentials getAWSCredentials(String authAlias, String defaultUsername, String defaultPassword) {
		return getAWSCredentials(new CredentialFactory(authAlias, defaultUsername, defaultPassword));
	}

	public static AWSCredentials getAWSCredentials(CredentialFactory cf) {
		return new AWSCredentials() {

			@Override
			public String getAWSAccessKeyId() {
				return cf.getUsername();
			}

			@Override
			public String getAWSSecretKey() {
				return cf.getPassword();
			}
		};
	}

	public static AWSCredentialsProvider getAWSCredentialsProvider(String authAlias, String defaultUsername, String defaultPassword) {
		return new AWSStaticCredentialsProvider(getAWSCredentials(authAlias, defaultUsername, defaultPassword));
	}

	public static AWSCredentialsProvider getAWSCredentialsProvider(CredentialFactory cf) {
		return new AWSStaticCredentialsProvider(getAWSCredentials(cf));
	}


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
