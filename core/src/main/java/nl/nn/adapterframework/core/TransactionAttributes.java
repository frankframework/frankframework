/*
   Copyright 2020, 2021 WeAreFrank!

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
import lombok.Setter;
import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;

public class TransactionAttributes implements HasTransactionAttribute {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter @Setter TransactionAttribute transactionAttribute = TransactionAttribute.SUPPORTS;
	private @Getter int transactionTimeout = 0;

	private @Getter TransactionDefinition txDef = null;

	public void configure() throws ConfigurationException {
		txDef = configureTransactionAttributes(log, getTransactionAttribute(), getTransactionTimeout());
	}

	public static TransactionDefinition configureTransactionAttributes(Logger log, TransactionAttribute transactionAttribute, int transactionTimeout) {
		if (isTransacted(transactionAttribute) && transactionTimeout>0) {
			int maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
			if (maximumTransactionTimeout > 0 && transactionTimeout > maximumTransactionTimeout) {
				ApplicationWarnings.add(log, "transaction timeout ["+transactionTimeout+"] exceeds the maximum transaction timeout ["+maximumTransactionTimeout+"]");
			}
		}

		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+transactionAttribute+"], timeout ["+transactionTimeout+"]");
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute.getTransactionAttributeNum(),transactionTimeout);
	}



	//@IbisDoc({"If set to <code>true</code>, messages will be processed under transaction control. (see below)", "<code>false</code>"})
	@Deprecated
	@ConfigurationWarning("implemented as setting of transacted=true as transactionAttribute=Required and transacted=false as transactionAttribute=Supports")
	public void setTransacted(boolean transacted) {
		if (transacted) {
			setTransactionAttribute(TransactionAttribute.REQUIRED);
		} else {
			setTransactionAttribute(TransactionAttribute.SUPPORTS);
		}
	}
	public boolean isTransacted() {
		return isTransacted(getTransactionAttribute());
	}

	public static boolean isTransacted(TransactionAttribute txAtt) {
		return  txAtt==TransactionAttribute.REQUIRED ||
				txAtt==TransactionAttribute.REQUIRESNEW ||
				txAtt==TransactionAttribute.MANDATORY;
	}

	@Override
	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
}
