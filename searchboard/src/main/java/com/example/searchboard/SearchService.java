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

    public SearchMainDto mainList(String keyword,List<String> searchCategory) throws IOException {
        String[] moisDomain = {"mois_photo", "mois_attach"};
        String[] yhnCategories = {"정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프", "연예", "스포츠", "오피니언", "사람들"};
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");
        SearchRequest searchRequest_yhn = new SearchRequest("yhn_index");
        SearchMainDto resultDto = new SearchMainDto();
        SearchSourceBuilder commonSearchSourceBuilder = new SearchSourceBuilder();
        commonSearchSourceBuilder.query(QueryBuilders.matchAllQuery());
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder()
                .sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC))
                .size(5)
                .trackTotalHits(true); // 개수 표시 위함

        if(keyword != null && !keyword.isEmpty()) {
            //키워드가 있을경우 키워드로 검색
            resultDto.setMoisMainList(searchKeywordMois(keyword, moisDomain, searchCategory,searchRequest_mois, mainBuilder));
            resultDto.setYhnMainList(searchKeywordYhn(keyword, yhnCategories, searchCategory,searchRequest_yhn, mainBuilder));

        }else{

            List<MoisDto> moisMainList = getMoisMainList(searchRequest_mois, mainBuilder, moisDomain);
            List<YhnDto> yhnMainList = getYhnMainList(searchRequest_yhn, mainBuilder, yhnCategories);

            resultDto.setYhnMainList(yhnMainList);
            resultDto.setMoisMainList(moisMainList);
        }
        return resultDto;
    }

    // 나머지 코드와 mainList 메서드 구현
    ////통합검색 메인 출력
    private List<MoisDto> getMoisMainList(SearchRequest searchRequest_mois, SearchSourceBuilder mainBuilder, String[] moisDomain) throws IOException {
        List<MoisDto> moisMainList = new ArrayList<>();

        for (String domain : moisDomain) {
            mainBuilder.query(QueryBuilders.matchQuery("domain", domain));
            SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
            moisMainList.addAll(moisList(moisResponse.getHits().getHits()));
        }
        return moisMainList;
    }
    private List<YhnDto> getYhnMainList(SearchRequest searchRequest_yhn, SearchSourceBuilder mainBuilder, String[] yhnCategories) throws IOException {
        List<YhnDto> yhnMainList = new ArrayList<>();

        for (String category : yhnCategories) {
            mainBuilder.query(QueryBuilders.matchQuery("category_one_depth", category));
            SearchResponse yhnResponse = client.search(searchRequest_yhn.source(mainBuilder), RequestOptions.DEFAULT);
            yhnMainList.addAll(yhnList(yhnResponse.getHits().getHits()));
        }

        return yhnMainList;
    }
    // 각각 카테고리 리스트
    public MoisPagination moisPageList(String keyword, List<String> searchFields, String domain, int page , String sort) throws IOException {
        List<MoisDto> moisMain = new ArrayList<>();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");
        Pagination pagination = new Pagination(10000, page);
        if(sort.equals("dateDesc")){

            mainBuilder.sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC))
                    .from(pagination.getStartIndex())
                    .size(10)
                    .trackTotalHits(true); // 개수 표시 위함
        }else if(sort.equals("dateAsc")){

            mainBuilder.sort(SortBuilders.fieldSort("write_date").order(SortOrder.ASC))
                    .from(pagination.getStartIndex())
                    .size(10)
                    .trackTotalHits(true); // 개수 표시 위함;
        }else if(sort.equals("accuracyDesc")){
            mainBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC))
                    .from(pagination.getStartIndex())
                    .size(10)
                    .trackTotalHits(true);
        }else{
            mainBuilder.sort(SortBuilders.scoreSort().order(SortOrder.ASC))
                    .from(pagination.getStartIndex())
                    .size(10)
                    .trackTotalHits(true);
        }


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (keyword != null && !keyword.isEmpty()) {

            // 키워드가 있을 경우 키워드로 검색
            if (searchFields.contains("all")) {
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
                boolQuery.must(multiMatchQuery);
            } else {
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
                boolQuery.must(multiMatchQuery);
            }
        } else {
            // 키워드가 없을 경우 domain으로 필터링
            TermQueryBuilder termQuery = QueryBuilders.termQuery("domain", domain);
            boolQuery.filter(termQuery);
        }

        mainBuilder.query(boolQuery);

        SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
        long totalHits = moisResponse.getHits().getTotalHits().value;

        moisMain.addAll(moisSearchList(moisResponse.getHits().getHits(),keyword));
        MoisPagination moisPagination = new MoisPagination(moisMain, pagination, keyword);
        return moisPagination;
    }
    public YhnPagination yhnPageList(String keyword, List<String> searchFields, String category, int page ) throws IOException {
        List<YhnDto> yhnMain = new ArrayList<>();
        SearchRequest searchRequest_mois = new SearchRequest("yhn_index");
        Pagination pagination = new Pagination(10000, page);
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder()
                .sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC))
                .from(pagination.getStartIndex())
                .size(10)
                .trackTotalHits(true); // 개수 표시 위함

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (keyword != null && !keyword.isEmpty()) {
            // 키워드가 있을 경우 키워드로 검색
            if (searchFields.contains("all")) {
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
                boolQuery.must(multiMatchQuery);
            } else {
                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
                boolQuery.must(multiMatchQuery);

            }
        }

        TermQueryBuilder termQuery = QueryBuilders.termQuery("category_one_depth", category);
        boolQuery.filter(termQuery);


        mainBuilder.query(boolQuery);

        SearchResponse yhnResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
        long totalHits = yhnResponse.getHits().getTotalHits().value;

        yhnMain.addAll(yhnSearchList(yhnResponse.getHits().getHits(),keyword));
        YhnPagination yhnPagination = new YhnPagination(yhnMain, pagination, keyword);
        return yhnPagination;
    }
    //검색 결과 메인
    public List<MoisDto> searchKeywordMois(String keyword, String[] moisDomain, List<String> searchFields,SearchRequest searchRequest_mois, SearchSourceBuilder mainBuilder  ) throws IOException {
        List<MoisDto> moisMain = new ArrayList<>();

        for (String domain : moisDomain) {
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


            searchRequest_mois.source(mainBuilder);

            SearchHit[] hits = client.search(searchRequest_mois, RequestOptions.DEFAULT).getHits().getHits();

            moisMain.addAll(moisSearchList(hits,keyword));
        }

        return moisMain;
    }
    public List<YhnDto> searchKeywordYhn(String keyword, String[] categories, List<String> searchFields, SearchRequest searchRequest_yhn, SearchSourceBuilder mainBuilder) throws IOException {
        List<YhnDto> YhnMain = new ArrayList<>();

        for (String category : categories) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Create a multi-match query for the given keyword and all searchFields
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
            TermQueryBuilder termQuery = QueryBuilders.termQuery("category_one_depth", category);
            boolQuery.filter(termQuery);

            mainBuilder.query(boolQuery);
            mainBuilder.trackTotalHits(true);


            searchRequest_yhn.source(mainBuilder);

            SearchHit[] hits = client.search(searchRequest_yhn, RequestOptions.DEFAULT).getHits().getHits();
            YhnMain.addAll(yhnSearchList(hits,keyword));
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

            // <b> 태그를 포함한 필드 설정
//            String contentWithBold = addBoldTags((String) attachMap.get("content"), keyword);
//            result.setContent(contentWithBold);

            results.add(result);
        }

        return results;
    }
    private List<MoisDto> moisSearchList(SearchHit[] hits, String keyword) {
        List<MoisDto> results = new ArrayList<>();

        for (SearchHit hit : hits) {
            Map<String, Object> attachMap = hit.getSourceAsMap();
            MoisDto result = new MoisDto();
//            result.setTitle((String) attachMap.get("title"));
            result.setThumbnailImg((String) attachMap.get("thumbnail_img"));
//            result.setContent((String) attachMap.get("content"));
            result.setWriter((String) attachMap.get("writer"));
            result.setWriteDate((String) attachMap.get("write_date"));
            result.setDomain((String) attachMap.get("domain"));
            result.setUrl((String) attachMap.get("url"));
            result.setFile_content((String) attachMap.get("_file.content"));
            result.setFile_name((String) attachMap.get("_file.nameOrg"));

            // <b> 태그를 포함한 필드 설정
            String contentWithBold = addBoldTags((String) attachMap.get("content"), keyword);
            result.setContent(contentWithBold);

            String titleWithBold = addBoldTags((String) attachMap.get("title"), keyword);
            result.setTitle(titleWithBold);

            results.add(result);
        }

        return results;
    }
    private List<YhnDto> yhnSearchList(SearchHit[] hits,String keyword) {
        List<YhnDto> results = new ArrayList<>();

        for (SearchHit hit : hits) {
            Map<String, Object> attachMap = hit.getSourceAsMap();
            YhnDto result = new YhnDto();
//            result.setTitle((String) attachMap.get("title"));
            result.setThumbnailImg((String) attachMap.get("thumbnail_img"));
//            result.setContent((String) attachMap.get("content"));
            result.setWriter((String) attachMap.get("writer"));
            result.setWriteDate((String) attachMap.get("write_date"));
            result.setDomain((String) attachMap.get("domain"));
            result.setCategory_one_depth((String) attachMap.get("category_one_depth"));
            result.setUrl((String) attachMap.get("url"));
            result.setThumbnailImg((String)attachMap.get("thumbnail_url"));

            // <b> 태그를 포함한 필드 설정
            String contentWithBold = addBoldTags((String) attachMap.get("content"), keyword);
            result.setContent(contentWithBold);

            String titleWithBold = addBoldTags((String) attachMap.get("title"), keyword);
            result.setTitle(titleWithBold);
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
    private String addBoldTags(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return text;
        }

        // 키워드를 <b> 태그로 감싸기
        String lowerCaseText = text.toLowerCase();
        String lowerCaseKeyword = keyword.toLowerCase();
        int index = lowerCaseText.indexOf(lowerCaseKeyword);
        if (index != -1) {
            String beforeKeyword = text.substring(0, index);
            String afterKeyword = text.substring(index + keyword.length());
            return beforeKeyword + "<b style='color:red'>" + text.substring(index, index + keyword.length()) + "</b>" + afterKeyword;
        }

        return text;
    }




}
