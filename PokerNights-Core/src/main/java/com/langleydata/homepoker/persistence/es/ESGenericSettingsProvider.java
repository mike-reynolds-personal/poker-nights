package com.langleydata.homepoker.persistence.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.persistence.ElasticSettings;
import com.langleydata.homepoker.persistence.GenericSettingsProvider;
import com.langleydata.homepoker.services.Account;

/** DAO for Elasticsearch
 * 
 * @author Mike Reynolds
 *
 */
@Profile( value = {"prod", "test"})
@Component
public class ESGenericSettingsProvider implements GenericSettingsProvider {
	protected final Logger logger = LoggerFactory.getLogger(ESGenericSettingsProvider.class);
	
	@Autowired
	private RestHighLevelClient esClient;
	private Gson gson = new Gson();

	@Override
	public boolean storeSettings(final String gameId, Map<String, Object> document) throws IOException {
		IndexRequest ir = new IndexRequest(ElasticSettings.TEXAS_SETTINGS_IDX).id(gameId).source(document);
		IndexResponse resp = esClient.index(ir, RequestOptions.DEFAULT);
		return resp.getResult() == Result.UPDATED || resp.getResult() == Result.CREATED;
	}

	@Override
	public Map<String, Object> getSettingsById(final String gameId) throws IOException {
		final GetResponse resp = esClient.get(new GetRequest(ElasticSettings.TEXAS_SETTINGS_IDX, gameId),
				RequestOptions.DEFAULT);
		if (resp.isExists()) {
			return resp.getSourceAsMap();
		}
		return new HashMap<>();
	}

	@Override
	public long getSettingsCount() throws IOException {
		final CountResponse cr = esClient.count(new CountRequest(ElasticSettings.TEXAS_SETTINGS_IDX), RequestOptions.DEFAULT);
		return cr.getCount();
	}
	
	@Override
	public boolean storeAccount(UserAccount userAccount) throws IOException {
		final String source = gson.toJson(userAccount);
		IndexRequest ir = new IndexRequest(ElasticSettings.ACCOUNT_IDX).id(userAccount.getPlayerId()).source(source, XContentType.JSON);
		IndexResponse resp = esClient.index(ir, RequestOptions.DEFAULT);
		return resp.getResult() == Result.UPDATED || resp.getResult() == Result.CREATED;
	}
	
	@Override
	public String getAccountById(final String userId) throws IOException {
		final GetResponse resp = esClient.get(new GetRequest(ElasticSettings.ACCOUNT_IDX, userId),
				RequestOptions.DEFAULT);
		if (resp.isExists()) {
			return resp.getSourceAsString();
		}
		return null;
	}

	
	@Override
	public String deleteAccountById(String userId) throws IOException {
		final DeleteResponse dr = esClient.delete(new DeleteRequest(ElasticSettings.ACCOUNT_IDX, userId), RequestOptions.DEFAULT);
		if (dr.getResult()==Result.DELETED) {
			return dr.getId();
		}
		return null;
	}
	
	@Override
	public long getAccountCount() throws IOException {
		final CountResponse cr = esClient.count(new CountRequest(ElasticSettings.ACCOUNT_IDX), RequestOptions.DEFAULT);
		return cr.getCount();
	}
	
	@Override
	public List<UserAccount> getAllAccounts(final int page, final int pageSize) {
		
		final SearchRequest searchRequest = new SearchRequest(ElasticSettings.ACCOUNT_IDX); 
		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.from(page * pageSize);
		searchSourceBuilder.size(pageSize);
		searchRequest.source(searchSourceBuilder);
		
		List<UserAccount> accs = new ArrayList<>();
		
		try {
			SearchResponse resp = esClient.search(searchRequest, RequestOptions.DEFAULT);
			resp.getHits().forEach(h -> {
				accs.add( Account.buildAccountInfo(h.getSourceAsString()) );
			});
		} catch (IOException e) {
			logger.error("Getting accounts", e);
		}

		return accs;
	}



}
