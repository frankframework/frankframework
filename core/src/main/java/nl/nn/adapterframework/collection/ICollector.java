/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.collection;

import java.io.OutputStream;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;

public interface ICollector<E extends ICollectingElement> extends AutoCloseable {

	Message writeItem(Message input, PipeLineSession session, ParameterValueList pvl, E collectingElement) throws CollectionException, TimeoutException;

	/** present to support the deprecated 'STREAM' action, cannot set to Deprecated, because TestAnnotationUtils.findInterfacesWithAnnotations() will complain */
	OutputStream streamItem(Message input, PipeLineSession session, ParameterValueList pvl, E collectingElement) throws CollectionException;

	default MessageOutputStream provideOutputStream(PipeLineSession session, ParameterValueList pvl, E collectingElement) throws CollectionException {
		return null;
	}

}