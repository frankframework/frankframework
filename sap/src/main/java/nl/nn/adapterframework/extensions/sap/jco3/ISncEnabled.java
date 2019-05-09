/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap.jco3;

public interface ISncEnabled {

	/**
	 * Enables SNC security
	 * @param sncEnabled
	 */
	public void setSncEnabled(boolean sncEnabled);

	public void setSncSSO2(String sncSSO2);

	public void setSncAuthMethod(String sncAuthMethod);

	public void setPartnerName(String partnerName);

	public void setMyName(String myName);

	public void setSncQop(int qop);

	public void setSncLibrary(String sncLibPath);
}
