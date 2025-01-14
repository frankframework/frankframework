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
package org.frankframework.core;

import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;

import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;

public enum TransactionAttribute implements DocumentedEnum {

	/** Support a current transaction; create a new one if none exists. */
	@EnumLabel("Required") 		REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
	/** Support a current transaction; execute non-transactionally if none exists. */
	@EnumLabel("Supports") 		SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),
	/** Support a current transaction; throw an exception if no current transaction exists. */
	@EnumLabel("Mandatory") 	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),
	/** Create a new transaction, suspending the current transaction if one exists. */
	@EnumLabel("RequiresNew") 	REQUIRESNEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
	/** Do not support a current transaction; rather always execute non-transactionally. */
	@EnumLabel("NotSupported") 	NOTSUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),
	/** Do not support a current transaction; throw an exception if a current transaction exists. */
	@EnumLabel("Never") 		NEVER(TransactionDefinition.PROPAGATION_NEVER);

	private final @Getter int transactionAttributeNum;

	TransactionAttribute(int transactionAttributeNum) {
		this.transactionAttributeNum=transactionAttributeNum;
	}
}
