/*
   Copyright 2013 Nationale-Nederlanden, 2025 WeAreFrank!

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
package org.frankframework.errormessageformatters;

import org.frankframework.core.HasName;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
/**
 * ErrorMessageFormatter for JUICE, introduced with the Y01-project.
 *
 * @deprecated Please note that the information returned by this ErrorMessageFormatter is not very
 * informative. Consider using one of {@link ErrorMessageFormatter} or {@link XslErrorMessageFormatter}
 *
 * @author Johan Verrips IOS
 */
@Deprecated
public class Y01ErrorMessageFormatter extends ErrorMessageFormatter {
	private final String applicationName = AppConstants.getInstance().getProperty("application.name");
	private final String applicationVersion = AppConstants.getInstance().getProperty("application.version");

	@Override
	public Message format(String message, Throwable t, HasName location, Message originalMessage, String messageId, long receivedTime) {
		String result= "<ServiceResponse>\n" +
				"   <ResponseEnvelope>\n" +
				"       <serviceType>ING_RES1006</serviceType>\n" +
				"       <messageId>" +messageId+   "</messageId>\n" +
				"       <from>"+applicationName+ " "+applicationVersion+ "</from>\n" +
				"       <to>JUICE</to>\n" +
				"       <timeStamp>" + DateFormatUtils.getTimeStamp() + "</timeStamp>\n" +
				"       <ResponseStatus>\n" +
				"           <statusCode>999</statusCode>\n" +
				"           <statusType>SYSTEM</statusType>\n" +
				"           <statusText>" + message + "</statusText>\n" +
				"       </ResponseStatus>\n" +
				"   </ResponseEnvelope>\n" +
				"   <Body>\n" +location.getName()+
				"   </Body>\n" +
				"</ServiceResponse>\n";

		return new Message(result);
	}
}
