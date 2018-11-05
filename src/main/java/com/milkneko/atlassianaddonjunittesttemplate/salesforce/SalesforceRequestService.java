package com.milkneko.atlassianaddonjunittesttemplate.salesforce;

import com.atlassian.json.jsonorg.JSONObject;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
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
import java.io.UnsupportedEncodingException;
import java.util.*;

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

        return executeService(new HttpGet(service), errorMessage);
    }

    public JSONObject getMetadata(String knowledgeArticleId){
        Map<String, String> getRecordTypes = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "Name,SobjectType+" +
                        "FROM+RecordType+WHERE+SobjectType='" + getArticleType() + "'+AND+IsActive=true",
                "record-types"
        );
        Map<String, String> getArticleEntity = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "IsVisibleInApp,IsVisibleInPkb,IsVisibleInCsp,IsVisibleInPrm," +
                        "RecordTypeId+FROM+" + getArticleType() + "+WHERE+Id='" + knowledgeArticleId + "'",
                "article"
        );
        Map<String, String> getArticleCategoriesEntity = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "ParentId,DataCategoryGroupName,DataCategoryName+" +
                        "FROM+" + getArticleType().replace(KNOWLEDGE_ARTICLE_TYPE_POSTFIX, "")
                        + "__DataCategorySelection+WHERE+ParentId='" + knowledgeArticleId + "'",
                "categories"
        );

        JSONObject requestEntityData = new JSONObject();
        requestEntityData.put("compositeRequest", Arrays.asList(
                getRecordTypes,
                getArticleEntity,
                getArticleCategoriesEntity
        ));

        HttpPost httpPost = new HttpPost(buildSalesforceRestUrl("/composite"));
        try {
            httpPost.setEntity(new StringEntity(requestEntityData.toString()));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error building Composite Request, "
                    + requestEntityData.toString(), e);
            e.printStackTrace();
        }

        JSONObject metadata =
                executeService(httpPost, "Could not retrieve Article Metadata with id=" + knowledgeArticleId  + ".");

        if(metadata == null){
            return null;
        }

        // return the error response
        if( !metadata.has("compositeResponse") ){
            return metadata;
        }

        for(JSONObject jsonObject : metadata.getJSONArray("compositeResponse").objects()){
            if(jsonObject.getInt("httpStatusCode") != HttpStatus.SC_OK){
                logger.info(jsonObject.toString());
                return new JSONObject().put("error", true)
                        .put("httpStatus", jsonObject.getInt("httpStatusCode"))
                        .put("httpMessage", jsonObject.get("body"))
                        .put("referenceId", jsonObject.getString("referenceId"));
            }
        }

        JSONObject responseJSON = new JSONObject();
        for(JSONObject jsonObject : metadata.getJSONArray("compositeResponse").objects()){
            responseJSON.put(jsonObject.getString("referenceId"), jsonObject.get("body"));
        }
        return responseJSON;
    }

    private Map<String, String> getCompositeRequestEntry(String service, String referenceId){
        Map<String, String> getArticleDataEntity = new HashMap<>();
        getArticleDataEntity.put("method", "GET");

        getArticleDataEntity.put("url", buildSalesforceRestUrlRelative(service));
        getArticleDataEntity.put("referenceId", referenceId);

        return getArticleDataEntity;
    }

    private JSONObject executeService(HttpRequestBase httpRequest, String errorMessage){
        try {
            HttpResponse httpResponse = sendRequest(httpRequest);
            String responseContent = EntityUtils.toString(httpResponse.getEntity());
            return getResponse(httpResponse, responseContent);
        } catch (Exception ex) {
            logger.error(errorMessage, ex);
        } finally {
            httpRequest.releaseConnection();
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
        return salesforceConfigProvider.getInstanceUrl() + buildSalesforceRestUrlRelative(service);
    }

    private String buildSalesforceRestUrlRelative(String service){
        return "/services/data/" + apiVersion  + service;
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
