package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateArticleMetadataCategoryRequest {
    @JsonProperty
    private String category;
    @JsonProperty private String categoryGroup;

    public String getCategory() {
        return category;
    }

    public String getCategoryGroup() {
        return categoryGroup;
    }
}
