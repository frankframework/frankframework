/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.doc;

public interface DocumentedEnum {

	/**
	 * @return Optional 'SimpleName' or label that's used to parse the Enum, should never be null but return <code>name()</code> instead!
	 */
	public default String getLabel() {
		return name();
	}

	// returns the fieldname of the enum.
	public String name();

	/**
	 * @return Optional description to explain the corresponding Label
	 */
	public default String getDescription() {
		return null;
	}
}
