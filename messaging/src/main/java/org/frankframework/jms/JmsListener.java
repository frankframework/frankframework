/*
   Copyright 2013 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.jms;

import org.frankframework.core.IJmsListener;
import org.frankframework.core.IPullingListener;

/**
 * A true multi-threaded {@link IPullingListener Listener}-class.
 * <br/>
 *
 * Since version 4.1, Ibis supports distributed transactions using the XA-protocol. This feature is controlled by the
 * {@link #setTransacted(boolean) transacted} attribute. If this is set to <code>true</code>, received messages are
 * committed or rolled back, possibly together with other actions, by the receiver or the pipeline.
 * In case of a failure, all actions within the transaction are rolled back.
 * </p><p>
 * Setting {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the success state for committing.
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages.
 * In cases where the client is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode,
 * since a session has lower overhead in trying to prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} will only be processed if
 * the setting for {@link #setTransacted(boolean) listener.transacted}.</p>
 *
 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified, that in turn sends the message to
 * whatever it is configured to.</p>
 *
 * <p>You can add parameters to the JmsListener, the values will be added as Headers to the JMS response message.</p>
 *
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * {@link jakarta.jms.TextMessage}s and {@link jakarta.jms.BytesMessage}<br/><br/>
 * </p>
 *
 * {@inheritClassDoc}
 *
 * @author Gerrit van Brakel
 * @since 4.0.1, since 4.8 as 'switch'-class
 */
public class JmsListener extends PushingJmsListener implements IJmsListener<jakarta.jms.Message> {

}
