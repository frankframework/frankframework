/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.util.LogUtil;

public abstract class AdapterLifecycleWrapperBase {
	protected final Logger log = LogUtil.getLogger(this);

	public abstract void addAdapter(Adapter adapter);

	public abstract void removeAdapter(Adapter adapter);
}
