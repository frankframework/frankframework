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
	JOB_DURATION("frank.job.duration", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),

	PIPE_DURATION("frank.pipe.duration", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),
	PIPE_SIZE_IN("frank.pipe.size.in", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.SIZE_UNIT),
	PIPE_SIZE_OUT("frank.pipe.size.out", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.SIZE_UNIT),
	PIPE_WAIT_TIME("frank.pipe.wait", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),

	PIPELINE_DURATION("frank.pipeline.duration", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),
	PIPELINE_SIZE("frank.pipeline.size", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.SIZE_UNIT), //"- pipeline in"
	PIPELINE_WAIT_TIME("frank.pipeline.wait", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),
	PIPELINE_IN_ERROR("frank.pipeline.messagesInError", Meter.Type.COUNTER),
	PIPELINE_PROCESSED("frank.pipeline.messagesProcessed", Meter.Type.COUNTER),
	PIPELINE_IN_PROCESS("frank.pipeline.messagesInProcess", Meter.Type.GAUGE),

	RECEIVER_DURATION("frank.receiver.duration", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),
	RECEIVER_RECEIVED("frank.receiver.messagesReceived", Meter.Type.COUNTER),
	RECEIVER_REJECTED("frank.receiver.messagesRejected", Meter.Type.COUNTER),
	RECEIVER_RETRIED("frank.receiver.messagesRetried", Meter.Type.COUNTER),

	SENDER_HTTP("frank.http.requests", Meter.Type.TIMER),
	SENDER_HTTP_POOL_MAX("frank.http.pool.max", Meter.Type.GAUGE),
	SENDER_HTTP_POOL_AVAILABLE("frank.http.pool.available", Meter.Type.GAUGE),
	SENDER_HTTP_POOL_LEASED("frank.http.pool.leased", Meter.Type.GAUGE),
	SENDER_HTTP_POOL_PENDING("frank.http.pool.pending", Meter.Type.GAUGE),

	LISTENER_MESSAGE_PEEKING("frank.listener.messagePeeking", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT),
	LISTENER_MESSAGE_RECEIVING("frank.listener.messageReceiving", Meter.Type.DISTRIBUTION_SUMMARY, FrankMeterType.TIME_UNIT);

	public static final String TIME_UNIT = "ms";
	public static final String SIZE_UNIT = "B";

	private final @Getter String meterName;
	private final @Getter Type meterType;
	private final @Getter String baseUnit;

	FrankMeterType(String meterName, Type type) {
		this(meterName, type, null);
	}

	FrankMeterType(String meterName, Type type, String units) {
		this.meterName = meterName;
		this.meterType = type;
		this.baseUnit = units;
	}

	public boolean isOfType(Meter meter) {
		return meterType == meter.getId().getType() && meterName.equals(meter.getId().getName());
	}
}
