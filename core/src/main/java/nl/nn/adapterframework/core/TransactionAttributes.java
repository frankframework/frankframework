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
package nl.nn.adapterframework.core;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

public class TransactionAttributes implements HasTransactionAttribute {
	protected Logger log = LogUtil.getLogger(this);

	private int transactionAttribute = TransactionDefinition.PROPAGATION_SUPPORTS;
	private @Getter int transactionTimeout = 0;

	private @Getter TransactionDefinition txDef = null;

	public void configure() throws ConfigurationException {
		txDef = configureTransactionAttributes(log, getTransactionAttributeNum(), getTransactionTimeout());
	}
	
	public static TransactionDefinition configureTransactionAttributes(Logger log, int transactionAttribute, int transactionTimeout) throws ConfigurationException {
		if (isTransacted(transactionAttribute) && transactionTimeout>0) {
			Integer maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
			if (maximumTransactionTimeout != null && transactionTimeout > maximumTransactionTimeout) {
				ApplicationWarnings.add(log, "transaction timeout ["+transactionTimeout+"] exceeds the maximum transaction timeout ["+maximumTransactionTimeout+"]");
			}
		}

		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+JtaUtil.getTransactionAttributeString(transactionAttribute)+"], timeout ["+transactionTimeout+"]");
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute,transactionTimeout);
	}
	
	
	@Override
	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	@Override
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

	@Override
	@Deprecated
	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	@Override
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	//@IbisDoc({"4", "If set to <code>true</code>, messages will be processed under transaction control. (see below)", "<code>false</code>"})
	@Deprecated
	@ConfigurationWarning("implemented as setting of transacted=true as transactionAttribute=Required and transacted=false as transactionAttribute=Supports")
	public void setTransacted(boolean transacted) {
		if (transacted) {
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_REQUIRED);
		} else {
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_SUPPORTS);
		}
	}
	public boolean isTransacted() {
		return isTransacted(getTransactionAttributeNum());
	}

	public static boolean isTransacted(int txAtt) {
		return  txAtt==TransactionDefinition.PROPAGATION_REQUIRED || 
				txAtt==TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				txAtt==TransactionDefinition.PROPAGATION_MANDATORY;
	}

	@Override
	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
}
