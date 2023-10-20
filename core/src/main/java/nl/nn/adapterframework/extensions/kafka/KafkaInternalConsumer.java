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

import java.time.Duration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;

public class KafkaInternalConsumer implements Runnable {
	private final Consumer<String, byte[]> consumer;
	private final long pollTimeout;
	private final java.util.function.Consumer<ConsumerRecord<String, byte[]>> onMessage;
	public KafkaInternalConsumer(Consumer<String, byte[]> consumer, long pollTimeout, java.util.function.Consumer<ConsumerRecord<String, byte[]>> onMessage) {
		this.consumer = consumer;
		this.onMessage = onMessage;
		this.pollTimeout = pollTimeout;
		new Thread(this).start();
	}
	@Override
	public void run() {
		Duration duration = Duration.ofMillis(pollTimeout);
		try {
			while (true) {
				consumer.poll(duration).forEach(onMessage);
			}
		} catch(WakeupException e) {
			consumer.close();
		}
	}
	public void kill() {
		consumer.wakeup();
		try {
			Thread.sleep(10); //Wait for the thread to stop. The actual delay varies a bit, but this should catch most cases.
		} catch(Exception ignored) {}
	}
}
