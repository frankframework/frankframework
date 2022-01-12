/*
   Copyright 2013,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Common interface to be implemented by SapSystem JCo3 implementations
 * 
 * @author Gerrit van Brakel
 * @since  7.3
 */
public interface ISapSystemJco3 extends ISapSystem {


	public void setHost(String string);
	public void setAshost(String string);
	public void setGroup(String string);
	public void setR3name(String string);
	public void setMshost(String string);
	public void setMsservOffset(int i);
	public void setGwservOffset(int i);

	public void setSncEnabled(boolean sncEnabled);
	public void setSncLibrary(String sncLibPath);
	public void setSncQop(int qop) throws ConfigurationException;

	public void setMyName(String myName);
	public void setPartnerName(String partnerName);
	public void setSncAuthMethod(String sncAuthMethod);
	public void setSncSSO2(String sncSSO2);

}
