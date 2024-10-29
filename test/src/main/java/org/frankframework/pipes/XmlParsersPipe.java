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
package org.frankframework.pipes;

import java.util.Map;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Pipe that returns information about all the sax/xml/soapMessage parsers
 */
public class XmlParsersPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Map<String, String> xmlInfo = XmlUtils.getVersionInfo();
		XmlBuilder builder = new XmlBuilder("parsers");
		for (Map.Entry<String, String> entry : xmlInfo.entrySet()) {
			XmlBuilder el = new XmlBuilder(entry.getKey());
			el.setValue(entry.getValue());
			builder.addSubElement(el);
		}

		return new PipeRunResult(getSuccessForward(), builder.asMessage());
	}
}
