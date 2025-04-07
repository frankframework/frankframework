/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.components;

import java.util.jar.Manifest;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ModuleInformation {
	private String title;
	private String version;
	private String vendor;

	private String jdkVersion;

	// Through technically these are not required, they are used as properties to declare the version.
	private @Setter String groupId;
	private @Setter String artifactId;

	public ModuleInformation(Manifest manifest) {
		this.title = manifest.getMainAttributes().getValue("Implementation-Title");
		this.version = manifest.getMainAttributes().getValue("Implementation-Version");
		this.vendor = manifest.getMainAttributes().getValue("Implementation-Vendor");
		this.jdkVersion = manifest.getMainAttributes().getValue("Build-Jdk-Spec");

		this.groupId = manifest.getMainAttributes().getValue("groupId");
		this.artifactId = manifest.getMainAttributes().getValue("artifactId");
	}

	@Override
	public final String toString() {
		return "Module ["+title+"] version ["+version+"] developed by ["+vendor+"] on jdk ["+jdkVersion+"]";
	}
}
