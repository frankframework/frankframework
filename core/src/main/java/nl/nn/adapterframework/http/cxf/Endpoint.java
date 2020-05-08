/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.http.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;

import nl.nn.adapterframework.util.AppConstants;

public class Endpoint extends EndpointImpl {
	
	private static final String CHECK_PUBLISH_PERMISSION_OPTION = "http.cxf.checkPublishPermission";

	private static boolean checkPublishPermissionOptionSet = AppConstants.getInstance().getBoolean(CHECK_PUBLISH_PERMISSION_OPTION, false);

	public Endpoint(Bus bus, Object implementor) {
		super(bus, implementor);
	}

	@Override
	protected void checkPublishPermission() {
		// if you really want to check the publishPermission, then set CHECK_PUBLISH_PERMISSION_OPTION in DeploymentSpecifics.properties
		if (checkPublishPermissionOptionSet) {
			super.checkPublishPermission();
		// } else {
			// skip publish permission check if not explicitly requested, as it fails on WebSphere if the property 
			// EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER has not been set on the command line.
			// It does not work if set programmatically in the constructor of Endpoint.
		}
	}

}
