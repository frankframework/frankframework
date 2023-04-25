/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package nl.nn.adapterframework.pipes;


import java.io.IOException;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

/**
 * Retrieves a message using an {@link IPostboxListener}.
 *
 * Note that most listeners allow you to specify a timeout. The timeout has the following
 * meaning:
 * <ul>
 * <li>&lt;0 = no wait</li>
 * <li>0 = block until message available</li>
 * <li>&gt;= 0 maximum wait in milliseconds<li>
 * </ul>
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link IPostboxListener listener}</td><td>specification of postbox listener to retrieve messages from</td></tr>
 * </table>
 * </p>
  *
 * @author  John Dekker
 */
@Deprecated
public class PostboxRetrieverPipe  extends FixedForwardPipe {

	private IPostboxListener listener = null;
	private String resultOnEmptyPostbox = "empty postbox";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getListener() == null) {
				throw new ConfigurationException("no sender defined ");
		}
	}

	public IPostboxListener getListener() {
		return listener;
	}

	public void setListener(IPostboxListener listener) {
		this.listener = listener;
	}

	@Override
	public void start() throws PipeStartException {
		try {
			getListener().open();
		}
		catch (Exception e) {
			throw new PipeStartException(e);
		}
	}

	@Override
	public void stop() {
		try {
			getListener().close();
		}
		catch (Exception e) {
			log.warn("exception closing sender", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String messageSelector;
		try {
			messageSelector = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		Map threadContext = null;
		try {
			threadContext = getListener().openThread();
			Object rawMessage = getListener().retrieveRawMessage(messageSelector, threadContext);

			if (rawMessage == null) {
				return new PipeRunResult(findForward("emptyPostbox"), getResultOnEmptyPostbox());
			}
			RawMessageWrapper rawMessageWrapper = new RawMessageWrapper(rawMessage, null, null, threadContext);
			Message result = getListener().extractMessage(rawMessageWrapper, threadContext);
			return new PipeRunResult(getSuccessForward(), result);
		} catch (Exception e) {
			throw new PipeRunException( this, "caught exception", e);
		}
		finally {
			try {
				getListener().closeThread(threadContext);
			}
			catch (ListenerException le) {
				log.error("got error closing listener");
			}
		}
	}

	public String getResultOnEmptyPostbox() {
		return resultOnEmptyPostbox;
	}

	/**
	 * result when no object is on postbox
	 * @ff.default empty postbox
	 */
	public void setResultOnEmptyPostbox(String string) {
		resultOnEmptyPostbox = string;
	}

}
