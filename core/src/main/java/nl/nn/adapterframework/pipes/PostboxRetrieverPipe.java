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
package nl.nn.adapterframework.pipes;


import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 


/** 
 * @author  John Dekker
 */
@IbisDescription(
	"Retrieves a message using an {@link IPostboxListener}. " + 
	"Note that most listeners allow you to specify a timeout. The timeout has the following" + 
	"meaning:" + 
	"<ul> " + 
	"<li>&lt;0 = no wait</li>" + 
	"<li>0 = block until message available</li>" + 
	"<li>&gt;= 0 maximum wait in milliseconds<li>" + 
	"</ul> " + 
	"<tr><th>nested elements</th><th>description</th></tr>" + 
	"<tr><td>{@link IPostboxListener listener}</td><td>specification of postbox listener to retrieve messages from</td></tr>" + 
	"</table>" + 
	"</p>" + 
	"<p><b>Exits:</b>" + 
	"<table border=\"1\">" + 
	"<tr><th>state</th><th>condition</th></tr>" + 
	"<tr><td>\"success\"</td><td>default when the message was successfully sent</td></tr>" + 
	"<tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as \"success\"</td></tr>" + 
	"</table>" + 
	"</p>" + 
	" " 
)
public class PostboxRetrieverPipe  extends FixedForwardPipe {

	private IPostboxListener listener = null;
	private String resultOnEmptyPostbox = "empty postbox";
		
	public void configure() throws ConfigurationException {
		super.configure();

		if (getListener() == null) {
				throw new ConfigurationException(getLogPrefix(null) + "no sender defined ");
		}
	}

	public IPostboxListener getListener() {
		return listener;
	}

	public void setListener(IPostboxListener listener) {
		this.listener = listener;
	}

	public void start() throws PipeStartException {
		try {
			getListener().open();
		} 
		catch (Exception e) {
			throw new PipeStartException(e);
		}
	}

	public void stop() {
		try {
			getListener().close();
		} 
		catch (Exception e) {
			log.warn(getLogPrefix(null) + " exception closing sender", e);
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (! (input instanceof String)) {
			throw new PipeRunException(this, "String expected, got a [" + input.getClass().getName() + "]");
		}

		Map threadContext = null;
		try {
			threadContext = getListener().openThread();
			Object rawMessage = getListener().retrieveRawMessage((String)input, threadContext);
			
			if (rawMessage == null)
				return new PipeRunResult(findForward("emptyPostbox"), getResultOnEmptyPostbox());
				
			String result = getListener().getStringFromRawMessage(rawMessage, threadContext);
			return new PipeRunResult(getForward(), result);
		} 
		catch (Exception e) {
			throw new PipeRunException( this, getLogPrefix(session) + "caught exception", e);
		} 
		finally {
			try {
				getListener().closeThread(threadContext);
			} 
			catch (ListenerException le) {
				log.error(getLogPrefix(session)+"got error closing listener");
			}
		}
	}

	public String getResultOnEmptyPostbox() {
		return resultOnEmptyPostbox;
	}

	@IbisDoc({"result when no object is on postbox", "empty postbox"})
	public void setResultOnEmptyPostbox(String string) {
		resultOnEmptyPostbox = string;
	}

}
