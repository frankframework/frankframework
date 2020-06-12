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
package nl.nn.adapterframework.webcontrol.api;

import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;

import nl.nn.adapterframework.util.AppConstants;

@Provider(value = Type.Feature, scope = Scope.Server)
public class Swagger extends Swagger2Feature {

	public Swagger() {
		AppConstants appConstants = AppConstants.getInstance();
		setContact("info@wearefrank.nl");
		setTitle("FF!API");
		setDescription(appConstants.getProperty("instance.name") + " API Documentation");
		setVersion(appConstants.getProperty("application.version"));
	}
}
