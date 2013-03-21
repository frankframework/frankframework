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
/*
 * $Log: FilenameSwitch.java,v $
 * Revision 1.6  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.2  2008/02/19 09:58:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2008/02/15 14:09:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;


/**
 * Selects an exitState, based on the last (filename) part of the path that is the input.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FilenameSwitch</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNotFoundForwardName(String) notFoundForwardName}</td><td>Forward returned when the forward or pipename derived from the filename that was the input could not be found.</i></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setToLowercase(boolean) toLowercase}</td><td>convert the result to lowercase, before searching for a corresponding forward</td><td>true</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;filenname part of the path&gt;</td><td>default</td></tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version $Id$
 */
public class FilenameSwitch extends AbstractPipe {
	
    private String notFoundForwardName=null;
    private boolean toLowercase=true;

	public void configure() throws ConfigurationException {
		super.configure();
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				String msg = getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.";
				configWarnings.add(log, msg);
			}
		}
	}
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String forward="";
	    String sInput=(String) input;
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
		log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

		if (findForward(forward) != null) 
			pipeForward=findForward(forward);
		else {
			log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
			pipeForward=findForward(getNotFoundForwardName());
		}
		
		if (pipeForward==null) {
			  throw new PipeRunException (this, getLogPrefix(session)+"cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, input);
	}
	
	
	public void setNotFoundForwardName(String notFound){
		notFoundForwardName=notFound;
	}
	public String getNotFoundForwardName(){
		return notFoundForwardName;
	}
	
	public void setToLowercase(boolean b) {
		toLowercase = b;
	}
	public boolean isToLowercase() {
		return toLowercase;
	}

}
