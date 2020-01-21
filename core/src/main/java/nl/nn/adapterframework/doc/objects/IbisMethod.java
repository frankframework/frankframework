/*
   Copyright 2019, 2020 Integration Partners

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
package nl.nn.adapterframework.doc.objects;

public class IbisMethod {
    private String methodName; // E.g. registerAdapter
    private String parameterName; // E.g. adapter
    int maxOccurs = -1;

    IbisMethod(String methodName, String parameterName) {
        this.methodName = methodName;
        this.parameterName = parameterName;
        if (methodName.startsWith("set")) {
            maxOccurs = 1;
        } else if (!(methodName.startsWith("add") || methodName.startsWith("register"))) {
            throw new RuntimeException("Unknow verb in method name: " + methodName);
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public String toString() {
        return methodName +  "(" + parameterName + ")";
    }
}