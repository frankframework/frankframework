/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.jta.narayana;

import java.io.IOException;
import java.util.Vector;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jboss.narayana.jta.jms.TransactionHelper;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;
import org.springframework.transaction.TransactionSystemException;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreAPI;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import nl.nn.adapterframework.jta.StatusRecordingTransactionManager;

public class NarayanaJtaTransactionManager extends StatusRecordingTransactionManager {

	private static final long serialVersionUID = 1L;

//	private @Getter @Setter int shutdownTimeout = 10000;

	private @Getter RecoveryManager recoveryManager;
	private @Getter TransactionHelper transactionHelper;

	private boolean initialized=false;


	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		initialize();
		return com.arjuna.ats.jta.UserTransaction.userTransaction();
	}

	@Override
	protected TransactionManager createTransactionManager() throws TransactionSystemException {
		initialize();
		TransactionManager result = com.arjuna.ats.jta.TransactionManager.transactionManager();
		transactionHelper = new TransactionHelperImpl(result);
		return result;
	}

	private void initialize() throws TransactionSystemException {
		if (!initialized) {
			initialized=true;
			determineTmUid();
			try {
				arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(getUid());
			} catch (CoreEnvironmentBeanException e) {
				throw new TransactionSystemException("Cannot set TmUid", e);
			}

			recoveryManager = RecoveryManager.manager();
			recoveryManager.initialize();
			recoveryManager.startRecoveryManagerThread();

			XARecoveryModule recoveryModule = new XARecoveryModule();
			recoveryManager.addModule(recoveryModule);
		}
	}

	@Override
	protected boolean shutdownTransactionManager() {
		try {
			
			
			recoveryManager.scan();
			boolean result = recoveryManager.getModules().size()==0;
			showStore();
			recoveryManager.terminate();
			showStore();
			return result;
		} catch (Exception e) {
			log.warn("could not shut down transaction manager", e);
			return false;
		}
	}

	public boolean recoveryStoreEmpty() throws ObjectStoreException {
		RecoveryStore store = StoreManager.getRecoveryStore();
		InputObjectState buff = new InputObjectState();
		return store.allObjUids("Recovery", buff) && !buff.notempty();
	}
	
	public void showStore() throws ObjectStoreException, IOException {
		//RecoveryStore recoveryStore = StoreManager.getRecoveryStore();
		
		RecoveryManager recMan = RecoveryManager.manager();
		Vector<RecoveryModule> modules = recMan.getModules();
		log.info("---> RecoveryModule size ["+modules.size()+"]");
		log.info("---> RecoveryModules toString ["+modules+"]");
		for(RecoveryModule module:modules) {
			log.info("---> module ["+ReflectionToStringBuilder.reflectionToString(module)+"]");
			if (module instanceof XARecoveryModule) {
				XARecoveryModule xaModule = (XARecoveryModule) module;
				xaModule.periodicWorkFirstPass();
			}
		}
		
		
		ObjectStoreAPI store = StoreManager.getTxOJStore();

		InputObjectState buff = new InputObjectState();
		if (store.allTypes(buff)) {
			log.info("---> allTypes ["+buff.unpackString()+"]");
			log.info("---> allTypes string ["+ReflectionToStringBuilder.toString(buff)+"]");
			
			byte[] bytes = buff.buffer();
			
			System.out.println();
			for (int i=0; i<bytes.length; i++) {
//				System.out.println(i+": "+bytes[i]+" ["+((char)bytes[i])+"]");
				System.out.print((char)bytes[i]);
			}
			System.out.println();
			
			if (store.allObjUids("StateManager\\BasicAction\\TwoPhaseCoordinator\\AtomicAction", buff)) {
				
				log.info("---> allObjUids toString ["+buff.toString()+"]");
				log.info("---> allObjUids notempty ["+buff.notempty()+"]");
				log.info("---> allObjUids length ["+buff.length()+"]");
				log.info("---> allObjUids size ["+buff.size()+"]");
				log.info("---> allObjUids type ["+buff.type()+"]");
				log.info("---> allObjUids valid ["+buff.valid()+"]");
				log.info("\n");
				log.info("---> allObjUids string ["+ReflectionToStringBuilder.toString(buff)+"]");
				log.info("---> allObjUids string ["+new String(buff.unpackBytes())+"]");
				log.info("\n");
				log.info("---> allObjUids toString ["+buff.toString()+"]");
				log.info("---> allObjUids notempty ["+buff.notempty()+"]");
				log.info("---> allObjUids length ["+buff.length()+"]");
				log.info("---> allObjUids size ["+buff.size()+"]");
				log.info("---> allObjUids type ["+buff.type()+"]");
				log.info("---> allObjUids valid ["+buff.valid()+"]");
				
			}
		} else {
			log.warn("allTypes returned error");
		};

	}
	


	public void registerXAResourceRecoveryHelper(XAResourceRecoveryHelper xaResourceRecoveryHelper) {
		getXARecoveryModule().addXAResourceRecoveryHelper(xaResourceRecoveryHelper);
	}

	private XARecoveryModule getXARecoveryModule() {
		XARecoveryModule xaRecoveryModule = XARecoveryModule.getRegisteredXARecoveryModule();
		if (xaRecoveryModule != null) {
			return xaRecoveryModule;
		}
		throw new IllegalStateException("XARecoveryModule is not registered with recovery manager");
	}

}
