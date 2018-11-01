package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

public class AuthInfoResult{
    private String username;
    private String id;
    private String accessToken;
    private String instanceUrl;
    private String connectedStatus;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getConnectedStatus() {
        return connectedStatus;
    }

    public void setConnectedStatus(String connectedStatus) {
        this.connectedStatus = connectedStatus;
    }
}