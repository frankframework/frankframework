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
	PIPES,
	@EnumLabel("Senders")
	SENDERS,
	@EnumLabel("Listeners")
	LISTENERS,
	@EnumLabel("Validators")
	VALIDATORS,
	@EnumLabel("Wrappers")
	WRAPPERS,
	@EnumLabel("TransactionalStorages")
	TRANSACTIONAL_STORAGES,
	@EnumLabel("ErrorMessageFormatters")
	ERROR_MESSAGE_FORMATTERS,
	@EnumLabel("Batch")
	BATCH,
	@EnumLabel("Monitoring")
	MONITORING,
	@EnumLabel("Job")
	JOB
	// We omit the others group to simplify the implementation.
	// You cannot explicitly assign FrankElement's to the others group
}
