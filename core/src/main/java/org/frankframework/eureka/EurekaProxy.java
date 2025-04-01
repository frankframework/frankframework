/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.eureka;

import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EurekaProxy {
    private final RestClient restClient;

 
    public EurekaProxy() {
        this.restClient = EurekaSingleton.getInstance().getRestClient();
    }
    
    public String getServiceByName(String appID){
        return restClient.get()
        .uri(EurekaRequestFactory.eurekaBaseQuery() + appID)
        .retrieve()
        .body(String.class); 
    }

    public String getHomePageUrl(String appID){
        String response = getServiceByName(appID);
        try {
            Document doc = XmlUtils.buildDomDocument(response);
            Element root = doc.getDocumentElement();
            NodeList instanceList = root.getElementsByTagName("instance");
            if (instanceList.getLength() > 0) {
                Element instanceElement = (Element) instanceList.item(0);
                String homePageUrl = XmlUtils.getChildTagAsString(instanceElement, "homePageUrl");
                return homePageUrl;
            }
        } catch (DomBuilderException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registerService(EurekaConfig config){
        String jsonBody = config.toJson();
        return restClient.post()
            .uri(EurekaRequestFactory.eurekaBaseQuery() + config.getAppName())
            .body(jsonBody)
            .retrieve()
            .toBodilessEntity()
            .getStatusCode().is2xxSuccessful();
    } 
}
