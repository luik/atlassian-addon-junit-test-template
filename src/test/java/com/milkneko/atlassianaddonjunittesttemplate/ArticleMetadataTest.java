package com.milkneko.atlassianaddonjunittesttemplate;

import com.atlassian.json.jsonorg.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milkneko.atlassianaddonjunittesttemplate.salesforce.SalesforceRequestService;
import com.milkneko.atlassianaddonjunittesttemplate.salesforce.json.UpdateArticleMetadataRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArticleMetadataTest {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    private String articleId = "ka0Z0000000AbVFIA0";

    @Autowired
    private SalesforceRequestService salesforceRequestService;

    @Test
    public void testGetArticleMetadata(){
        JSONObject metadata = salesforceRequestService.getMetadata(articleId);

        logger.info(metadata.toString());
    }

    @Test
    public void testPutArticleMetadata() throws IOException {
        UpdateArticleMetadataRequest updateArticleMetadataRequest =
                objectMapper.readValue(Thread.currentThread().getContextClassLoader().getResource("updateArticleMetadata.json"),
                        UpdateArticleMetadataRequest.class);

        JSONObject response = salesforceRequestService.setMetadata(articleId, updateArticleMetadataRequest);
        logger.info(response.toString());
    }

}
