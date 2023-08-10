package com.example.searchboard;

import com.example.searchboard.domain.MoisAttachDto;
import com.example.searchboard.domain.MoisDto;
import com.example.searchboard.domain.MoisPhotoDto;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SearchService {
    private final RestHighLevelClient client;
    @Autowired
    public SearchService(RestHighLevelClient client) {
        this.client = client;
    }

    public MoisDto mapSearchResults() {
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.query(QueryBuilders.matchQuery("domain", "mois_photo"));
        searchSourceBuilder.size(5);
        searchSourceBuilder.sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC));
        searchRequest_mois.source(searchSourceBuilder);

        SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder();
        searchSourceBuilder1.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder1.query(QueryBuilders.matchQuery("domain", "mois_attach"));
        searchSourceBuilder1.size(5);
        searchSourceBuilder1.sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC));
        searchRequest_mois.source(searchSourceBuilder1);
        try{
            SearchResponse searchResponse_photo = client.search(searchRequest_mois, RequestOptions.DEFAULT);
            SearchResponse searchResponse_attach = client.search(searchRequest_mois, RequestOptions.DEFAULT);
            SearchHit[] hits_photo = searchResponse_photo.getHits().getHits();
            SearchHit[] hits_attach = searchResponse_attach.getHits().getHits();
            List<MoisAttachDto> attachResults = new ArrayList<>();
            List<MoisPhotoDto> photoResults = new ArrayList<>();

            for (SearchHit hit : hits_attach) {
                Map<String, Object> attachMap = hit.getSourceAsMap();
                MoisAttachDto attachResult = new MoisAttachDto();
                attachResult.setTitle((String) attachMap.get("title"));
                attachResult.setThumbnailImg((String) attachMap.get("thumbnail_img"));
                attachResult.setContent((String) attachMap.get("content"));
                attachResult.setWriter((String) attachMap.get("writer"));
                attachResult.setWriteDate((String) attachMap.get("write_date"));
                attachResult.setDomain((String) attachMap.get("domain"));

                attachResults.add(attachResult);
            }
            for (SearchHit hit : hits_photo) {
                Map<String, Object> photoMap = hit.getSourceAsMap();
                MoisPhotoDto photoResult = new MoisPhotoDto();
                photoResult.setTitle((String) photoMap.get("title"));
                photoResult.setThumbnailImg((String) photoMap.get("thumbnail_img"));
                photoResult.setContent((String)photoMap.get("content"));
                photoResult.setWriter((String) photoMap.get("writer"));
                photoResult.setWriteDate((String) photoMap.get("write_date"));
                photoResult.setDomain((String) photoMap.get("domain"));

                photoResults.add(photoResult);
            }

            return new MoisDto(attachResults, photoResults);
        }catch(IOException e){
            log.debug("searchResponseError");

        }

        return null;

    }
}
