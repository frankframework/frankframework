/*
 * Copyright 2020 WeAreFrank!
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frankframework.extensions.akamai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import org.frankframework.extensions.akamai.NetStorageSender.Action;
import org.frankframework.extensions.akamai.NetStorageUtils.KeyedHashAlgorithm;

public class NetStorageCmsSignerTest {

	@Test
	public void vaidateDataAndSignHeader() throws URISyntaxException {
		URI uri = new URI("http://127.0.0.1/cpCode123/path");
		NetStorageRequest action = new NetStorageRequest(Action.DU);
		NetStorageCmsSigner signer = new NetStorageCmsSigner(uri, "myNonce", "accessToken") {
			@Override
			protected String getAuthDataHeaderValue() {
				return "much auth value";
			}
		};
		Map<String, String> headers = signer.computeHeaders(action);
		String sign = headers.get(NetStorageCmsSigner.AUTH_SIGN_HEADER);

		assertEquals("w8k5LZTRNKubFvWIqUEgcTrDZhPCo48PeHX45zxSlBU=", sign);
	}

	// Validate hash compute method
	@Test
	public void netStorageUtilsComputeSignHeader() {
		byte[] data = "myDummyString".getBytes();
		byte[] binaryData = NetStorageUtils.computeKeyedHash(data, "accessToken", KeyedHashAlgorithm.HMACSHA256);
		byte[] base64Result = Base64.encodeBase64Chunked(binaryData);
		String result = new String(base64Result);

		assertEquals("9zw0n7D/Rw2X9bVTtlJiWzeFKhAZAge0l3DY8d3lQxk=", result.trim());
	}
}
