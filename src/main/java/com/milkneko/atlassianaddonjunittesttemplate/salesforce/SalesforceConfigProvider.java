package com.milkneko.atlassianaddonjunittesttemplate.salesforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milkneko.atlassianaddonjunittesttemplate.salesforce.json.AuthInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SalesforceConfigProvider {

    private AuthInfo authInfo;

    public SalesforceConfigProvider() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        authInfo = objectMapper.readValue(
                Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("authInfo.json"), AuthInfo.class);
    }

    public String getAccessToken(){
        return authInfo.getResult().getAccessToken();
    }

    public String getInstanceUrl(){
        return authInfo.getResult().getInstanceUrl();
    }


}
