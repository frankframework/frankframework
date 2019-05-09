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
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.core.INamedObject;

public interface ISapSystem extends INamedObject{

	public void configure();
	public void openSystem() throws SapException;
	public void closeSystem();

	public void setMandant(String string);
	public String getMandant();

	public void setGwhost(String string);
	public String getGwhost();

	public void setSystemnr(String string);
	public String getSystemnr();

	public void setUnicode(boolean b);
	public boolean isUnicode();

	public void setTraceLevel(int i);
	public int getTraceLevel();

	public void registerItem(Object item);

	public void setAuthAlias(String string);
	public void setUserid(String string);
	public void setPasswd(String string);

	public void setLanguage(String string);
	public void setMaxConnections(int i);
	public void setServiceOffset(int i);
}
