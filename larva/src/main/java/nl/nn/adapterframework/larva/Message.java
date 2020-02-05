/*
   Copyright 2020 Integration Partners
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
package nl.nn.adapterframework.larva;

import java.util.Map;

/**
 * Simple Message structure for message listener.
 *
 * @author Murat Kaan Meral
 */
class Message{
	Map<String, String> message;
	String testName;
	int logLevel;
	long timestamp;
	
	Message(String testName, Map<String, String> message, int logLevel, long timestamp) {
		super();
		this.testName = testName;
		this.message = message;
		this.logLevel = logLevel;
		this.timestamp = timestamp;
	}
}
