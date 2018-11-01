package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

public class AuthInfo{
    private int status;
    private AuthInfoResult result;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public AuthInfoResult getResult() {
        return result;
    }

    public void setResult(AuthInfoResult result) {
        this.result = result;
    }
}