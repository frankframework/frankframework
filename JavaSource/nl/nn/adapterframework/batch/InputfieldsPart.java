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
 * $Log: InputfieldsPart.java,v $
 * Revision 1.6  2011-11-30 13:51:57  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2007/07/26 16:08:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.3  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

/**
 * @author  John Dekker
 * @version $Id$
 */
public class InputfieldsPart {
	public static final String version = "$RCSfile: InputfieldsPart.java,v $  $Revision: 1.6 $ $Date: 2011-11-30 13:51:57 $";

	private String value;
	private String description;
	

	public void setValue(String string) {
		value = string;
	}
	public String getValue() {
		return value;
	}


	public void setDescription(String string) {
		description = string;
	}
	public String getDescription() {
		return description;
	}

}
