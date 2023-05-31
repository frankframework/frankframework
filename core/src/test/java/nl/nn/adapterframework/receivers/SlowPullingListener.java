/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import nl.nn.adapterframework.core.IPullingListener;

public class SlowPullingListener extends SlowListenerBase implements IPullingListener<javax.jms.Message> {

	@Nonnull
	@Override
	public Map<String, Object> openThread() {
		return new LinkedHashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) {
		log.debug("closeThread called in slow pulling listener");
	}

	@Override
	public RawMessageWrapper<javax.jms.Message> getRawMessage(@Nonnull @Nonnull Map<String, Object> threadContext) {
		return null;
	}
}
