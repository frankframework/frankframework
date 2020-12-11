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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * Baseclass for {@link IBasicFileSystem FileSystems} that use a 'Connection' to connect to their storage.
 * 
 * @author Gerrit van Brakel
 *
 */
public abstract class ConnectedFileSystemBase<F,C extends AutoCloseable> extends FileSystemBase<F> {

	// implementations that have a thread-safe connector can set pooledConnection = false to use a shared connection.
	private @Setter @Getter boolean pooledConnection=true;
	
	private ConnectionProxy globalConnection;
	private ObjectPool<ConnectionProxy> connectionPool;

	protected abstract C createConnection() throws FileSystemException;
	
	protected void closeConnection(C connection) throws Exception {
		connection.close();
	}
	
	/*
	 * Wrapper around Connection, that returns it to pool at close().
	 */
	private class ConnectionProxy {		
		private @Getter C originalConnection;
		private @Getter C proxiedConnection;
		private @Getter @Setter boolean active;
		
		public ConnectionProxy(C connection) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
			originalConnection = connection;

			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass(originalConnection.getClass());

			ConnectionProxy thisObject = this;
			MethodHandler handler = new MethodHandler() {
				@Override
				public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
					if (method.getName().equals("close") && method.getParameterCount()==0) {
						if (connectionPool!=null && isActive()) {
							connectionPool.returnObject(thisObject);
						}
						return null;
					} else {
						try {
							return method.invoke(originalConnection, args);
						} catch (InvocationTargetException e) {
							throw e.getCause();
						}
					}
				}
			};
			
			// this requires C to have a zero argument constructor. 
			// Could maybe avoid this / improve by implementing this proxy via AOP
			proxiedConnection = (C) factory.create(new Class<?>[0], new Object[0], handler);
		}		
	}
	
	@Override
	public void open() throws FileSystemException {
		if (isPooledConnection()) {
			openPool();
		} else {
			try {
				globalConnection = new ConnectionProxy(createConnection());
			} catch (FileSystemException | NoSuchMethodException | IllegalArgumentException | InstantiationException
					| IllegalAccessException | InvocationTargetException e) {
				throw new FileSystemException(e);
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
					try {
						globalConnection.getOriginalConnection().close();
					} catch (Exception e) {
						throw new FileSystemException(e);
					}
				}
			}
		}
	}
	

	/**
	 * Get the Connector from the thread, or a fresh one from the pool.
	 * At close(), it is returned to the pool, if it came from the pool; 
	 * If it was allocated, it is just left allocated.
	 */
	protected C getConnection() throws FileSystemException {
		try {
			return isPooledConnection() ? connectionPool.borrowObject().getProxiedConnection() : globalConnection.getProxiedConnection();
		} catch (Exception e) {
			throw new FileSystemException("Cannot get connector from pool", e);
		}
	}


	private void openPool() {
		if (connectionPool==null) {
			connectionPool=new GenericObjectPool<>(new BasePooledObjectFactory<ConnectionProxy>() {

				@Override
				public ConnectionProxy create() throws Exception {
					return new ConnectionProxy(createConnection());
				}

				@Override
				public PooledObject<ConnectionProxy> wrap(ConnectionProxy connectionProxy) {
					return new DefaultPooledObject<ConnectionProxy>(connectionProxy);
				}

				@Override
				public void destroyObject(PooledObject<ConnectionProxy> p) throws Exception {
					closeConnection(p.getObject().getOriginalConnection());
					super.destroyObject(p);
				}

				@Override
				public void activateObject(PooledObject<ConnectedFileSystemBase<F, C>.ConnectionProxy> p) throws Exception {
					super.activateObject(p);
					p.getObject().setActive(true);
				}

				@Override
				public void passivateObject(PooledObject<ConnectedFileSystemBase<F, C>.ConnectionProxy> p) throws Exception {
					super.passivateObject(p);
					p.getObject().setActive(false);
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
			log.warn("exception clearing Pool",e);
		}
	}

}
