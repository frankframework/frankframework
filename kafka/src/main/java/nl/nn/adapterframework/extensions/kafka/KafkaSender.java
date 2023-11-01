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
package nl.nn.adapterframework.extensions.kafka;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;

public class KafkaSender extends KafkaFacade implements ISender {
	/** The topic to send messages to. Only one topic per sender. Wildcards are not supported. */
	private @Setter String topic;
	/** The type of the key. Used for serializing.
	 * @ff.default STRING **/
	private @Setter KafkaType keyType = KafkaType.STRING;
	/** The type of the message. Used for serializing.
	 * @ff.default BYTEARRAY **/
	private @Setter KafkaType messageType = KafkaType.BYTEARRAY;
	//getter is for testing purposes only.
	private @Getter(AccessLevel.PACKAGE) KafkaInternalSender internalSender;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		internalSender = generateInternalSender(keyType, messageType);
		internalSender.setTopic(topic);
		internalSender.setBootstrapServers(getBootstrapServers());
		internalSender.setClientId(getClientId());
		internalSender.configure();
	}

	@Override
	public void open() throws SenderException {
		internalSender.open();
	}

	@Override
	public void close() throws SenderException {
		internalSender.close();
	}

	@Override
	public SenderResult sendMessage(nl.nn.adapterframework.stream.Message message, PipeLineSession session) throws SenderException {
		return internalSender.sendMessage(message, session);
	}

	@Override
	public String getPhysicalDestinationName() {
		return internalSender.getPhysicalDestinationName();
	}

	public static KafkaInternalSender generateInternalSender(KafkaType keyType, KafkaType messageType) {
		if(keyType == KafkaType.STRING && messageType == KafkaType.STRING) return new KafkaInternalSender<String, String>(keyType, messageType);
		if(keyType == KafkaType.STRING && messageType == KafkaType.BYTEARRAY) return new KafkaInternalSender<String, byte[]>(keyType, messageType);
		if(keyType == KafkaType.BYTEARRAY && messageType == KafkaType.STRING) return new KafkaInternalSender<byte[], String>(keyType, messageType);
		if(keyType == KafkaType.BYTEARRAY && messageType == KafkaType.BYTEARRAY) return new KafkaInternalSender<byte[], byte[]>(keyType, messageType);
		throw new IllegalArgumentException("Unknown KafkaType combination ["+keyType+"-"+messageType+"]");
	}
}
