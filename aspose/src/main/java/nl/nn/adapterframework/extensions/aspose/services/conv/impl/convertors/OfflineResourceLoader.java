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
