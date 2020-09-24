/*
   Copyright 2019, 2020 WeAreFrank!

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

import lombok.Getter;

public class SpringBean implements Comparable<SpringBean> {
    private @Getter String name;
    private @Getter Class<?> clazz;

    public SpringBean(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public int compareTo(SpringBean s) {
        return name.compareTo(s.name);
    }

    @Override
    public String toString() {
        return name +  "[" + clazz.getName() + "]";
    }
}