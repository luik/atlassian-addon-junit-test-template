package com.milkneko.atlassianaddonjunittesttemplate.salesforce;

import com.atlassian.json.jsonorg.JSONObject;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpStatus;
import com.atlassian.json.jsonorg.JSONArray;

import javax.ws.rs.core.MediaType;

@Component
public class SalesforceRequestService {

    private static String KNOWLEDGE_ARTICLE_TYPE_POSTFIX = "__kav";
    private final SalesforceConfigProvider salesforceConfigProvider;
    private HttpClient httpClient;
    private Logger logger = Logger.getLogger(getClass());
    private String apiVersion = "v44.0";

    @Autowired
    public SalesforceRequestService(SalesforceConfigProvider salesforceConfigProvider){
        httpClient = HttpClients.createDefault();

        this.salesforceConfigProvider = salesforceConfigProvider;
    }


    public JSONObject getArticleMetadata(String knowledgeArticleId) {
        String service = "/knowledgeManagement/articles/" + knowledgeArticleId;
        String errorMessage = "Could not retrieve Article Metadata with id=" + knowledgeArticleId  + ".";

        return executeService(service, errorMessage);
    }

    public JSONObject getMetadata(String knowledgeArticleId){
        String service = "/query/?q=SELECT+Id," +
                "IsVisibleInApp,IsVisibleInPkb,IsVisibleInCsp,IsVisibleInPrm," +
                "RecordTypeId+FROM+" + getArticleType() + "+WHERE+Id='" + knowledgeArticleId + "'";

        logger.info(service);

        String errorMessage = "Could not retrieve Metadata with id=" + knowledgeArticleId  + ".";
        JSONObject recordTypeAndChannelsData = executeService(service, errorMessage);

        if(recordTypeAndChannelsData == null){
            return null;
        }

        service = "/query/?q=SELECT+Id," +
                "ParentId,DataCategoryGroupName,DataCategoryName+" +
                "FROM+" + getArticleType().replace(KNOWLEDGE_ARTICLE_TYPE_POSTFIX, "")
                + "__DataCategorySelection+WHERE+ParentId='" + knowledgeArticleId + "'";

        logger.info(service);

        JSONObject categoriesData = executeService(service, errorMessage);

        if(categoriesData == null){
            return null;
        }

        JSONObject responseJSON = new JSONObject();
        responseJSON.put("metadata", recordTypeAndChannelsData);
        responseJSON.put("categoriesMetadata", categoriesData);

        return responseJSON;
    }

    private JSONObject executeService(String service, String errorMessage){
        HttpGet httpGet = new HttpGet(buildSalesforceRestUrl(service));
        try {
            HttpResponse httpResponse = sendRequest(httpGet);
            String responseContent = EntityUtils.toString(httpResponse.getEntity());
            return getResponse(httpResponse, responseContent);
        } catch (Exception ex) {
            logger.error(errorMessage, ex);
        } finally {
            httpGet.releaseConnection();
        }
        return null;
    }

    private JSONObject getResponse(HttpResponse httpResponse, String responseContent) {
        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return new JSONObject(responseContent);
        }
        else {
            JSONArray response = new JSONArray(responseContent);
            logger.debug("Status=" + httpResponse.getStatusLine() + ", Body=" + response);
            return new JSONObject().put("error", true).put("httpStatus", httpResponse.getStatusLine().getStatusCode()).put("httpMessage", response.getJSONObject(0).getString("message"));
        }
    }

    private String buildSalesforceRestUrl(String service) {
        return salesforceConfigProvider.getInstanceUrl() + "/services/data/" + apiVersion  + service;
    }

    private HttpResponse sendRequest(HttpRequestBase request) throws ClientProtocolException, Exception {
        request.setHeader(HttpHeaders.AUTHORIZATION, "OAuth " + salesforceConfigProvider.getAccessToken());
        request.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return httpClient.execute(request);
    }

    private String getArticleType(){
        return "Knowledge__kav";
    }

}
