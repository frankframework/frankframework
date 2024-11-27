/*
   Copyright 2020, 2023 WeAreFrank!

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
package org.frankframework.extensions.ibm;

import java.nio.charset.StandardCharsets;

import jakarta.jms.BytesMessage;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class TestJMSMessage extends Mockito implements BytesMessage, Answer<BytesMessage> {

	private byte[] messageContent = null;
	private int byteOffset = 0;

	public static TestJMSMessage newInstance() {
		return mock(TestJMSMessage.class, CALLS_REAL_METHODS);
	}

	@Override
	public BytesMessage answer(InvocationOnMock invocation) {
		return this; //created at newInstance() to mock unimplemented methods, initialized once answer is called
	}

	@Override
	public String getJMSMessageID() {
		return "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
	}

	@Override
	public String getJMSCorrelationID() {
		return "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";
	}

	@Override
	public String getStringProperty(String name) {
		return StandardCharsets.ISO_8859_1.name();
	}

	@Override
	public long getBodyLength() {
		return messageContent.length;
	}

	@Override
	public int readBytes(byte[] value) {
		int length = value.length;
		int to = byteOffset + length;
		int from = byteOffset;

		int newLength = to - from;
		if(newLength < 0)
			throw new IllegalArgumentException(from + " > " + to);
		int ceil = messageContent.length - from;
		int len = ceil < newLength ? ceil : newLength;
		System.arraycopy(messageContent, from, value, 0, len);

		byteOffset += length;
		return length;
	}

	@Override
	public int readBytes(byte[] value, int length) {
		return 0;
	}

	@Override
	public void writeBytes(byte[] value) {
		messageContent = value;
	}
}
