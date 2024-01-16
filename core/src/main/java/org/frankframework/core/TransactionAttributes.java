/*
   Copyright 2020-2023 WeAreFrank!

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
package org.frankframework.core;

import org.frankframework.configuration.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationWarning;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.util.LogUtil;

public class TransactionAttributes implements HasTransactionAttribute {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter @Setter TransactionAttribute transactionAttribute = TransactionAttribute.SUPPORTS;
	private @Getter int transactionTimeout = 0;

	private @Getter TransactionDefinition txDef = null;

	public void configure() throws ConfigurationException {
		txDef = configureTransactionAttributes(log, getTransactionAttribute(), getTransactionTimeout());
	}

	public static TransactionDefinition configureTransactionAttributes(Logger log, TransactionAttribute transactionAttribute, int transactionTimeout) {
		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+transactionAttribute+"], timeout ["+transactionTimeout+"]");
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute.getTransactionAttributeNum(),transactionTimeout);
	}

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
