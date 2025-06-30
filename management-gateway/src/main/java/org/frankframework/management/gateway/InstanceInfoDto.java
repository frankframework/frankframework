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
package org.frankframework.management.gateway;

import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class InstanceInfoDto {
	private final UUID clientId;
	private final String instanceName;
	private final String instanceVersion;
	private final String instanceType = "worker"; // NOSONAR
	private final PublicKey publicKey;

	@JsonProperty("publicKey")
	public String getPublicKeyBase64() {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}
}
