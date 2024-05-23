/*
Copyright 2024 WeAreFrank!

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
package org.frankframework.doc;

public enum FrankDocGroupValue {
	@EnumLabel("Pipes")
	PIPE,
	@EnumLabel("Senders")
	SENDER,
	@EnumLabel("Listeners")
	LISTENER,
	@EnumLabel("Validators")
	VALIDATOR,
	@EnumLabel("Wrappers")
	WRAPPER,
	@EnumLabel("TransactionalStorages")
	TRANSACTIONAL_STORAGE,
	@EnumLabel("ErrorMessageFormatters")
	ERROR_MESSAGE_FORMATTER,
	@EnumLabel("Batch")
	BATCH,
	@EnumLabel("Monitoring")
	MONITORING,
	@EnumLabel("Scheduling")
	JOB,
	@EnumLabel("Parameters")
	PARAMETER
	// We omit the others group to simplify the implementation.
	// You cannot explicitly assign FrankElement's to the others group
}
