package com.langleydata.homepoker.config;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class, classes=AWSElasticsearchConfig.class)
@ActiveProfiles(profiles = "prod")
@SpringBootTest(properties = {"aws.es.endpoint=localhost:9200"})
public class AWSElasticsearchConfigTest {
	
	@Autowired
	private AWSElasticsearchConfig config;
	
	@Test
	public void testGetClient() {
		Assert.assertNotNull(config);
		Assert.assertNotNull(config.elasticsearchClient());
	}
}
