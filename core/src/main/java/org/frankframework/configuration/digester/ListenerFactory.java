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
package org.frankframework.configuration.digester;

import java.util.Map;

import org.xml.sax.Attributes;

import lombok.extern.log4j.Log4j2;

import org.frankframework.pipes.MessageSendingPipe;

/**
 * Factory for instantiating listeners from the Digester framework.
 * Instantiates correlated listener in the context of a MessageSendingPipe.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
@Log4j2
public class ListenerFactory extends GenericFactory {
	public static final String JMS_LISTENER_CLASSNAME_SUFFIX = ".JmsListener";
	protected static final String CORRELATED_LISTENER_CLASSNAME = "org.frankframework.jms.PullingJmsListener";

	@Override
	protected Map<String, String> copyAttrsToMap(Attributes attrs) {
		Map<String, String> map = super.copyAttrsToMap(attrs);
		String className = attrs.getValue("className");
		if (className != null && getDigester().peek() instanceof MessageSendingPipe && className.endsWith(JMS_LISTENER_CLASSNAME_SUFFIX)) {
			log.debug("JmsListener is created as part of a MessageSendingPipe; replace classname with '{}' to ensure compatibility", CORRELATED_LISTENER_CLASSNAME);
			map.put("className", CORRELATED_LISTENER_CLASSNAME);
		}
		return map;
	}

}
