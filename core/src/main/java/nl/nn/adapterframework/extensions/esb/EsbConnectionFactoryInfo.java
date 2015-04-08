/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

public class EsbConnectionFactoryInfo {
	private Object managedConnectionFactory;
	private String contextFactoryClassname;
	private String url;
	private String userName;
	private String password;

	public EsbConnectionFactoryInfo(Object managedConnectionFactory,
			String contextFactoryClassname, String url, String userName,
			String password) {
		super();
		this.managedConnectionFactory = managedConnectionFactory;
		this.contextFactoryClassname = contextFactoryClassname;
		this.url = url;
		this.userName = userName;
		this.password = password;
	}

	public Object getManagedConnectionFactory() {
		return managedConnectionFactory;
	}

	public String getContextFactoryClassname() {
		return contextFactoryClassname;
	}

	public String getUrl() {
		return url;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}
}
