/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import com.aspose.slides.IResourceLoadingArgs;
import com.aspose.words.ResourceLoadingAction;
import com.aspose.words.ResourceLoadingArgs;

import org.frankframework.util.ClassLoaderUtils;

public class OfflineResourceLoader implements com.aspose.words.IResourceLoadingCallback, com.aspose.slides.IResourceLoadingCallback {

	@Override
	public int resourceLoading(IResourceLoadingArgs resourceLoadingArgs) {
		return canLoadResource(resourceLoadingArgs.getOriginalUri());
	}
	@Override
	public int resourceLoading(ResourceLoadingArgs resourceLoadingArgs) {
		return canLoadResource(resourceLoadingArgs.getOriginalUri());
	}

	private int canLoadResource(String resourceUri) {
		return !isAllowed(resourceUri) // Deny when protocol:// is used, i.e: file://, http://, https://, ftp://, ssl://
				? ResourceLoadingAction.SKIP
				: ResourceLoadingAction.DEFAULT;
	}

	public boolean isAllowed(String resourceUri) {
		if (resourceUri.contains(":")) {
			// Check if it's a website
			if (resourceUri.contains("://") || resourceUri.contains(":443") || resourceUri.contains(":80") || resourceUri.startsWith("www.")) {
				return false;
			}

			// If it's not a website but another protocol of sorts, see if it's allowed.
			String protocol = resourceUri.substring(0, resourceUri.indexOf(":"));
			return ClassLoaderUtils.getAllowedProtocols().contains(protocol);
		}

		return true;
	}
}
