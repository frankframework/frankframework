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
import nl.nn.adapterframework.extensions.sap.ISapSystem;

public interface ISapSystem3 extends ISapSystem, ISncEnabled {

	public void setAshost(String string);
	public void setGroup(String string);
	public void setGwservOffset(int i);
	public void setHost(String string);
	public void setMshost(String string);
	public void setMsservOffset(int i);
	public void setR3name(String string);
}
