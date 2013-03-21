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
package nl.nn.adapterframework.extensions.fxf;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder forTrigger message.
 * 
 * @author  Gerrit van Brakel
 * @since   FXF 2.0
 * @version $Id$
 */
public class Trigger {
	
	private String version;
	private String action;
	private String transporthandle;
	private List transfers;
	
	public Trigger() {
		super();
		transfers=new ArrayList();
	}


	public void setVersion(String string) {
		version = string;
	}
	public String getVersion() {
		return version;
	}

	public void setAction(String string) {
		action = string;
	}
	public String getAction() {
		return action;
	}

	public void setTransporthandle(String string) {
		transporthandle = string;
	}
	public String getTransporthandle() {
		return transporthandle;
	}

	public List getTransfers() {
		return transfers;
	}

	public void registerTransfer(Transfer transfer) {
		transfers.add(transfer);
	}
	public Transfer getTransfer(int index) {
		return (Transfer)transfers.get(index);
	}
}
