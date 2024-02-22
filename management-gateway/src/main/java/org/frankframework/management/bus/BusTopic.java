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
package org.frankframework.management.bus;

public enum BusTopic {

	APPLICATION,
	CONFIGURATION,
	ADAPTER,
	FLOW,
	IBISACTION,
	LOGGING,
	SECURITY_ITEMS,
	JDBC,
	JDBC_MIGRATION,
	DEBUG,
	ENVIRONMENT,
	LOG_CONFIGURATION,
	LOG_DEFINITIONS,
	CONNECTION_OVERVIEW,
	IBISSTORE_SUMMARY,
	INLINESTORAGE_SUMMARY,
	QUEUE,
	HEALTH,
	WEBSERVICES,
	SCHEDULER,
	SERVICE_LISTENER,
	TEST_PIPELINE,
	MESSAGE_BROWSER,
	MONITORING,
	FILE_VIEWER;

	public static final String TOPIC_HEADER_NAME = "topic";
}
