/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.extentions.script;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

import lombok.extern.log4j.Log4j2;

/**
 * A MapContext that can operate on streams and has Frank!Framework specific custom functionality.
 * <p>
 * Based on example in https://commons.apache.org/proper/commons-jexl/
 * </p>
 */
@Log4j2
public class FrankScriptContext extends MapContext {

	public FrankScriptContext(Map<String, Object> contextMap) {
		super(contextMap);
	}

	public void warn(String message) {
		log.warn(message);
	}

	/**
	 * This allows using a JEXL lambda as a mapper.
	 *
	 * @param stream the stream
	 * @param mapper the lambda to use as mapper
	 * @return the mapped stream
	 */
	public Stream<?> map(Stream<?> stream, final JexlScript mapper) {
		return stream.map(x -> mapper.execute(this, x));
	}

	/**
	 * This allows using a JEXL lambda as a filter.
	 *
	 * @param stream the stream
	 * @param filter the lambda to use as filter
	 * @return the filtered stream
	 */
	public Stream<?> filter(Stream<?> stream, final JexlScript filter) {
		return stream.filter(x -> Boolean.TRUE.equals(filter.execute(this, x)));
	}
}
