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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import lombok.Getter;
import lombok.Setter;

/**
 * Baseclass for {@link IBasicFileSystem FileSystems} that use a 'Connection' to connect to their storage.
 * 
 * @author Gerrit van Brakel
 *
 */
public abstract class ConnectedFileSystemBase<F,C> extends FileSystemBase<F> {

	// implementations that have a thread-safe connector can set pooledConnection = false to use a shared connection.
	private @Setter @Getter boolean pooledConnection=true;
	
	private Connector<C> globalConnector;
	private ObjectPool<Connector<C>> connectorPool;

	protected abstract C createConnection() throws FileSystemException;
	
	protected void closeConnection(C connection) throws FileSystemException {
		if (connection instanceof AutoCloseable) {
			try {
				((AutoCloseable) connection).close();
			} catch (Exception e) {
				throw new FileSystemException(e);
			}
		}
	}
	
	
	@Override
	public void open() throws FileSystemException {
		if (isPooledConnection()) {
			openPool();
		} else {
			globalConnector = new Connector<>(createConnection(), null);
		}
		super.open();
	}
	
	@Override
	public void close() throws FileSystemException {
		try {
			super.close();
		} finally {
			if (isPooledConnection()) {
				closePool();
			} else {
				if (globalConnector!=null) {
					closeConnection(globalConnector.getConnection());
				}
			}
		}
	}
	

	/**
	 * Get the Connector from the thread, or a fresh one from the pool.
	 * At close(), it is returned to the pool, if it came from the pool; 
	 * If it was allocated, it is just left allocated.
	 */
	protected Connector<C> getConnector() throws FileSystemException {
		try {
			return isPooledConnection() ? connectorPool.borrowObject() : globalConnector;
		} catch (Exception e) {
			throw new FileSystemException("Cannot get connector from pool", e);
		}
	}
	
	/**
	 * Remove the connector from the pool, e.g. after it has been part of trouble.
	 */
	protected void invalidateConnector(Connector<C> connector) {
		try {
			if (isPooledConnection()) {
				connectorPool.invalidateObject(connector);
			} else {
				try {
					closeConnection(globalConnector.getConnection());
				} finally {
					globalConnector = new Connector(createConnection(), null);
				}
			}
		} catch (Exception e) {
			log.warn("Cannot invalidate connector", e);
		}
	}

	private void openPool() {
		if (connectorPool==null) {
			connectorPool=new GenericObjectPool<>(new BasePooledObjectFactory<Connector<C>>() {

				@Override
				public Connector<C> create() throws Exception {
					return new Connector<C>(createConnection(), connectorPool);
				}

				@Override
				public PooledObject<Connector<C>> wrap(Connector<C> connector) {
					return new DefaultPooledObject<Connector<C>>(connector);
				}

				@Override
				public void destroyObject(PooledObject<Connector<C>> p) throws Exception {
					closeConnection(p.getObject().getConnection());
					super.destroyObject(p);
				}

				@Override
				public void activateObject(PooledObject<Connector<C>> p) throws Exception {
					super.activateObject(p);
					p.getObject().setActive(true);
				}

				@Override
				public void passivateObject(PooledObject<Connector<C>> p) throws Exception {
					super.passivateObject(p);
					p.getObject().setActive(false);
				}

			}); 
		}
	}

	private void closePool() {
		try {
			if (connectorPool!=null) {
				connectorPool.close();
				connectorPool=null;
			}
		} catch (Exception e) {
			log.warn("exception clearing Pool",e);
		}
	}

}
