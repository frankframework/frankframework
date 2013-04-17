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
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;

/**
 * Plain extension of {@link ReceiverBase} that can be used directly in configurations.
 * Only extension is that the setters for its three worker-objects are public, and can therefore
 * be set from the configuration file.
 * For configuration options, see {@link ReceiverBase}.
 * 
 * @version $Id$
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class GenericReceiver extends ReceiverBase {
	public static final String version="$RCSfile: GenericReceiver.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:54 $";

	public void setListener(IListener listener) {
		super.setListener(listener);
	}
	public void setInProcessStorage(ITransactionalStorage inProcessStorage) {
		super.setInProcessStorage(inProcessStorage);
	}
	public void setErrorSender(ISender errorSender) {
		super.setErrorSender(errorSender);
	}			
	public void setErrorStorage(ITransactionalStorage errorStorage) {
		super.setErrorStorage(errorStorage);
	}
	public void setMessageLog(ITransactionalStorage messageLog) {
		super.setMessageLog(messageLog);
	}
	
	public void setSender(ISender sender) {
		super.setSender(sender);
	}
    
    public IAdapter getAdapter() {
        return super.getAdapter();
    }
}
