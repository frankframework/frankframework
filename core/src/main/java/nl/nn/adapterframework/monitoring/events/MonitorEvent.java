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
public enum MonitorEvent {
	// global pipe events
	LONG_DURATION_MONITORING_EVENT("Pipe Long Processing Duration"),
	PIPE_EXCEPTION_MONITORING_EVENT("Pipe Exception"),
	MESSAGE_SIZE_MONITORING_EVENT("Pipe Message Size Exceeding"),

	PIPE_TIMEOUT_MONITOR_EVENT("Sender Timeout"),
	PIPE_CLEAR_TIMEOUT_MONITOR_EVENT("Sender Received Result on Time"),
	PIPE_EXCEPTION_MONITOR_EVENT("Sender Exception Caught"),

	// receiver events
	RCV_CONFIGURED_MONITOR_EVENT("Receiver Configured"),
	RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT("Exception Configuring Receiver"),
	RCV_STARTED_RUNNING_MONITOR_EVENT("Receiver Started Running"),
	RCV_SHUTDOWN_MONITOR_EVENT("Receiver Shutdown"),
	RCV_SUSPENDED_MONITOR_EVENT("Receiver Operation Suspended"),
	RCV_RESUMED_MONITOR_EVENT("Receiver Operation Resumed"),
	RCV_THREAD_EXIT_MONITOR_EVENT("Receiver Thread Exited"),
	RCV_MESSAGE_TO_ERRORSTORE_EVENT("Receiver Moved Message to ErrorStorage"),

	// validator events
	XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT("Invalid XML: parser error"),
	XML_VALIDATOR_NOT_VALID_MONITOR_EVENT("Invalid XML: does not comply to XSD"),
	XML_VALIDATOR_VALID_MONITOR_EVENT("valid XML"),

	XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT("Switch: Forward Found"),
	XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT("Switch: Forward Not Found");

	MonitorEvent(String string) {
		// TODO Auto-generated constructor stub
	}
}
