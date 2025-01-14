/*
   Copyright 2013,2019 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.extensions.sap;

import org.frankframework.configuration.ConfigurationException;

/**
 * Common interface to be implemented by SapSystem implementations.
 *
 * @author Gerrit van Brakel
 * @since  7.3
 */
public interface ISapSystem extends org.frankframework.configuration.extensions.ISapSystem {

	void setGwhost(String string);
	void setMandant(String string);
	void setAuthAlias(String string);
	void setUserid(String string);
	void setPasswd(String string);
	void setLanguage(String string);
	void setUnicode(boolean b);
	void setMaxConnections(int i);
	void setTraceLevel(int i);
	void setServiceOffset(int i);
	void setSystemnr(String string);

	void setHost(String string);
	void setAshost(String string);
	void setGroup(String string);
	void setR3name(String string);
	void setMshost(String string);
	void setMsservOffset(int i);
	void setGwservOffset(int i);

	void setSncEnabled(boolean sncEnabled);
	void setSncLibrary(String sncLibPath);
	void setSncQop(int qop) throws ConfigurationException;

	void setMyName(String myName);
	void setPartnerName(String partnerName);
	void setSncAuthMethod(String sncAuthMethod);
	void setSncSSO2(String sncSSO2);

}
