/*
   Copyright 2024-2026 WeAreFrank!

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
package org.frankframework.core;

/**
 * Marker interface for correlated sender, used in {@link org.frankframework.receivers.Receiver}.
 */
public interface ICorrelatedSender extends ISender {

	LinkMethod getLinkMethod();

	public enum LinkMethod {
		/** use the generated messageId as the correlationId in the selector for response messages */
		MESSAGEID,
		/** set the correlationId of the pipeline as the correlationId of the message sent, and use that as the correlationId in the selector for response messages */
		CORRELATIONID,
		/** do not automatically set the correlationId of the message sent, but use use the value found in that header after sending the message as the selector for response messages */
		CORRELATIONID_FROM_MESSAGE
	}
}
