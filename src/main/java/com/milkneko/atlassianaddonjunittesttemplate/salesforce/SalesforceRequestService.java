package com.milkneko.atlassianaddonjunittesttemplate.salesforce;

import com.atlassian.json.jsonorg.JSONObject;
import com.milkneko.atlassianaddonjunittesttemplate.salesforce.json.UpdateArticleMetadataRequest;
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
import org.apache.commons.lang3.StringUtils;
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

        return executeService(new HttpGet( buildSalesforceRestUrl(service) ), errorMessage);
    }

    public JSONObject getArticleVersionDraft(String knowledgeArticleId) throws UnsupportedEncodingException {
        JSONObject articleMetadata = getArticleMetadata(knowledgeArticleId);

        String draftVersionId = articleMetadata.getString("draftArticleMasterVersionId");
        if( StringUtils.isEmpty(draftVersionId) ){
            String service = "/knowledgeManagement/articleVersions/masterVersions";
            String errorMessage = "Could not retrieve Draft Article Version of Article with Id " + knowledgeArticleId;
            JSONObject requestBody = new JSONObject();
            requestBody.put("articleId", knowledgeArticleId);

            HttpPost postRequest = new HttpPost( buildSalesforceRestUrl(service) );
            postRequest.setEntity(new StringEntity(requestBody.toString()));

            return executeService(postRequest, errorMessage);
        }

        JSONObject response = new JSONObject();
        response.put("id", draftVersionId);

        return response;
    }

    public JSONObject getMetadata(String knowledgeArticleId){
        Map<String, Object> getRecordTypes = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "Name,SobjectType+" +
                        "FROM+RecordType+WHERE+SobjectType='" + getArticleType() + "'+AND+IsActive=true",
                "record-types",
                "GET"
        );
        Map<String, Object> getArticleEntity = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "IsVisibleInApp,IsVisibleInPkb,IsVisibleInCsp,IsVisibleInPrm," +
                        "RecordTypeId+FROM+" + getArticleType() + "+WHERE+Id='" + knowledgeArticleId + "'",
                "article",
                "GET"
        );
        Map<String, Object> getArticleCategoriesEntity = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "ParentId,DataCategoryGroupName,DataCategoryName+" +
                        "FROM+" + getArticleType().replace(KNOWLEDGE_ARTICLE_TYPE_POSTFIX, "")
                        + "__DataCategorySelection+WHERE+ParentId='" + knowledgeArticleId + "'",
                "categories",
                "GET"
        );

        List<Map<String, Object>> requests = Arrays.asList(
                getRecordTypes,
                getArticleEntity,
                getArticleCategoriesEntity
        );
        String errorMessage = "Could not retrieve Article Metadata with id=" + knowledgeArticleId  + ".";

        return getCompositeResponse(requests, errorMessage).response;
    }

    public JSONObject setMetadata(String knowledgeArticleId, UpdateArticleMetadataRequest updateArticleMetadataRequest){

        JSONObject patchBody = new JSONObject();

        patchBody.put("IsVisibleInPkb", updateArticleMetadataRequest.getChannels().indexOf("IsVisibleInPkb") >= 0);
        patchBody.put("IsVisibleInCsp",  updateArticleMetadataRequest.getChannels().indexOf("IsVisibleInCsp") >= 0);
        patchBody.put("IsVisibleInPrm", updateArticleMetadataRequest.getChannels().indexOf("IsVisibleInPrm") >= 0);
        patchBody.put("RecordTypeId", updateArticleMetadataRequest.getRecordType().getId());

        Map<String, Object> patchArticle = getCompositeRequestEntry(
                "/sobjects/" + getArticleType() +"/" + knowledgeArticleId,
                "article",
                "PATCH",
                patchBody
        );
        Map<String, Object> getArticleCategories = getCompositeRequestEntry(
                "/query/?q=SELECT+Id," +
                        "ParentId,DataCategoryGroupName,DataCategoryName+" +
                        "FROM+" + getArticleType().replace(KNOWLEDGE_ARTICLE_TYPE_POSTFIX, "")
                        + "__DataCategorySelection+WHERE+ParentId='" + knowledgeArticleId + "'",
                "categories",
                "GET"
        );

        CompositeResponse compositeResponse =
                getCompositeResponse(Arrays.asList(patchArticle, getArticleCategories),
                        "Unable to patch or get article metadata with id " + knowledgeArticleId);

        if(!compositeResponse.success){
            return compositeResponse.response;
        }

        JSONObject remoteCategories = compositeResponse.response.getJSONObject("categories");

        List<JSONObject> categories = new ArrayList<>();
        List<JSONObject> categoriesToAdd = new ArrayList<>();
        List<JSONObject> categoriesToDelete = new ArrayList<>();

        remoteCategories.getJSONArray("records").forEach(categoryData -> {
            JSONObject categoryJson = (JSONObject) categoryData;
            categories.add(categoryJson);

            if(
                    updateArticleMetadataRequest.getCategories().stream()
                            .noneMatch(categoryRequest ->
                                (categoryRequest.getCategoryGroup() + ":" + categoryRequest.getCategory()).equals(
                                        categoryJson.getString("DataCategoryGroupName") + ":"
                                                + categoryJson.getString("DataCategoryName")))
            ){
                categoriesToDelete.add(categoryJson);
            }
        });

        updateArticleMetadataRequest.getCategories().forEach(requestCategory -> {
            if(
                    categories.stream().noneMatch(categoryRemote -> (
                            categoryRemote.getString("DataCategoryGroupName") + ":" +
                            categoryRemote.getString("DataCategoryName")
                        ).equals(requestCategory.getCategoryGroup() + ":" + requestCategory.getCategory()))
            ){
                JSONObject categoryToAdd = new JSONObject();
                JSONObject categoryAttributes = new JSONObject();
                categoryAttributes.put("type",getArticleType().replace(KNOWLEDGE_ARTICLE_TYPE_POSTFIX, "")
                                + "__DataCategorySelection");

                categoryToAdd.put("attributes", categoryAttributes);
                categoryToAdd.put("ParentId", knowledgeArticleId);
                categoryToAdd.put("DataCategoryGroupName", requestCategory.getCategoryGroup());
                categoryToAdd.put("DataCategoryName", requestCategory.getCategory());

                categoriesToAdd.add(categoryToAdd);
            }
        });

        if(categoriesToAdd.size() == 0 && categoriesToDelete.size() == 0){
            JSONObject updateResponse = new JSONObject();
            updateResponse.put("patchResult", compositeResponse.response.get("article"));

            return updateResponse;
        }

        List<Map<String, Object>> writeRequests = new ArrayList<>();
        if(categoriesToDelete.size() > 0){
            StringJoiner categoriesToDeleteIds = new StringJoiner(",");
            categoriesToDelete.forEach(categoryToDelete -> {
                categoriesToDeleteIds.add(categoryToDelete.getString("Id"));
            });

            writeRequests.add( getCompositeRequestEntry(
                    "/composite/sobjects?ids=" + categoriesToDeleteIds.toString(),
                    "delete-response",
                    "DELETE"
            ) );
        }
        if(categoriesToAdd.size() > 0){
            JSONObject addBodyJson = new JSONObject();
            addBodyJson.put("allOrNone", false);
            addBodyJson.put("records", categoriesToAdd);

            writeRequests.add( getCompositeRequestEntry(
                    "/composite/sobjects",
                    "add-response",
                    "POST",
                    addBodyJson

            ) );
        }

        CompositeResponse writeCompositeResponse =
                getCompositeResponse(writeRequests,
                        "Unable to update data categories on article with id " + knowledgeArticleId);

        if(!writeCompositeResponse.success){
            return compositeResponse.response;
        }

        JSONObject updateResponse = new JSONObject();
        updateResponse.put("patchResult", compositeResponse.response.get("article"));
        updateResponse.put("updateCategoriesResult", writeCompositeResponse.response);

        return updateResponse;
    }

    private static class CompositeResponse{
        private boolean success;
        private JSONObject response;

        CompositeResponse(boolean success, JSONObject response) {
            this.success = success;
            this.response = response;
        }
    }

    private CompositeResponse getCompositeResponse(List<Map<String, Object>> requests, String errorMessage){
        JSONObject requestEntityData = new JSONObject();
        requestEntityData.put("compositeRequest", requests);

        HttpPost httpPost = new HttpPost(buildSalesforceRestUrl("/composite"));
        try {
            httpPost.setEntity(new StringEntity(requestEntityData.toString()));
        } catch (UnsupportedEncodingException e) {
            logger.error("Error building Composite Request, "
                    + requestEntityData.toString(), e);
            e.printStackTrace();
        }

        JSONObject metadata =
                executeService(httpPost, errorMessage);

        if(metadata == null){
            return new CompositeResponse(false, null);
        }

        // return the error response
        if( !metadata.has("compositeResponse") ){
            return new CompositeResponse(false, metadata);
        }

        for(JSONObject jsonObject : metadata.getJSONArray("compositeResponse").objects()){
            if(jsonObject.getInt("httpStatusCode") != HttpStatus.SC_OK &&
                    jsonObject.getInt("httpStatusCode") != HttpStatus.SC_NO_CONTENT &&
                    jsonObject.getInt("httpStatusCode") != HttpStatus.SC_CREATED &&
                    jsonObject.getInt("httpStatusCode") != HttpStatus.SC_ACCEPTED
            ){
                logger.info(jsonObject.toString());
                return new CompositeResponse(false, new JSONObject().put("error", true)
                        .put("httpStatus", jsonObject.getInt("httpStatusCode"))
                        .put("httpMessage", jsonObject.get("body"))
                        .put("referenceId", jsonObject.getString("referenceId")));
            }
        }

        JSONObject responseJSON = new JSONObject();
        for(JSONObject jsonObject : metadata.getJSONArray("compositeResponse").objects()){
            responseJSON.put(jsonObject.getString("referenceId"), jsonObject.get("body"));
        }
        return new CompositeResponse(true, responseJSON);
    }

    private Map<String, Object> getCompositeRequestEntry(String service, String referenceId, String method){
        return getCompositeRequestEntry(service, referenceId, method, null);
    }

    private Map<String, Object> getCompositeRequestEntry(String service, String referenceId, String method, JSONObject body){
        Map<String, Object> getArticleDataEntity = new HashMap<>();
        getArticleDataEntity.put("method", method);

        getArticleDataEntity.put("url", buildSalesforceRestUrlRelative(service));
        if(body != null){
            getArticleDataEntity.put("body", body);
        }
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
        if ( httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED
        ) {
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
