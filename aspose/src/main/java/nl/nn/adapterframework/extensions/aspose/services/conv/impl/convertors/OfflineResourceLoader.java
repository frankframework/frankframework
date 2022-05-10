/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import com.aspose.words.IResourceLoadingCallback;
import com.aspose.words.ResourceLoadingAction;
import com.aspose.words.ResourceLoadingArgs;

public class OfflineResourceLoader implements IResourceLoadingCallback {
	@Override
	public int resourceLoading(ResourceLoadingArgs resourceLoadingArgs) throws Exception {
		String originalUri = resourceLoadingArgs.getOriginalUri();
		if(originalUri.startsWith("https://") || originalUri.startsWith("http://")){
			return ResourceLoadingAction.SKIP;
		}
		return ResourceLoadingAction.DEFAULT;
	}
}
