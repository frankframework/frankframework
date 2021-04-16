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
package nl.nn.adapterframework.jta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class ThreadConnectorHelperTransactionManagerProxyHandler implements MethodHandler {

	private AbstractPlatformTransactionManager txManager;
	
	private ThreadLocal<Object> parentThreadsTransaction = new ThreadLocal<>();
	
	public ThreadConnectorHelperTransactionManagerProxyHandler(AbstractPlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	
	public AbstractPlatformTransactionManager getProxy() throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(AbstractPlatformTransactionManager.class);
		return (AbstractPlatformTransactionManager)factory.create(new Class[0], new Object[0], this);
	}

	@Override
	public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
		if (method.getName().equals("doGetTransaction")) {
			return doGetTransaction();
		}
		method.setAccessible(true);
		return method.invoke(txManager, args);
	}

	public Object doGetTransaction() throws TransactionException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> txManagerClass = txManager.getClass();
		Method doGetTransaction = txManagerClass.getDeclaredMethod("doGetTransaction");
		doGetTransaction.setAccessible(true);
		Object transaction = doGetTransaction.invoke(txManager);
		boolean transactionExists=false;;
		if (transaction!=null) {
			Method isExistingTransaction = txManagerClass.getDeclaredMethod("isExistingTransaction", Object.class);
			isExistingTransaction.setAccessible(true);
			transactionExists = (Boolean)isExistingTransaction.invoke(txManager, transaction);
		}
		if (transaction==null || !transactionExists) {
			Object parentTransaction = parentThreadsTransaction.get();
			if (parentTransaction!=null) {
				transaction = parentTransaction;
			}
		}
		return transaction;
	}

	public void joinParentThreadsTransaction(Object transaction) {
		parentThreadsTransaction.set(transaction);
	}
}
