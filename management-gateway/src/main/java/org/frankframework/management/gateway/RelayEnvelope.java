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

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class RelayEnvelope {
	private UUID messageId;
	private byte[] payload;
	private String authentication;
	private @Setter String type;

	public RelayEnvelope(UUID messageId, byte[] payload, String authentication) {
		this.messageId = messageId;
		this.payload = payload;
		this.authentication = authentication;
	}

	public RelayEnvelope(UUID messageId, byte[] payload) {
		this.messageId = messageId;
		this.payload = payload;
	}
}
