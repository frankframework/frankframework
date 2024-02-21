/*
   Copyright 2024 WeAreFrank!

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
package nl.nn.adapterframework.jta;

import java.time.Duration;

import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

public class CustomJmsPoolConnectionFactory extends JmsPoolConnectionFactory implements CustomPoolExtensions {

	@Override
	public void setMaxConnections(int maxConnections) {
		// Super-method sets both max-idle and max-total connections. Set only max-total.
		this.getConnectionsPool().setMaxTotalPerKey(maxConnections);
	}

	@Override
	public int getMaxConnections() {
		// Super-method actually gets the max-idle per key, b/c it would set that equal to the max total.
		return getConnectionsPool().getMaxTotalPerKey();
	}

	@Override
	public void setMaxIdle(int maxIdle) {
		this.getConnectionsPool().setMaxIdlePerKey(maxIdle);
	}

	@Override
	public int getMaxIdle() {
		return getConnectionsPool().getMaxIdlePerKey();
	}

	@Override
	public int getNumIdle() {
		return getConnectionsPool().getNumIdle();
	}

	@Override
	public void setMaxIdleTimeSeconds(int maxIdleTimeSeconds) {
		this.getConnectionsPool().setMinEvictableIdleDuration(Duration.ofSeconds(maxIdleTimeSeconds));
	}
}
