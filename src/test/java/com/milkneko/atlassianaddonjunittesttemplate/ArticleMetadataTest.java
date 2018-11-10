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
import java.util.Iterator;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArticleMetadataTest {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    private String articleVersionId = "ka0Z0000000AbVFIA0";
    private String articleId = "kA0Z00000009JaGKAU";

    @Autowired
    private SalesforceRequestService salesforceRequestService;

    @Test
    public void testGetArticleMetadata(){
        JSONObject metadata = salesforceRequestService.getMetadata(articleVersionId);

        logger.info(metadata.toString());
    }

    @Test
    public void testPutArticleMetadata() throws IOException {
        UpdateArticleMetadataRequest updateArticleMetadataRequest =
                objectMapper.readValue(Thread.currentThread().getContextClassLoader()
                                .getResource("updateArticleMetadata.json"),
                        UpdateArticleMetadataRequest.class);

        JSONObject response = salesforceRequestService.setMetadata(articleVersionId, updateArticleMetadataRequest);

        if(response.has("patchResult") && response.has("updateCategoriesResult")){
            logger.info("success");
        }

        logger.info(response.toString());
    }

    @Test
    public void testPutArticleMetadataManyCategories() throws IOException{
        UpdateArticleMetadataRequest updateArticleMetadataRequest =
                objectMapper.readValue(Thread.currentThread().getContextClassLoader()
                                .getResource("updateArticleMetadata-manyCategories.json"),
                        UpdateArticleMetadataRequest.class);

        JSONObject response = salesforceRequestService.setMetadata(articleVersionId, updateArticleMetadataRequest);
        logger.info(response.toString());

        JSONObject delta = response.getJSONObject("delta");
        printDelta(delta);
    }

    @Test
    public void testPutArticleMetadataWithoutCategories() throws IOException{
        UpdateArticleMetadataRequest updateArticleMetadataRequest =
                objectMapper.readValue(Thread.currentThread().getContextClassLoader()
                                .getResource("updateArticleMetadata-withoutCategories.json"),
                        UpdateArticleMetadataRequest.class);

        JSONObject response = salesforceRequestService.setMetadata(articleVersionId, updateArticleMetadataRequest);
        logger.info(response.toString());

        JSONObject delta = response.getJSONObject("delta");
        printDelta(delta);
    }

    @Test
    public void testGetDraft() throws IOException {
        JSONObject response = salesforceRequestService.getArticleVersionDraft(articleId);
        logger.info(response.toString());
    }

    private void printDelta(JSONObject delta){
        Iterator<String> deltaIterator = delta.keys();
        while (deltaIterator.hasNext()){
            String deltaKey = deltaIterator.next();
            String[] values = (String[]) delta.get(deltaKey);
            System.out.println(deltaKey + "\t" + values[0] + "\t" + values[1]);
        }
    }

}
