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

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.extensions.ifsa.jms.PushingIfsaProviderListener;

/**
 * Implementation of {@link IPullingListener} that acts as an IFSA-service.
 * 
 * There is no need or possibility to set the ServiceId as the Provider will receive all messages
 * for this Application on the same serviceQueue.
 * 
 * The following session keys are set for each message:
 * <ul>
 *   <li>id (the message id)</li>
 *   <li>cid (the correlation id)</li>
 *   <li>timestamp</li>
 *   <li>replyTo</li>
 *   <li>messageText</li>
 *   <li>fullIfsaServiceName</li>
 *   <li>ifsaServiceName</li>
 *   <li>ifsaGroup</li>
 *   <li>ifsaOccurrence</li>
 *   <li>ifsaVersion</li>
 * </ul>
 * 
 * N.B. 
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on 
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 * 
 * <code>ifsa.provider.useSelectors=false</code>
 * 
 * @author  Gerrit van Brakel
 * @since   4.2, switch class: 4.8
 */
public class IfsaProviderListener extends PushingIfsaProviderListener {
}
