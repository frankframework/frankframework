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
package org.frankframework.components.plugins;

import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.pf4j.Plugin;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;

import lombok.Getter;

import org.frankframework.components.ComponentInfo;

public class PluginInfo extends ComponentInfo implements PluginDescriptor {

	@Getter
	private final Artifact artifact;

	public PluginInfo(Manifest manifest) {
		super(manifest);

		String artifactId = manifest.getMainAttributes().getValue("Artifact-Id");
		String groupId = manifest.getMainAttributes().getValue("Group-Id");
		if (StringUtils.isNoneBlank(artifactId, groupId, getVersion())) {
			this.artifact = new DefaultArtifact(groupId, artifactId, getVersion(), null, getFormattedTimestamp(), "", null);
		} else {
			this.artifact = null;
		}
	}

	@Override
	public String getPluginId() {
		return getName();
	}

	@Override
	public String getPluginDescription() {
		return getDescription();
	}

	@Override
	public String getRequires() {
		return getFrameworkVersion().getRecommendedVersion().toString();
	}

	@Override
	public String getProvider() {
		return getOrganisation();
	}

	@Override
	public String getLicense() {
		return null;
	}

	@Override
	public String getPluginClass() {
		return Plugin.class.getCanonicalName();
	}

	@Override
	public List<PluginDependency> getDependencies() {
		return Collections.emptyList();
	}

}
