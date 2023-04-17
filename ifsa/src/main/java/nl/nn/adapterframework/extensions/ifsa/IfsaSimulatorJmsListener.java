/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.ifsa;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * Extension of JmsListener which only stores IFSA variables with their value to simulate IFSA.
 *
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */
public class IfsaSimulatorJmsListener extends JmsListener {

	@Override
    public String getIdFromRawMessage(Message rawMessage, Map threadContext) throws ListenerException {
		String cid = super.getIdFromRawMessage(rawMessage, threadContext);

		String ifsa_bif_id = null;
		String ifsa_source = null;
		String ifsa_node_id = null;
		String ifsa_destination = null;
		try{
			ifsa_bif_id = rawMessage.getStringProperty("ifsa_bif_id");
			ifsa_source = rawMessage.getStringProperty("ifsa_source");
			ifsa_node_id = rawMessage.getStringProperty("ifsa_node_id");
			ifsa_destination = rawMessage.getStringProperty("ifsa_destination");
		} catch (JMSException ignore) {
			log.debug("error getting IFSA jms properties", ignore);
		}

		threadContext.put("ifsa_bif_id",ifsa_bif_id);
		threadContext.put("ifsa_source",ifsa_source);
		threadContext.put("ifsa_node_id",ifsa_node_id);
		threadContext.put("ifsa_destination",ifsa_destination);

		return cid;
	}

}
