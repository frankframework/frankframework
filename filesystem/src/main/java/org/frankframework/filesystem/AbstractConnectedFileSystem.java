/*
   Copyright 2020, 2022 WeAreFrank!

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
package org.frankframework.filesystem;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.ClassUtils;

/**
 * Baseclass for {@link IBasicFileSystem FileSystems} that use a 'Connection' to connect to their storage.
 *
 * @author Gerrit van Brakel
 *
 */
public abstract class AbstractConnectedFileSystem<F,C> extends AbstractFileSystem<F> {

	// implementations that have a thread-safe connection can set pooledConnection = false to use a shared connection.
	private @Setter @Getter boolean pooledConnection=true;

	private C globalConnection;
	private ObjectPool<C> connectionPool;

	/**
	 * Create a fresh connection to the FileSystem.
	 */
	protected abstract C createConnection() throws FileSystemException;

	/**
	 * Close connection to the FileSystem, releasing all resources.
	 */
	protected void closeConnection(C connection) throws FileSystemException {
		if (connection instanceof AutoCloseable closeable) {
			try {
				closeable.close();
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
			globalConnection = createConnection();
			if (globalConnection==null) {
				throw new FileSystemException("Cannot create connection");
			}
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
				if (globalConnection!=null) {
					closeConnection(globalConnection);
				}
			}
		}
	}


	/**
	 * Get a Connection from the pool, or the global shared connection.
	 */
	protected C getConnection() throws FileSystemException {
		log.trace("Get Connection from FS, pooled: {}", this::isPooledConnection);
		try {
			return isPooledConnection()
					? connectionPool!=null ? connectionPool.borrowObject() : null // connectionPool can be null if getConnection() is called before open() or after close() is called. This happens in the adapter status page when the adapter is stopped.
					: globalConnection;
		} catch (Exception e) {
			throw new FileSystemException("Cannot get connection from pool of "+ClassUtils.nameOf(this), e);
		}
	}

	/**
	 * Release the connection, return it to the pool or invalidate it.
	 */
	protected void releaseConnection(C connection, boolean invalidateConnection) {
		if (connection!=null) {
			if (invalidateConnection) {
				invalidateConnection(connection);
			} else {
				releaseConnection(connection);
			}
		}
	}

	/**
	 * Release the connection, return it to the pool.
	 * This method should not be called if invalidateConnection() has been called with the same connection, so the typical use case
	 * cannot be in a finally-clause after an exception-clause.
	 */
	private void releaseConnection(C connection) {
		log.trace("Releasing connection, pooled: {}", this::isPooledConnection);
		if (isPooledConnection()) {
			try {
				connectionPool.returnObject(connection);
			} catch (Exception e) {
				log.warn("Cannot return connection of {}", ClassUtils.nameOf(this), e);
			}
		}
	}

	/**
	 * Remove the connection from the pool, e.g. after it has been part of trouble.
	 * If a shared (non-pooled) connection is invalidated, the shared connection is recreated.
	 */
	private void invalidateConnection(C connection) {
		log.trace("Invalidating connection, is pooled: {}", this::isPooledConnection);
		try {
			if (isPooledConnection()) {
				connectionPool.invalidateObject(connection);
			} else {
				try {
					closeConnection(globalConnection);
				} finally {
					globalConnection = createConnection();
				}
			}
		} catch (Exception e) {
			log.warn("Cannot invalidate connection of {}", ClassUtils.nameOf(this), e);
		}
	}

	private void openPool() {
		if (connectionPool==null) {
			connectionPool=new GenericObjectPool<>(new BasePooledObjectFactory<C>() {

				@Override
				public C create() throws Exception {
					log.trace("Adding connection to the pool");
					return createConnection();
				}

				@Override
				public PooledObject<C> wrap(C connection) {
					return new DefaultPooledObject<>(connection);
				}

				@Override
				public void destroyObject(PooledObject<C> p) throws Exception {
					log.trace("Releasing connection from the pool");
					closeConnection(p.getObject());
					super.destroyObject(p);
				}

			});
		}
	}

	private void closePool() {
		try {
			if (connectionPool!=null) {
				connectionPool.close();
				connectionPool=null;
			}
		} catch (Exception e) {
			log.warn("exception clearing Pool of {}", ClassUtils.nameOf(this), e);
		}
	}

}
