/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.statistics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import lombok.Getter;

public enum FrankMeterType {
	TOTAL_MESSAGES_IN_ERROR("frank.messagesInError", Meter.Type.COUNTER),
	TOTAL_MESSAGES_PROCESSED("frank.messagesProcessed", Meter.Type.COUNTER),
	TOTAL_MESSAGES_REJECTED("frank.messagesRejected", Meter.Type.COUNTER),

	PIPE_DURATION("frank.pipe.duration", Meter.Type.DISTRIBUTION_SUMMARY),
	PIPE_SIZE("frank.pipe.msgsize", Meter.Type.DISTRIBUTION_SUMMARY),

	PIPELINE_DURATION("frank.pipeline.duration", Meter.Type.DISTRIBUTION_SUMMARY),
	PIPELINE_IN_ERROR("frank.pipeline.messagesInError", Meter.Type.COUNTER),
	PIPELINE_PROCESSED("frank.pipeline.messagesProcessed", Meter.Type.COUNTER),

	RECEIVER_RECEIVED("frank.receiver.messagesReceived", Meter.Type.COUNTER),
	RECEIVER_REJECTED("frank.receiver.messagesRejected", Meter.Type.COUNTER),
	RECEIVER_RETRIED("frank.receiver.messagesRetried", Meter.Type.COUNTER);

	private final @Getter String meterName;
	private final @Getter Type meterType;

	private FrankMeterType(String meterName, Type type) {
		this.meterName = meterName;
		this.meterType = type;
	}

	public boolean isOfType(Meter meter) {
		return meterType == meter.getId().getType() && meterName.equals(meter.getId().getName());
	}
}
