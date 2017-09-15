/*
Copyright 2017 Integration Partners B.V.

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
package nl.nn.adapterframework.http.rest;

import java.util.Comparator;

public class ApiUriComparator implements Comparator<String> {

	public int compare(String uri1, String uri2) {
		int uri1c = uri1.length() - uri1.replace("*", "").length();
		int uri2c = uri2.length() - uri2.replace("*", "").length();

		int result=uri1c-uri2c;
		if (result==0) {
			result=uri1.compareTo(uri2);
		}
		return result;
	}
}
