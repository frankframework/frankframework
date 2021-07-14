/*
 * Copyright 2016 - Fabio "MrWHO" Torchetti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wedjaa.ansible.vault;

public class ProvisioningInfo
{

        String apiUser;
        String apiClientId;
        String apiPassword;

        public ProvisioningInfo()
        {

        }

        public String getApiUser()
        {
            return apiUser;
        }

        public void setApiUser(String apiUser)
        {
            this.apiUser = apiUser;
        }

        public String getApiClientId()
        {
            return apiClientId;
        }

        public void setApiClientId(String apiClientId)
        {
            this.apiClientId = apiClientId;
        }

        public String getApiPassword()
        {
            return apiPassword;
        }

        public void setApiPassword(String apiPassword)
        {
            this.apiPassword = apiPassword;
        }

        public String toString()
        {
            return apiUser + "@" + apiClientId + " - " + apiPassword;
        }
}
