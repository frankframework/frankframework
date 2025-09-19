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

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.pf4j.CompoundPluginRepository;
import org.pf4j.DefaultPluginManager;
import org.pf4j.DefaultPluginRepository;
import org.pf4j.JarPluginRepository;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginRepository;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;

import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

public class FrankPluginManager extends DefaultPluginManager {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	public FrankPluginManager(Path pluginDirectory) {
		super(pluginDirectory);

		String applVersion = AppConstants.getInstance().getProperty("application.version", null);
		if (StringUtils.isNotBlank(applVersion) && !isTestOrSnapshotVersion(applVersion)) {
			setSystemVersion(applVersion);
		}
	}

	/**
	 * Don't set the version, or rather keep it set to '0.0.0' when we're developing.
	 */
	private boolean isTestOrSnapshotVersion(String applVersion) {
		return "LOCKED_FF_CORE_VERSION".equals(applVersion) || applVersion.endsWith("-SNAPSHOT");
	}

	@Override
	protected PluginRepository createPluginRepository() {
		return new CompoundPluginRepository()
				.add(new JarPluginRepository(getPluginsRoots()))
				.add(new DefaultPluginRepository(getPluginsRoots()));
	}

	@Override
	public RuntimeMode getRuntimeMode() {
		return "0.0.0".equals(getSystemVersion()) ? RuntimeMode.DEVELOPMENT : RuntimeMode.DEPLOYMENT;
	}

	@Override
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new ManifestDescriptorFinder();
	}

	@Override
	protected boolean isPluginValid(PluginWrapper pluginWrapper) {
		if (isDevelopment()) {
			return true;
		}

		if (pluginWrapper.getDescriptor() instanceof PluginInfo pluginInfo) {
			VersionRange range = pluginInfo.getFrameworkVersion();
			return range == null || range.containsVersion(new DefaultArtifactVersion(getSystemVersion()));
		}

		return super.isPluginValid(pluginWrapper);
	}

	@Override
	protected PluginWrapper loadPluginFromPath(Path pluginPath) {
		PluginWrapper plugin = super.loadPluginFromPath(pluginPath);

		PluginDescriptor descriptor = plugin.getDescriptor();
		APPLICATION_LOG.info("Loading Plugin [{}] version [{}] developed by [{}]", descriptor::getPluginId, descriptor::getVersion, descriptor::getProvider);

		return plugin;
	}
}