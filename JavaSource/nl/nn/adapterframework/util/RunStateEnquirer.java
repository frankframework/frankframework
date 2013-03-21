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
 * $Log: RunStateEnquirer.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/10/27 08:42:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced RunStateEnquiries
 *
 */
package nl.nn.adapterframework.util;

/**
 * Interface to support enquiries about the run state.
 * 
 * @version $Id$
 * @author Gerrit van Brakel
 */
public interface RunStateEnquirer {
	public static final String version="$RCSfile: RunStateEnquirer.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:51:48 $";

	public RunStateEnum getRunState();
	public boolean isInState(RunStateEnum state);	
}
