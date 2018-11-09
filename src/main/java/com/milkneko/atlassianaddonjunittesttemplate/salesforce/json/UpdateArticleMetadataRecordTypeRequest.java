package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateArticleMetadataRecordTypeRequest {
    @JsonProperty
    private String id;
    @JsonProperty private String name;

    public String getId() {
        return id;
    }
}
