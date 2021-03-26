/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.nn.adapterframework.jta.narayana;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

/**
 * Bean to set up Narayana recovery manager.
 * See org.springframework.boot.jta.narayana.NarayanaRecoveryManagerBean.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaRecoveryManager implements InitializingBean, DisposableBean {

	private RecoveryManager recoveryManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		recoveryManager = RecoveryManager.manager();

		recoveryManager.initialize();
		recoveryManager.startRecoveryManagerThread();
	}

	@Override
	public void destroy() throws Exception {
		this.recoveryManager.terminate();
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
