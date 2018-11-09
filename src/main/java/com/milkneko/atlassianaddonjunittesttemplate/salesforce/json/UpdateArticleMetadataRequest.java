package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UpdateArticleMetadataRequest {
    @JsonProperty("channels") private List<String> channels;
    @JsonProperty("recordType") private UpdateArticleMetadataRecordTypeRequest recordType;
    @JsonProperty("categories") private List<UpdateArticleMetadataCategoryRequest> categories;

    @JsonIgnore public List<String> getChannels() {
        return channels;
    }

    @JsonIgnore public UpdateArticleMetadataRecordTypeRequest getRecordType() {
        return recordType;
    }

    @JsonIgnore public List<UpdateArticleMetadataCategoryRequest> getCategories() {
        return categories;
    }

}
