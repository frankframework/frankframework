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

import java.io.InputStream;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Baseclass for {@link IBasicFileSystem FileSystems} that use a 'Connection' to connect to their storage.
 * 
 * @author Gerrit van Brakel
 *
 */
public abstract class ConnectedFileSystemBase<F,C> extends FileSystemBase<F> {

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
			globalConnection = createConnection();
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
		try {
			return isPooledConnection() ? connectionPool.borrowObject() : globalConnection;
		} catch (Exception e) {
			throw new FileSystemException("Cannot get connection from pool of "+ClassUtils.nameOf(this), e);
		}
	}
	
	protected void releaseConnection(C connection) {
		if (isPooledConnection()) {
			try {
				connectionPool.returnObject(connection);
			} catch (Exception e) {
				log.warn("Cannot return connection of "+ClassUtils.nameOf(this), e);
			}
		}
	}
	
	/**
	 * Remove the connection from the pool, e.g. after it has been part of trouble.
	 * If a shared (non-pooled) connection is invalidated, the shared connection is recreated.
	 */
	protected void invalidateConnection(C connection) {
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
			log.warn("Cannot invalidate connection of "+ClassUtils.nameOf(this), e);
		}
	}
	
	/**
	 * Postpone the release of the connection to after the stream is closed.
	 * If any IOExceptions on the stream occur, the connection is invalidated.
	 */
	protected InputStream pendingRelease(InputStream stream, C connection) {
		return StreamUtil.watch(stream, () -> releaseConnection(connection) , () -> invalidateConnection(connection));
	}

	private void openPool() {
		if (connectionPool==null) {
			connectionPool=new GenericObjectPool<>(new BasePooledObjectFactory<C>() {

				@Override
				public C create() throws Exception {
					return createConnection();
				}

				@Override
				public PooledObject<C> wrap(C connection) {
					return new DefaultPooledObject<C>(connection);
				}

				@Override
				public void destroyObject(PooledObject<C> p) throws Exception {
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
			log.warn("exception clearing Pool of "+ClassUtils.nameOf(this),e);
		}
	}

}
