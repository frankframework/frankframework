/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.lifecycle;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.InitializingBean;

import org.frankframework.util.AppConstants;

@IbisInitializer
public class LoadBouncyCastleBean implements InitializingBean {
	private static final boolean LOAD_BC = AppConstants.getInstance().getBoolean("application.crypto.bouncycastle.enabled", true);

	/**
	 * Register the BouncyCastle provider as first provider. This is a requirement for BouncyGPG.
	 * This procedure also makes it possible to use BC on devices that ship their own BC implementation.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!LOAD_BC) {
			return;
		}

		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
			Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
		}

		// Always load BouncyCastle
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

}
