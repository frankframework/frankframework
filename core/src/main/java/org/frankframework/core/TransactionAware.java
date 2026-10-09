/*
<<<<<<<< HEAD:core/src/main/java/org/frankframework/core/TransactionAware.java
   Copyright 206 WeAreFrank!
========
   Copyright 2013 Nationale-Nederlanden, 2023-2026 WeAreFrank!
>>>>>>>> c60b767b85 (Fix transactional JDBC sender behaviour):core/src/main/java/org/frankframework/core/IXAEnabled.java

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

/**
 * Marker Interface to indicate a class is aware of current transaction. This is used in checks such as in the {@link org.frankframework.processors.TransactionAttributePipeProcessor}.
 */
<<<<<<<< HEAD:core/src/main/java/org/frankframework/core/TransactionAware.java
public interface TransactionAware {
========
public interface IXAEnabled extends TransactionAware {

	/**
	 * indicates implementing object is under transaction control, using XA-transactions
	 */
	boolean isTransacted();

>>>>>>>> c60b767b85 (Fix transactional JDBC sender behaviour):core/src/main/java/org/frankframework/core/IXAEnabled.java
}
