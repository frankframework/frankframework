/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.monitoring.events;

/**
 * Shared collection of known monitoring events
 * 
 * @author Niels Meijer
 */
public class MonitorEvent {
	// global pipe events
	public static final MonitorEvent LONG_DURATION_MONITORING_EVENT = new MonitorEvent("Pipe Long Processing Duration");
	public static final MonitorEvent PIPE_EXCEPTION_MONITORING_EVENT = new MonitorEvent("Pipe Exception");
	public static final MonitorEvent MESSAGE_SIZE_MONITORING_EVENT = new MonitorEvent("Pipe Message Size Exceeding");

	public static final MonitorEvent PIPE_TIMEOUT_MONITOR_EVENT = new MonitorEvent("Sender Timeout");
	public static final MonitorEvent PIPE_CLEAR_TIMEOUT_MONITOR_EVENT = new MonitorEvent("Sender Received Result on Time");
	public static final MonitorEvent PIPE_EXCEPTION_MONITOR_EVENT = new MonitorEvent("Sender Exception Caught");

	// receiver events
	public static final MonitorEvent RCV_CONFIGURED_MONITOR_EVENT = new MonitorEvent("Receiver Configured");
	public static final MonitorEvent RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT = new MonitorEvent("Exception Configuring Receiver");
	public static final MonitorEvent RCV_STARTED_RUNNING_MONITOR_EVENT = new MonitorEvent("Receiver Started Running");
	public static final MonitorEvent RCV_SHUTDOWN_MONITOR_EVENT = new MonitorEvent("Receiver Shutdown");
	public static final MonitorEvent RCV_SUSPENDED_MONITOR_EVENT = new MonitorEvent("Receiver Operation Suspended");
	public static final MonitorEvent RCV_RESUMED_MONITOR_EVENT = new MonitorEvent("Receiver Operation Resumed");
	public static final MonitorEvent RCV_THREAD_EXIT_MONITOR_EVENT = new MonitorEvent("Receiver Thread Exited");
	public static final MonitorEvent RCV_MESSAGE_TO_ERRORSTORE_EVENT = new MonitorEvent("Receiver Moved Message to ErrorStorage");

	// validator events
	public static final MonitorEvent XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT = new MonitorEvent("Invalid XML: parser error");
	public static final MonitorEvent XML_VALIDATOR_NOT_VALID_MONITOR_EVENT = new MonitorEvent("Invalid XML: does not comply to XSD");
	public static final MonitorEvent XML_VALIDATOR_VALID_MONITOR_EVENT = new MonitorEvent("valid XML");

	public static final MonitorEvent XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT = new MonitorEvent("Switch: Forward Found");
	public static final MonitorEvent XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT = new MonitorEvent("Switch: Forward Not Found");

	private final String event;
	public MonitorEvent(String event) {
		if(event == null) {
			throw new IllegalArgumentException();
		}

		this.event = event;
	}

	public String name() {
		return event;
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MonitorEvent) {
			return obj.hashCode() == hashCode();
		}

		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return name().hashCode();
	}
}
