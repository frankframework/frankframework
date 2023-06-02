/*
   Copyright 2023 WeAreFrank!

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

import java.io.IOException;
import java.util.List;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Implementations should convert their input to a 'usable' part.
 * The {@link Collection collection} handles the collection of parts,
 * ensures they are closed if required and 'builds' the collection.
 * 
 * @author Niels Meijer
 *
 * @param <P> Part to be added to the {@link Collection collection}.
 */
public interface ICollector<P> extends AutoCloseable {

	/** Add a single item to the collection */
	P createPart(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException;

	/** 'builds' the collection and returns a persistent Message 
	 * @throws IOException */
	Message build(List<P> parts) throws IOException;
}