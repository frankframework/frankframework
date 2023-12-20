/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.dbms;

import java.sql.SQLType;

class CustomSQLType implements SQLType {

	private final String vendor;
	private final int jdbcTypeNr;

	public CustomSQLType(String vendor, int jdbcTypeNr) {
		this.vendor = vendor;
		this.jdbcTypeNr = jdbcTypeNr;
	}

	@Override
	public String getName() {
		return "CURSOR";
	}

	@Override
	public String getVendor() {
		return vendor;
	}

	@Override
	public Integer getVendorTypeNumber() {
		return jdbcTypeNr;
	}
}
