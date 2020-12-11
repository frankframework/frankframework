/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import org.apache.commons.pool2.ObjectPool;

import lombok.Getter;
import lombok.Setter;

/*
 * Wrapper around Connection, that returns it to pool at close().
 */
public class Connector<C> implements AutoCloseable {
	private @Getter C connection;
	private ObjectPool<Connector<C>> pool;
	private @Getter @Setter boolean active;
	
	public Connector(C connection, ObjectPool<Connector<C>> pool) {
		this.connection = connection;
		this.pool = pool;
	}
	
	@Override
	public void close() throws FileSystemException {
		if (pool!=null && isActive()) {
			try {
				pool.returnObject(this);
			} catch (Exception e) {
				throw new FileSystemException(e);
			}
		}
	}

}
