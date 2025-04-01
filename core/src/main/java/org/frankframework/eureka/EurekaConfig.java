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

public class EurekaConfig {
    private String hostName;
    private String appName;
    private String ipAddr;
    private String vipAddress;
    private String secureVipAddress;
    private String status;
    private int port;
    private int securePort;
    private String homePageUrl;
    private String statusPageUrl;
    private String healthCheckUrl;
    private EurekaDataCenterInfo eurekaDataCenterInfo;

    public EurekaConfig(){
        this.hostName = "localhost";
        this.appName = "default-app";
        this.ipAddr = "127.0.0.1";
        this.vipAddress = "localhost";
        this.secureVipAddress = "localhost-secure";
        this.status = "UP";
        this.homePageUrl = "http://localhost:8080";
        this.statusPageUrl = "http://localhost:8080/status";
        this.healthCheckUrl = "http://localhost:8080/health";
        this.eurekaDataCenterInfo = new EurekaDataCenterInfo("MyOwn");
    }

    private void updateUrls() {
        String baseUrl = "http://" + hostName + ":" + port;

        this.homePageUrl = baseUrl + "/home";
        this.healthCheckUrl = baseUrl + "/health";
        this.statusPageUrl = baseUrl + "/status";
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getVipAddress() {
        return vipAddress;
    }

    public void setVipAddress(String vipAddress) {
        this.vipAddress = vipAddress;
    }

    public String getSecureVipAddress() {
        return secureVipAddress;
    }

    public void setSecureVipAddress(String secureVipAddress) {
        this.secureVipAddress = secureVipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        updateUrls();
    }

    public int getSecurePort() {
        return securePort;
    }

    public void setSecurePort(int securePort) {
        this.securePort = securePort;
    }

    public String getHomePageUrl() {
        return homePageUrl;
    }

    public void setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
    }

    public String getStatusPageUrl() {
        return statusPageUrl;
    }

    public void setStatusPageUrl(String statusPageUrl) {
        this.statusPageUrl = statusPageUrl;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    public EurekaDataCenterInfo getEurekaDataCenterInfo() {
        return eurekaDataCenterInfo;
    }

    public String toJson() {
        return "{"
                + "\"appName\":\"" + appName + "\","
                + "\"hostName\":\"" + hostName + "\","
                + "\"ipAddr\":\"" + ipAddr + "\","
                + "\"vipAddress\":\"" + vipAddress + "\","
                + "\"secureVipAddress\":\"" + secureVipAddress + "\","
                + "\"status\":\"" + status + "\","
                + "\"port\":" + port + ","
                + "\"securePort\":" + securePort + ","
                + "\"homePageUrl\":\"" + homePageUrl + "\","
                + "\"statusPageUrl\":\"" + statusPageUrl + "\","
                + "\"healthCheckUrl\":\"" + healthCheckUrl + "\""
                + "}";
    }

}
