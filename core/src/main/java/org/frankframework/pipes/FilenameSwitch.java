/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;


/**
 * Selects an exitState, based on the last (filename) part of the path that is the input.
 *
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;filenname part of the path&gt;</td><td>default</td></tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.8
 */
@Deprecated(forRemoval = true, since = "7.6.0")
@ConfigurationWarning("Please replace with XmlSwitch with an xpathExpression or serviceSelectionStylesheetFilename")
public class FilenameSwitch extends AbstractPipe {

	private String notFoundForwardName=null;
	private boolean toLowercase=true;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				ConfigurationWarnings.add(this, log, "has notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward="";
		String sInput;
		try {
			sInput = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		PipeForward pipeForward=null;

		int slashPos=sInput.lastIndexOf('/');
		if (slashPos>0) {
			sInput=sInput.substring(slashPos+1);
		}
		slashPos=sInput.lastIndexOf('\\');
		if (slashPos>0) {
			sInput=sInput.substring(slashPos+1);
		}
		forward=sInput;
		if (isToLowercase()) {
			forward=forward.toLowerCase();
		}
		log.debug("determined forward [{}]", forward);

		if (findForward(forward) != null)
			pipeForward=findForward(forward);
		else {
			log.info("determined forward [{}], which is not defined. Will use [{}] instead", forward, getNotFoundForwardName());
			pipeForward=findForward(getNotFoundForwardName());
		}

		if (pipeForward==null) {
			throw new PipeRunException (this, "cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, message);
	}

	/** forward returned when the forward or pipename derived from the filename that was the input could not be found. */
	public void setNotFoundForwardName(String notFound){
		notFoundForwardName=notFound;
	}
	public String getNotFoundForwardName(){
		return notFoundForwardName;
	}

	/**
	 * convert the result to lowercase, before searching for a corresponding forward
	 * @ff.default true
	 */
	public void setToLowercase(boolean b) {
		toLowercase = b;
	}
	public boolean isToLowercase() {
		return toLowercase;
	}

}
