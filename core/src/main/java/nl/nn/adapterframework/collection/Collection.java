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

import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Aggregator which handles the collection of {@link ICollector collector parts}.
 * Ensures they are closed if required and 'builds' the collection.
 * 
 * @author Niels Meijer
 *
 * @param <C> Collector to use, which creates the parts
 * @param <P> Parts to be added to each collection
 */
public class Collection<C extends ICollector<P>, P> implements AutoCloseable {
	private final List<P> parts;
	private final C collector;

	public Collection(C collector) {
		this.collector = collector;
		this.parts = new ArrayList<>();
	}

	public void add(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		P part = collector.createPart(input, session, pvl);
		parts.add(part);
	}

	/** closes the collector */
	public Message build() throws CollectionException {
		try(C closeMe = collector) {
			return collector.build(parts);
		} catch (Exception e) {
			throw new CollectionException("unable to build collection", e);
		}
	}

	@Override
	public void close() throws Exception {
		collector.close();
		for(P part : parts) {
			if(part instanceof AutoCloseable) {
				try {
					((AutoCloseable) part).close();
				} catch (Exception e) {
					LogUtil.getLogger(collector).warn("unable to close collection part", e);
				}
			}
		}
		parts.clear();
	}
}
