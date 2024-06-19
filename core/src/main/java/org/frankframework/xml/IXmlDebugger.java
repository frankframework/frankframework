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
package org.frankframework.xml;

import org.frankframework.core.PipeLineSession;
import org.xml.sax.ContentHandler;

/**
 * Utility interface to allow the debugger to inspect XML in the middle of a streaming execution.
 *
 * @author Gerrit van Brakel
 *
 */
public interface IXmlDebugger {

	/**
	 * Allow the debugger to see the XML stream.
	 */
	ContentHandler inspectXml(PipeLineSession session, String label, ContentHandler contentHandler);
}
