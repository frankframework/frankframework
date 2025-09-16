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

import java.util.jar.Manifest;

import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;

/**
 * We use our own PluginDescriptor, but can use the handy tools from the
 * {@link ManifestPluginDescriptorFinder parent class} to find the {@link Manifest} file.
 */
public class ManifestDescriptorFinder extends ManifestPluginDescriptorFinder {

	@Override
	protected PluginDescriptor createPluginDescriptor(Manifest manifest) {
		return new PluginInfo(manifest);
	}
}
