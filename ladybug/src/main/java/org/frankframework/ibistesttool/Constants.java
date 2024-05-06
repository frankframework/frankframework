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
package org.frankframework.ibistesttool;

public class Constants {
	private Constants() {}

	// Both Ladybug and the FF! truncate messages as configured through
	// maxMessageLength. In this class we keep one character more such that
	// Ladybug can detect when a message is being truncated. This way,
	// Ladybug can show a message in its GUI that a report message has been
	// truncated.
	static final int MAX_MESSAGE_LENGTH_ADJUSTMENT = 1;
}
