package com.milkneko.atlassianaddonjunittesttemplate;

import com.atlassian.json.jsonorg.JSONObject;
import com.milkneko.atlassianaddonjunittesttemplate.salesforce.SalesforceRequestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArticleMetadataTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SalesforceRequestService salesforceRequestService;

    @Test
    public void testGetArticleMetadata(){
        JSONObject metadata = salesforceRequestService.getMetadata("ka0Z0000000Ab1SIAS");

        logger.info(metadata.toString());
    }

}
