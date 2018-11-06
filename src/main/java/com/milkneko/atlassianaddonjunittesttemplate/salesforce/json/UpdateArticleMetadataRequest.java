package com.milkneko.atlassianaddonjunittesttemplate.salesforce.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UpdateArticleMetadataRequest {
    @JsonProperty("Categories") private List<String> Categories;
    @JsonProperty("CategoryGroup") private String CategoryGroup;

    @JsonProperty("IsVisibleInCsp") private boolean IsVisibleInCsp;
    @JsonProperty("IsVisibleInPkb") private boolean IsVisibleInPkb;
    @JsonProperty("IsVisibleInPrm") private boolean IsVisibleInPrm;
    @JsonProperty("RecordTypeId") private String RecordTypeId;

    public UpdateArticleMetadataRequest() {
    }

    @JsonIgnore public List<String> getCategories() {
        return Categories;
    }

    @JsonIgnore public void setCategories(List<String> categories) {
        Categories = categories;
    }

    @JsonIgnore public String getCategoryGroup() {
        return CategoryGroup;
    }

    @JsonIgnore public void setCategoryGroup(String categoryGroup) {
        CategoryGroup = categoryGroup;
    }

    @JsonIgnore public boolean isVisibleInCsp() {
        return IsVisibleInCsp;
    }

    @JsonIgnore public void setVisibleInCsp(boolean visibleInCsp) {
        IsVisibleInCsp = visibleInCsp;
    }

    @JsonIgnore public boolean isVisibleInPkb() {
        return IsVisibleInPkb;
    }

    @JsonIgnore public void setVisibleInPkb(boolean visibleInPkb) {
        IsVisibleInPkb = visibleInPkb;
    }

    @JsonIgnore public boolean isVisibleInPrm() {
        return IsVisibleInPrm;
    }

    @JsonIgnore public void setVisibleInPrm(boolean visibleInPrm) {
        IsVisibleInPrm = visibleInPrm;
    }

    @JsonIgnore public String getRecordTypeId() {
        return RecordTypeId;
    }

    @JsonIgnore public void setRecordTypeId(String recordTypeId) {
        RecordTypeId = recordTypeId;
    }
}
