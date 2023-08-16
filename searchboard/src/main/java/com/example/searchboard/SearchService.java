package com.example.searchboard;

import com.example.searchboard.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.elasticsearch.common.text.Text;

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

    public SearchMainDto mainList(String keyword,List<String> searchCategory) throws IOException {
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");
        SearchRequest searchRequest_yhn = new SearchRequest("yhn_index");
        String[] moisDomain = {"mois_photo", "mois_attach"};
        String[] yhnCategories = {"정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프", "연예", "스포츠", "오피니언", "사람들"};
        SearchMainDto resultDto = new SearchMainDto();
        SearchSourceBuilder commonSearchSourceBuilder = new SearchSourceBuilder();
        commonSearchSourceBuilder.query(QueryBuilders.matchAllQuery());
        if(keyword != null && !keyword.isEmpty()){
            //키워드가 있을경우 키워드로 검색
            resultDto.setMoisMainList(searchKeywordMois(keyword, moisDomain, searchCategory));
            resultDto.setYhnMainList(searchKeywordYhn(keyword, yhnCategories, searchCategory));
            return resultDto;

        }else{
            //키워드가 없을 경우
            //메인페이지는 최신순 5개씩 불러온다
            SearchSourceBuilder mainBuilder = new SearchSourceBuilder()
                    .sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC))
                    .trackTotalHits(true); // 개수 표시 위함

            try {

                //메인 페이지 전체 기사 불러오기
                List<SearchResponse> moisMain = new ArrayList<>();
                List<SearchResponse> yhnMain = new ArrayList<>();
                List<MoisDto> moisMainList = new ArrayList<>();

                for (String domain : moisDomain) {
                    mainBuilder.query(QueryBuilders.matchQuery("domain", domain));
                    SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
                    moisMain.add(moisResponse);

                    long moisHitsCount = moisResponse.getHits().getTotalHits().value;
                    resultDto.setDomainMoisHitsCount(domain, moisHitsCount);
                }

                for (SearchResponse moisResponse : moisMain) {
                    moisMainList.addAll(moisList(moisResponse.getHits().getHits()));
                }

                for (String category : yhnCategories) {
                    mainBuilder.query(QueryBuilders.matchQuery("category_one_depth", category));
                    SearchResponse yhnResponse = client.search(searchRequest_yhn.source(mainBuilder), RequestOptions.DEFAULT);
                    yhnMain.add(yhnResponse);
                    long moisHitsCount = yhnResponse.getHits().getTotalHits().value;
                    resultDto.setDomainYhnHitsCount(category, moisHitsCount);
                }
                List<YhnDto> yhnMainList = new ArrayList<>();

                for (SearchResponse yhnResponse : yhnMain) {
                    yhnMainList.addAll(yhnList(yhnResponse.getHits().getHits()));
                }

                resultDto.setYhnMainList(yhnMainList);
                resultDto.setMoisMainList(moisMainList);

                return resultDto;
            } catch (IOException e) {
                log.debug("ElasticsearchError!!!");
            }

        }


        return null;
    }

    public List<MoisDto> searchKeywordMois(String keyword, String[] moisDomain, List<String> searchFields) throws IOException {
        List<MoisDto> moisMain = new ArrayList<>();

        for (String domain : moisDomain) {
            SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            if (searchFields.contains("all")) {
                // Create a multi-match query for the given keyword and all specified fields
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
                boolQuery.must(multiMatchQuery);
            } else {
                // Create a multi-match query for the given keyword and specific fields
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
                boolQuery.must(multiMatchQuery);
            }

            // Create a filter for the given domain
            TermQueryBuilder termQuery = QueryBuilders.termQuery("domain", domain);
            boolQuery.filter(termQuery);

            mainBuilder.query(boolQuery);

            SearchRequest searchRequest = new SearchRequest("mois_index");
            searchRequest.source(mainBuilder);

            SearchHit[] hits = client.search(searchRequest, RequestOptions.DEFAULT).getHits().getHits();

            moisMain.addAll(moisList(hits));
        }

        return moisMain;
    }
    public List<YhnDto> searchKeywordYhn(String keyword, String[] categories, List<String> searchFields) throws IOException {
        List<YhnDto> YhnMain = new ArrayList<>();
        log.debug("searchfields:{}", searchFields);

        for (String category : categories) {
            SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Create a multi-match query for the given keyword and all searchFields
            if (searchFields.contains("all")) {
                // Create a multi-match query for the given keyword and all specified fields
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
                boolQuery.must(multiMatchQuery);
            } else {
                // Create a multi-match query for the given keyword and specific fields
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
                log.debug("size:{}", searchFields.size());
                log.debug("fileds:{}", searchFields.toArray(new String[searchFields.size()]));
                boolQuery.must(multiMatchQuery);
            }

            // Create a filter for the given domain
            TermQueryBuilder termQuery = QueryBuilders.termQuery("category_one_depth", category);
            boolQuery.filter(termQuery);

            mainBuilder.query(boolQuery);
            SearchRequest searchRequest = new SearchRequest("yhn_index");
            searchRequest.source(mainBuilder);

            SearchHit[] hits = client.search(searchRequest, RequestOptions.DEFAULT).getHits().getHits();
            YhnMain.addAll(yhnList(hits));
        }

        return YhnMain;
    }



    private List<MoisDto> moisList(SearchHit[] hits) {
        List<MoisDto> results = new ArrayList<>();

        for (SearchHit hit : hits) {
            Map<String, Object> attachMap = hit.getSourceAsMap();
            MoisDto result = new MoisDto();
            result.setTitle((String) attachMap.get("title"));
            result.setThumbnailImg((String) attachMap.get("thumbnail_img"));
            result.setContent((String) attachMap.get("content"));
            result.setWriter((String) attachMap.get("writer"));
            result.setWriteDate((String) attachMap.get("write_date"));
            result.setDomain((String) attachMap.get("domain"));
            result.setUrl((String) attachMap.get("url"));
            result.setFile_content((String) attachMap.get("_file.content"));
            result.setFile_name((String) attachMap.get("_file.nameOrg"));

            results.add(result);
        }

        return results;
    }
    private List<YhnDto> yhnList(SearchHit[] hits) {
        List<YhnDto> results = new ArrayList<>();

        for (SearchHit hit : hits) {
            Map<String, Object> attachMap = hit.getSourceAsMap();
            YhnDto result = new YhnDto();
            result.setTitle((String) attachMap.get("title"));
            result.setThumbnailImg((String) attachMap.get("thumbnail_img"));
            result.setContent((String) attachMap.get("content"));
            result.setWriter((String) attachMap.get("writer"));
            result.setWriteDate((String) attachMap.get("write_date"));
            result.setDomain((String) attachMap.get("domain"));
            result.setCategory_one_depth((String) attachMap.get("category_one_depth"));
            result.setUrl((String) attachMap.get("url"));
            result.setThumbnailImg((String)attachMap.get("thumbnail_url"));
            results.add(result);
        }

        return results;
    }


}
