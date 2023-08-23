package com.example.searchboard.service;

import com.example.searchboard.CheckEng;
import com.example.searchboard.EnglishToKoreanConverter;
import com.example.searchboard.domain.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.text.Normalizer;
import java.text.Normalizer.Form;

@Service
@Slf4j
public class SearchService {
    private final RestHighLevelClient client;

    @Autowired
    public SearchService(RestHighLevelClient client) {
        this.client = client;
    }

    public SearchMainDto mainList(String keyword, List<String> searchCategory, String sort, String startPeriod, String endPeriod, String preKeyword) throws IOException {
        log.debug("preKeyword:{}",preKeyword);
        String[] moisDomain = {"mois_photo", "mois_attach"};
        String[] yhnCategories = {"정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프", "연예", "스포츠", "오피니언", "사람들"};
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");
        SearchRequest searchRequest_yhn = new SearchRequest("yhn_index");
        SearchMainDto resultDto = new SearchMainDto();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();

        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod, endPeriod);
        setSizeQuery(sort, mainBuilder, null,5);
        String searchKeyword ="";
        if(!ObjectUtils.isEmpty(preKeyword)){
            searchKeyword = preKeyword;
        }else{
            searchKeyword = keyword;
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            log.debug("keywlrl:{}",searchKeyword);
            resultDto = getSearchResult(searchKeyword, resultDto, moisDomain, yhnCategories, searchCategory, dateRangeQuery, mainBuilder
                    , searchRequest_mois, searchRequest_yhn);

            if (CheckEng.isEnglishString(keyword) && resultDto.getYhnMainList().isEmpty() && resultDto.getMoisMainList().isEmpty()) {
                String engToKor = EnglishToKoreanConverter.engToKor(keyword);
                resultDto = getSearchResult(engToKor, resultDto, moisDomain, yhnCategories, searchCategory, dateRangeQuery, mainBuilder
                        , searchRequest_mois, searchRequest_yhn);
            }

            return resultDto;
        } else {
            try {

                //메인 페이지 전체 기사 불러오기
                List<SearchResponse> moisMain = new ArrayList<>();
                List<SearchResponse> yhnMain = new ArrayList<>();
                List<MoisDto> moisMainList = new ArrayList<>();

                for (String domain : moisDomain) {
                    BoolQueryBuilder moisDomainQuery = QueryBuilders.boolQuery().filter(new TermQueryBuilder("domain", domain));
                    BoolQueryBuilder finalQuery = QueryBuilders.boolQuery().filter(moisDomainQuery);

                    if (dateRangeQuery != null) { // dateRangeQuery가 null이 아닐 때만 추가
                        finalQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(finalQuery);
                    SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
                    moisMain.add(moisResponse);

                    long moisHitsCount = moisResponse.getHits().getTotalHits().value;

                }

                for (SearchResponse moisResponse : moisMain) {
                    moisMainList.addAll(moisList(moisResponse.getHits().getHits()));
                }

                for (String category : yhnCategories) {

                    BoolQueryBuilder yhnCategoryQuery = QueryBuilders.boolQuery().filter(new TermQueryBuilder("category_one_depth", category));
                    BoolQueryBuilder finalQuery = QueryBuilders.boolQuery().filter(yhnCategoryQuery);

                    if (dateRangeQuery != null) { // dateRangeQuery가 null이 아닐 때만 추가
                        finalQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(finalQuery);
                    SearchResponse yhnResponse = client.search(searchRequest_yhn.source(mainBuilder), RequestOptions.DEFAULT);
                    yhnMain.add(yhnResponse);

                    long HitsCount = yhnResponse.getHits().getTotalHits().value;
                }
                List<YhnDto> yhnMainList = new ArrayList<>();

                for (SearchResponse yhnResponse : yhnMain) {
                    yhnMainList.addAll(yhnList(yhnResponse.getHits().getHits()));
                }

                resultDto.setYhnMainList(yhnMainList);
                resultDto.setMoisMainList(moisMainList);

            } catch (IOException e) {
                log.debug("ElasticsearchError!!!");
            }

        }
        return resultDto;
    }

    private static RangeQueryBuilder getRangeQueryBuilder(String startPeriod, String endPeriod) {
        RangeQueryBuilder dateRangeQuery = null;
        if (startPeriod != null && !startPeriod.isEmpty() && endPeriod != null && !endPeriod.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.parse(startPeriod, formatter);
            LocalDate endDate = LocalDate.parse(endPeriod, formatter);
            dateRangeQuery = QueryBuilders
                    .rangeQuery("write_date")
                    .format("yyyy-MM-dd")
                    .gte(startDate)
                    .lte(endDate);
        }
        return dateRangeQuery;
    }

    //오타교정
    public String typoCorrect(String search) throws IOException {
        if (search.isEmpty() == true || search == null) {
            return null;
        }

        SuggestBuilder suggestBuilder = new SuggestBuilder().addSuggestion("mySuggestion", SuggestBuilders.termSuggestion("keyword.suggest").text(search));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().suggest(suggestBuilder);
        SearchRequest searchRequest = new SearchRequest("typocorrect-dict");
        searchRequest.source(sourceBuilder);
//        System.out.println(searchRequest);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Suggest suggest = searchResponse.getSuggest();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(suggest.toString());

        JsonNode mySuggestions = jsonNode.get("suggest").get("mySuggestion");
        JsonNode lastMySuggestion = mySuggestions.get(mySuggestions.size() - 1);
        JsonNode options;
        try {
            options = lastMySuggestion.get("options");
        } catch (NullPointerException e) {
            return null;
        }
//        System.out.println(options.size());
        if (options.size() == 0) {    // 오타제안이 없다면 (검색결과가 존재한다면) null Return
            return null;
        }
        String lastOptionText = options.get(0).get("text").asText();
        System.out.println("오타교정 : " + lastOptionText);
        String normalizedText = Normalizer.normalize(lastOptionText, Normalizer.Form.NFC);


        return normalizedText;      // 오타 교정 제안이 있다면 해당 String Return (option 마지막 인덱스의 Score가 가장 높은 String)
    }


    private SearchMainDto getSearchResult(String keyword, SearchMainDto resultDto, String[] moisDomain, String[] yhnCategories, List<String> searchCategory,
                                          RangeQueryBuilder dateRangeQuery,
                                          SearchSourceBuilder mainBuilder, SearchRequest searchRequest_mois, SearchRequest searchRequest_yhn) throws IOException {
        if (keyword != null && !keyword.isEmpty()) {

            List<MoisDto> moisMain = new ArrayList<>();
            if(!ObjectUtils.isEmpty(searchRequest_mois)) {
                for (String domain : moisDomain) {
                    // Reset boolQuery for each domain iteration
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    setKeywordquery(keyword, searchCategory, boolQuery);

                    // Create a filter for the given domain using match query
                    MatchQueryBuilder domainMatchQuery = QueryBuilders.matchQuery("domain", domain);
                    boolQuery.filter(domainMatchQuery);

                    // Apply date range query if applicable
                    if (dateRangeQuery != null) {
                        boolQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(boolQuery);
                    searchRequest_mois.source(mainBuilder);

                    SearchHit[] hits = client.search(searchRequest_mois, RequestOptions.DEFAULT).getHits().getHits();

                    moisMain.addAll(moisSearchList(hits, keyword));
                    resultDto.setMoisMainList(moisMain);
                }
            }


            List<YhnDto> YhnMain = new ArrayList<>();
            if(!ObjectUtils.isEmpty(searchRequest_yhn)) {

                for (String category : yhnCategories) {

                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

                    // Create a multi-match query for the given keyword and all searchFields
                    setKeywordquery(keyword, searchCategory, boolQuery);

                    MatchQueryBuilder categoryMatchQuery = QueryBuilders.matchQuery("category_one_depth", category);
                    boolQuery.filter(categoryMatchQuery);
                    if (dateRangeQuery != null) {
                        boolQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(boolQuery);

                    searchRequest_yhn.source(mainBuilder);

                    SearchHit[] hits = client.search(searchRequest_yhn, RequestOptions.DEFAULT).getHits().getHits();
                    YhnMain.addAll(yhnSearchList(hits, keyword));
                    resultDto.setYhnMainList(YhnMain);
                }
            }

        }
        return resultDto;
    }


    ////통합검색 메인 출력
//    private List<MoisDto> getMoisMainList(SearchRequest searchRequest_mois, SearchSourceBuilder mainBuilder, String[] moisDomain) throws IOException {
//        List<MoisDto> moisMainList = new ArrayList<>();
//        log.debug("main:{}",mainBuilder);
//
//        for (String domain : moisDomain) {
//            mainBuilder.query(QueryBuilders.matchQuery("domain", domain));
//            SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
//
//            log.debug("filecontet:{}", searchRequest_mois);
//            moisMainList.addAll(moisList(moisResponse.getHits().getHits()));
//        }
//
//        log.debug("filecontsssssset:{}", searchRequest_mois);
//
//        return moisMainList;
//    }
//    private List<YhnDto> getYhnMainList(SearchRequest searchRequest_yhn, SearchSourceBuilder mainBuilder, String[] yhnCategories) throws IOException {
//        List<YhnDto> yhnMainList = new ArrayList<>();
//
//        for (String category : yhnCategories) {
//            mainBuilder.query(QueryBuilders.matchQuery("category_one_depth", category));
//            SearchResponse yhnResponse = client.search(searchRequest_yhn.source(mainBuilder), RequestOptions.DEFAULT);
//            yhnMainList.addAll(yhnList(yhnResponse.getHits().getHits()));
//        }
//
//        return yhnMainList;
//    }
    // 각각 카테고리 리스트
    public MoisPagination moisPageList(String keyword, List<String> searchFields, String domain, int page, String sort, String startPeriod, String endPeriod) throws IOException {
        List<MoisDto> moisMain = new ArrayList<>();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
        SearchRequest searchRequest_mois = new SearchRequest("mois_index");
        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod,endPeriod);
        Pagination pagination = new Pagination(10000, page);

        setSizeQuery(sort, mainBuilder, pagination,10);


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        setKeywordquery(keyword, searchFields, boolQuery);
        // 키워드가 없을 경우 domain으로 필터링
        MatchQueryBuilder domainMatchQuery = QueryBuilders.matchQuery("domain", domain);
        boolQuery.filter(domainMatchQuery);


        // Apply date range query if applicable
        if (dateRangeQuery != null) {
            boolQuery.filter(dateRangeQuery);
        }
        mainBuilder.query(boolQuery);
        log.debug("match:{}", mainBuilder);
        SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
        long totalHits = moisResponse.getHits().getTotalHits().value;

        moisMain.addAll(moisSearchList(moisResponse.getHits().getHits(), keyword));
        MoisPagination moisPagination = new MoisPagination(moisMain, pagination, keyword);
        return moisPagination;
    }



    public YhnPagination yhnPageList(String keyword, List<String> searchFields, String category, int page, String sort, String startPeriod, String endPeriod) throws IOException {
        List<YhnDto> yhnMain = new ArrayList<>();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
        SearchRequest searchRequest_yhn = new SearchRequest("yhn_index");
        Pagination pagination = new Pagination(10000, page);
        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod,endPeriod);


        setSizeQuery(sort, mainBuilder, pagination,10);


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        setKeywordquery(keyword, searchFields, boolQuery);

        MatchQueryBuilder domainMatchQuery = QueryBuilders.matchQuery("category_one_depth", category);
        boolQuery.filter(domainMatchQuery);

        if (dateRangeQuery != null) {
            boolQuery.filter(dateRangeQuery);
        }
        mainBuilder.query(boolQuery);


        SearchResponse yhnResponse = client.search(searchRequest_yhn.source(mainBuilder), RequestOptions.DEFAULT);
        long totalHits = yhnResponse.getHits().getTotalHits().value;

        yhnMain.addAll(yhnSearchList(yhnResponse.getHits().getHits(), keyword));
        YhnPagination yhnPagination = new YhnPagination(yhnMain, pagination, keyword);
        return yhnPagination;
    }
    //검색 결과 메인
//    public List<MoisDto> searchKeywordMois(String keyword, String[] moisDomain, List<String> searchFields,SearchRequest searchRequest_mois, SearchSourceBuilder mainBuilder  ) throws IOException {
//        List<MoisDto> moisMain = new ArrayList<>();
//
//        for (String domain : moisDomain) {
//            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//
//            if (searchFields.contains("all")) {
//                // Create a multi-match query for the given keyword and all specified fields
//                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
//                boolQuery.must(multiMatchQuery);
//            } else {
//                // Create a multi-match query for the given keyword and specific fields
//                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
//                boolQuery.must(multiMatchQuery);
//            }
//
//            // Create a filter for the given domain
//            TermQueryBuilder termQuery = QueryBuilders.termQuery("domain", domain);
//            boolQuery.filter(termQuery);
//
//            mainBuilder.query(boolQuery);
//
//
//            searchRequest_mois.source(mainBuilder);
//
//            SearchHit[] hits = client.search(searchRequest_mois, RequestOptions.DEFAULT).getHits().getHits();
//
//            moisMain.addAll(moisSearchList(hits,keyword));
//        }
//
//        return moisMain;
//    }
//    public List<YhnDto> searchKeywordYhn(String keyword, String[] categories, List<String> searchFields, SearchRequest searchRequest_yhn, SearchSourceBuilder mainBuilder) throws IOException {
//        List<YhnDto> YhnMain = new ArrayList<>();
//
//        for (String category : categories) {
//            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//
//            // Create a multi-match query for the given keyword and all searchFields
//            if (searchFields.contains("all")) {
//                // Create a multi-match query for the given keyword and all specified fields
//                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
//                boolQuery.must(multiMatchQuery);
//            } else {
//                /* Create a multi-match query for the given keyword and specific fields */
//                MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
//                boolQuery.must(multiMatchQuery);
//            }
//
//            // Create a filter for the given domain
//            TermQueryBuilder termQuery = QueryBuilders.termQuery("category_one_depth", category);
//            boolQuery.filter(termQuery);
//
//            mainBuilder.query(boolQuery);
//            mainBuilder.trackTotalHits(true);
//
//
//            searchRequest_yhn.source(mainBuilder);
//
//            SearchHit[] hits = client.search(searchRequest_yhn, RequestOptions.DEFAULT).getHits().getHits();
//            YhnMain.addAll(yhnSearchList(hits,keyword));
//        }
//
//        return YhnMain;
//    }

    private static void setKeywordquery(String keyword, List<String> searchFields, BoolQueryBuilder boolQuery) {
        if (keyword != null && !keyword.isEmpty()) {

            String[] keywords = keyword.split(",");
            for(String key : keywords) {
                key = key.trim();
                MultiMatchQueryBuilder multiMatchQuery = getMultiMatchQueryBuilder(key, searchFields);
                boolQuery.must(multiMatchQuery);
            }
        }
    }

    private static MultiMatchQueryBuilder getMultiMatchQueryBuilder(String keyword, List<String> searchFields) {
        MultiMatchQueryBuilder multiMatchQuery;
        // 키워드가 있을 경우 키워드로 검색
        if (searchFields.contains("all")) {
            multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, "title", "content", "_file.content", "_file.nameOrg");
        } else {
            multiMatchQuery = QueryBuilders.multiMatchQuery(keyword, searchFields.toArray(new String[searchFields.size()]));
        }
        return multiMatchQuery;
    }

    private static void setSizeQuery(String sort, SearchSourceBuilder mainBuilder, Pagination pagination, int size) {
        int startIndex = 0;
        if(!ObjectUtils.isEmpty(pagination)) {
            startIndex = pagination.getStartIndex();
        }
        if (sort.equals("dateDesc")) {
            mainBuilder.sort(SortBuilders.fieldSort("write_date").order(SortOrder.DESC))
                    .from(startIndex)
                    .size(size)
                    .trackTotalHits(true); // 개수 표시 위함
        } else if (sort.equals("dateAsc")) {
            mainBuilder.sort(SortBuilders.fieldSort("write_date").order(SortOrder.ASC))
                    .from(startIndex)
                    .size(size)
                    .trackTotalHits(true); // 개수 표시 위함;
        } else if (sort.equals("accuracyDesc")) {
            mainBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC))
                    .from(startIndex)
                    .size(size)
                    .trackTotalHits(true);
        } else {
            mainBuilder.sort(SortBuilders.scoreSort().order(SortOrder.ASC))
                    .from(startIndex)
                    .size(10)
                    .trackTotalHits(true);
        }
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

    private List<YhnDto> yhnSearchList(SearchHit[] hits, String keyword) {
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
            result.setThumbnailImg((String) attachMap.get("thumbnail_url"));

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
            result.setThumbnailImg((String) attachMap.get("thumbnail_url"));
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

    public List<String> searchAutocomplete(String keyword) {
        try {
            // Create a match query for the "keyword" field
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("keyword", keyword);
            // Create a search source builder and set the size and query
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(matchQuery);
            sourceBuilder.size(5);

            // Create a search request
            SearchRequest searchRequest = new SearchRequest("autocomplete-dict");
            searchRequest.source(sourceBuilder);

            // Execute the search request
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            List<String> keywordList = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                String keywordValue = hit.getSourceAsMap().get("keyword").toString();
                keywordList.add(keywordValue);
            }
            log.debug("asdf:{}", keywordList);
            return keywordList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> searchForbiddenWords() {
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.matchAllQuery());

            SearchRequest searchRequest = new SearchRequest("forbiddenword-dict");
            searchRequest.source(sourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            List<String> forbiddenList = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                String keyword = (String) hit.getSourceAsMap().get("keyword");
                forbiddenList.add(keyword);
            }

            return forbiddenList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> searchStopWords() {
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.matchAllQuery());

            SearchRequest searchRequest = new SearchRequest("stopword-dict");
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            List<String> stopList = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                String forbiddenWord = (String) hit.getSourceAsMap().get("keyword");
                stopList.add(forbiddenWord);
            }

            return stopList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> recommendWord(String keyword) {
        try {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("recomm", keyword);

            // 검색 요청을 생성
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(matchQuery);

            SearchRequest searchRequest = new SearchRequest("recomm-manual-dict");
            searchRequest.source(sourceBuilder);

            // 쿼리를 실행
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<String> recommendList = new ArrayList<>();
            SearchHit[] hits = searchResponse.getHits().getHits();
            if (hits.length > 0) {
                String recommend = hits[0].getSourceAsMap().get("recomm").toString();

                recommend = recommend.substring(1, recommend.length() - 1); // 대괄호 제거
                String[] items = recommend.split(", "); // 쉼표와 공백을 기준으로 분리

                recommendList = Arrays.asList(items);
            } else {
                return null;
            }

            return recommendList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void saveLog(String keyword, String sort, List<String> categories,String preKeyword ){

    }


}
