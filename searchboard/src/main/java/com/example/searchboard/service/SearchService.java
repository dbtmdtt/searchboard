package com.example.searchboard.service;

import com.example.searchboard.CheckEng;
import com.example.searchboard.EnglishToKoreanConverter;
import com.example.searchboard.domain.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.Normalizer;

@Service
@Slf4j
public class SearchService {
    private final RestHighLevelClient client;

    @Autowired
    public SearchService(RestHighLevelClient client) {
        this.client = client;
    }

    SearchRequest searchRequest_mois = new SearchRequest("mois_index");
    SearchRequest searchRequest_yhn = new SearchRequest("yhn_index");
    SearchSourceBuilder mainBuilder = new SearchSourceBuilder();

    public SearchMainDto mainList(String keyword, List<String> searchField, String sort, String startPeriod, String endPeriod, String reKeyword, String user,String must, String must_not, String match_phrase, String categoryMenu) throws IOException {
        String[] moisDomain = {"mois_photo", "mois_attach"};
        String[] yhnCategories = {"정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프", "연예", "스포츠", "오피니언", "사람들"};
        SearchMainDto resultDto = new SearchMainDto();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
        if(StringUtils.hasText(must) || StringUtils.hasText(must_not) || StringUtils.hasText(match_phrase)){
            parsedSearch(must,must_not,match_phrase, searchField, categoryMenu, startPeriod, endPeriod, sort);
        }

        if (keyword != null) {
            saveLog(keyword, sort, searchField, reKeyword, user);
        }

        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod, endPeriod);

        if(sort != null){
            setSortQuery(sort, mainBuilder, null, 5);
        }


        String searchKeyword = "";
        if (!ObjectUtils.isEmpty(reKeyword)) {
            searchKeyword = reKeyword;
        } else {
            searchKeyword = keyword;
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            resultDto = getSearchResult(searchKeyword, resultDto, moisDomain, yhnCategories, searchField, dateRangeQuery, mainBuilder);

            if (CheckEng.isEnglishString(keyword) && resultDto.getYhnMainList().isEmpty() && resultDto.getMoisMainList().isEmpty()) {
                String engToKor = EnglishToKoreanConverter.engToKor(keyword);
                resultDto = getSearchResult(engToKor, resultDto, moisDomain, yhnCategories, searchField, dateRangeQuery, mainBuilder);
            }
            return resultDto;
        } else {
            try {

                //메인 페이지 전체 기사 불러오기
                List<SearchResponse> moisMain = new ArrayList<>();
                List<SearchResponse> yhnMain = new ArrayList<>();
                List<ListDto> yhnMainList = new ArrayList<>();
                List<ListDto> moisMainList = new ArrayList<>();


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
                    moisMainList.addAll(totalList(moisResponse.getHits().getHits()));
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

                for (SearchResponse yhnResponse : yhnMain) {
                    yhnMainList.addAll(totalList(yhnResponse.getHits().getHits()));
                }

                resultDto.setYhnMainList(yhnMainList);
                resultDto.setMoisMainList(moisMainList);

            } catch (IOException e) {
                log.debug("ElasticsearchError!!!");
            }

        }
        return resultDto;
    }
    private SearchMainDto parsedSearch(String must, String must_not, String match_phrase, List<String> searchField, String categoryMenu,String startPeriod,String endPeriod, String sort) throws IOException{
        SearchMainDto searchMainDto = new SearchMainDto();

        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod, endPeriod);
        if (sort != null) {
            setSortQuery(sort, mainBuilder, null, 5);
        }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StringUtils.hasText(must)) {
            setKeyword(must, searchField, boolQuery);
        }

        if (StringUtils.hasText(match_phrase)) {
            String[] keywords = match_phrase.split(",");
            for (String key : keywords) {
                key = key.trim();
                for (String field : searchField) {
                    MatchPhraseQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery(field, key);
                    boolQuery.should(matchPhraseQuery);
                }
            }

        }

        if (StringUtils.hasText(must_not)) {
            setNotKeyword(must_not, searchField, boolQuery);
        }

        SearchHit[] hits;

        if (categoryMenu.equals("mois_attach") || categoryMenu.equals("mois_photo")) {
            TermQueryBuilder categoryQuery = QueryBuilders.termQuery("domain", categoryMenu);
            boolQuery.filter(categoryQuery);
            hits = client.search(searchRequest_mois.source(new SearchSourceBuilder().query(boolQuery)), RequestOptions.DEFAULT).getHits().getHits();
            System.out.println("searchRequest_mois = " + searchRequest_mois);
        } else {
            TermQueryBuilder categoryQuery = QueryBuilders.termQuery("category_one_depth", categoryMenu);
            boolQuery.filter(categoryQuery);
            hits = client.search(searchRequest_yhn.source(new SearchSourceBuilder().query(boolQuery)), RequestOptions.DEFAULT).getHits().getHits();
            System.out.println("searchRequest_yhn = " + searchRequest_yhn);
        }

        // 여기서 Elasticsearch에 쿼리를 실행하고 결과를 얻어온 후, SearchMainDto 객체를 채워 반환한다.
        List<ListDto> moisMainList = new ArrayList<>();
        List<ListDto> yhnMainList = new ArrayList<>();
        List<ListDto> mainList = new ArrayList<>();
        Pagination pagination = new Pagination();  // 예시에서는 Pagination을 생성하는 코드가 없어 가정함

        for (SearchHit hit : hits) {
            // hit에서 필요한 정보를 추출하여 ListDto로 변환하고 해당 리스트에 추가하는 코드를 작성해야 함
        }

        searchMainDto.setMoisMainList(moisMainList);
        searchMainDto.setYhnMainList(yhnMainList);
        searchMainDto.setMainList(mainList);
        searchMainDto.setPagination(pagination);

        return searchMainDto;

    }


    private SearchMainDto getSearchResult(String keyword, SearchMainDto resultDto, String[] moisDomain, String[] yhnCategories, List<String> searchCategory, RangeQueryBuilder dateRangeQuery,SearchSourceBuilder mainBuilder) throws IOException {
        if (StringUtils.hasText(keyword)) {
            List<ListDto> moisMain = new ArrayList<>();
            if(!ObjectUtils.isEmpty(searchRequest_mois)) {
                for (String domain : moisDomain) {
                    // Reset boolQuery for each domain iteration
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    setKeyword(keyword, searchCategory, boolQuery);

                    // Create a filter for the given domain using match query
                    MatchQueryBuilder domainMatchQuery = QueryBuilders.matchQuery("domain", domain);
                    boolQuery.filter(domainMatchQuery);

                    // Apply date range query if applicable
                    if (dateRangeQuery != null) {
                        boolQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(boolQuery);
                    mainBuilder.highlighter(createHighlightBuilder());
                    searchRequest_mois.source(mainBuilder);

                    SearchHit[] hits = client.search(searchRequest_mois, RequestOptions.DEFAULT).getHits().getHits();

                    moisMain.addAll(totalList(hits));
                    resultDto.setMoisMainList(moisMain);
                }
            }


            List<ListDto> YhnMain = new ArrayList<>();
            if(!ObjectUtils.isEmpty(searchRequest_yhn)) {

                for (String category : yhnCategories) {

                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

                    // Create a multi-match query for the given keyword and all searchFields
                    setKeyword(keyword, searchCategory, boolQuery);

                    MatchQueryBuilder categoryMatchQuery = QueryBuilders.matchQuery("category_one_depth", category);
                    boolQuery.filter(categoryMatchQuery);
                    if (dateRangeQuery != null) {
                        boolQuery.filter(dateRangeQuery);
                    }

                    mainBuilder.query(boolQuery);
                    mainBuilder.highlighter(createHighlightBuilder());
                    searchRequest_yhn.source(mainBuilder);

                    SearchHit[] hits = client.search(searchRequest_yhn, RequestOptions.DEFAULT).getHits().getHits();
                    YhnMain.addAll(totalList(hits));
                    resultDto.setYhnMainList(YhnMain);
                }
            }

        }
        return resultDto;
    }

    public SearchMainDto totalPageList(String keyword, List<String> searchFields, String domain, int page, String sort, String startPeriod, String endPeriod, String reKeyword, String must, String must_not, String match_phrase, String category) throws IOException {
        String searchKeyword = "";
        if (!ObjectUtils.isEmpty(reKeyword)) {
            searchKeyword = reKeyword;
        } else {
            searchKeyword = keyword;
        }
        if(StringUtils.hasText(must) || StringUtils.hasText(must_not) || StringUtils.hasText(match_phrase)){
            parsedSearch(must,must_not,match_phrase, searchFields, domain, startPeriod, endPeriod, sort);
        }
        List<ListDto> moisMain = new ArrayList<>();
        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod, endPeriod);
        SearchMainDto resultDto;
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        setKeyword(searchKeyword, searchFields, boolQuery);
        if (dateRangeQuery != null) {
            boolQuery.filter(dateRangeQuery);
        }
        TermQueryBuilder domainMatchQuery;
        SearchResponse moisResponse;
        if (domain.equals("mois_attach") || domain.equals("mois_photo")) {            // 키워드가 없을 경우 domain으로 필터링
            domainMatchQuery = QueryBuilders.termQuery("domain", domain);
        } else {
            domainMatchQuery = QueryBuilders.termQuery("category_one_depth", domain);
        }
        boolQuery.filter(domainMatchQuery);
        mainBuilder.query(boolQuery);
        mainBuilder.highlighter(createHighlightBuilder());
        SearchRequest searchRequest = domain.equals("mois_attach") || domain.equals("mois_photo")
                ? searchRequest_mois.source(mainBuilder)
                : searchRequest_yhn.source(mainBuilder);
        moisResponse = client.search(searchRequest.source(mainBuilder), RequestOptions.DEFAULT);


        long totalHits = moisResponse.getHits().getTotalHits().value;
        moisPage(totalHits, searchKeyword, searchFields, domain, page, sort, startPeriod, endPeriod, must, must_not,match_phrase);

        moisMain.addAll(totalList(moisResponse.getHits().getHits()));
        resultDto = moisPage(totalHits, searchKeyword, searchFields, domain, page, sort, startPeriod, endPeriod, must,  must_not, match_phrase);
        return resultDto;
    }



    public SearchMainDto moisPage(long total, String keyword, List<String> searchFields, String domain, int page, String sort, String startPeriod, String endPeriod, String must, String must_not, String match_phrase) throws IOException {
        List<ListDto> moisMain = new ArrayList<>();
        log.debug("asdf2");
        Pagination pagination = new Pagination((int) total, page);
        SearchMainDto resultDto = new SearchMainDto();
        SearchSourceBuilder mainBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        RangeQueryBuilder dateRangeQuery = getRangeQueryBuilder(startPeriod, endPeriod);
        setKeyword(keyword, searchFields, boolQuery);

        setSortQuery(sort, mainBuilder, pagination, 10);

        if (dateRangeQuery != null) {
            boolQuery.filter(dateRangeQuery);
        }
        TermQueryBuilder domainMatchQuery;
        if (domain.equals("mois_attach") || domain.equals("mois_photo")) {            // 키워드가 없을 경우 domain으로 필터링
            domainMatchQuery = QueryBuilders.termQuery("domain", domain);
        } else {
           domainMatchQuery = QueryBuilders.termQuery("category_one_depth", domain);
        }
        boolQuery.filter(domainMatchQuery);



        mainBuilder.query(boolQuery);
        mainBuilder.highlighter(createHighlightBuilder());
        SearchRequest searchRequest = domain.equals("mois_attach") || domain.equals("mois_photo")
                ? searchRequest_mois.source(mainBuilder)
                : searchRequest_yhn.source(mainBuilder);

        SearchResponse moisResponse = client.search(searchRequest.source(mainBuilder), RequestOptions.DEFAULT);
        moisMain.addAll(totalList(moisResponse.getHits().getHits()));
        resultDto.setMainList(moisMain);
        resultDto.setPagination(pagination);
        return resultDto;
    }




    private List<ListDto> totalList(SearchHit[] hits) {
        List<ListDto> results = new ArrayList<>();

        for (SearchHit hit : hits) {
            Map<String, Object> attachMap = hit.getSourceAsMap();
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            ListDto result = new ListDto();

            if (highlightFields.containsKey("title")) {
                HighlightField titleHighlight = highlightFields.get("title");
                result.setTitle(String.valueOf(titleHighlight.fragments()[0].string()));
            } else {
                result.setTitle((String) attachMap.get("title"));
            }

            if (highlightFields.containsKey("content")) {
                HighlightField contentHighlight = highlightFields.get("content");
                result.setContent(String.valueOf(contentHighlight.fragments()[0].string()));
            } else {
                result.setContent((String) attachMap.get("content"));
            }

            if (highlightFields.containsKey("_file.content")) {
                HighlightField fileContentHighlight = highlightFields.get("_file.content");
                result.setFile_content(String.valueOf(fileContentHighlight.fragments()[0].string()));
            } else {
                result.setFile_content((String) attachMap.get("_file.content"));
            }

            if (highlightFields.containsKey("_file.nameOrg")) {
                HighlightField fileNameOrgHighlight = highlightFields.get("_file.nameOrg");
                result.setFile_name(String.valueOf(fileNameOrgHighlight.fragments()[0].string()));
            } else {
                result.setFile_name((String) attachMap.get("_file.nameOrg"));
            }


            result.setThumbnailImg((String) attachMap.get("thumbnail_img"));
            result.setWriter((String) attachMap.get("writer"));
            result.setWriteDate((String) attachMap.get("write_date"));
            result.setDomain((String) attachMap.get("domain"));
            result.setUrl((String) attachMap.get("url"));
            result.setCategory_one_depth((String) attachMap.get("category_one_depth"));
            result.setThumbnailImg((String) attachMap.get("thumbnail_url"));

            results.add(result);
        }

        return results;
    }


    private static void setKeyword(String keyword, List<String> searchFields, BoolQueryBuilder boolQuery) {
        if (keyword != null && !keyword.isEmpty()) {

            String[] keywords = keyword.split(",");
            for (String key : keywords) {
                key = key.trim();
                MultiMatchQueryBuilder multiMatchQuery = getMultiMatchQueryBuilder(key, searchFields);
                boolQuery.must(multiMatchQuery);
            }
        }
    }
    private static void setNotKeyword(String keyword, List<String> searchFields, BoolQueryBuilder boolQuery) {
        if (keyword != null && !keyword.isEmpty()) {

            String[] keywords = keyword.split(",");
            for (String key : keywords) {
                key = key.trim();
                MultiMatchQueryBuilder multiMatchQuery = getMultiMatchQueryBuilder(key, searchFields);
                boolQuery.mustNot(multiMatchQuery);
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

    private static RangeQueryBuilder getRangeQueryBuilder(String startPeriod, String endPeriod) {
        RangeQueryBuilder dateRangeQuery = null;
//        StringUtils.hasText();
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

    private static void setSortQuery(String sort, SearchSourceBuilder mainBuilder, Pagination pagination, int size) {
        int startIndex = 0;
        log.debug("asdf3");
        if (!ObjectUtils.isEmpty(pagination)) {
            startIndex = pagination.getStartIndex();
        }
        if (sort.equals("dateDesc")) {
            log.debug("asdf4");
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
                    .size(size)
                    .trackTotalHits(true);
        }
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

    private HighlightBuilder createHighlightBuilder() {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'><b>");
        highlightBuilder.postTags("</b></span>");
        highlightBuilder.field("title").highlighterType("plain");
        highlightBuilder.field("content").highlighterType("plain");
        highlightBuilder.field("_file.content").highlighterType("plain");
        highlightBuilder.field("_file.nameOrg").highlighterType("plain");
        highlightBuilder.fragmentSize(100);

        return highlightBuilder;
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
            keywordList.sort(Comparator.comparingInt(String::length));
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

    public void saveLog(String keyword, String sort, List<String> categories, String preKeyword, String user) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String createdDate = now.format(formatter);
        boolean re = false;
        if (preKeyword != null) {
            re = true;
        }
        String domain = "0번사전";
        String ip = "127.0.0.1";
        // IP 주소 취득
        try {
            // 현재 호스트의 InetAddress 객체 얻기
            InetAddress localHost = InetAddress.getLocalHost();

            // IP 주소 얻기
            ip = localHost.getHostAddress();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {

            Map<String, Object> tempMap = new HashMap<>();
            tempMap.put("asc", true);
            tempMap.put("category", categories);
            tempMap.put("createdDate", createdDate);
            tempMap.put("domain", domain);
            tempMap.put("ip", ip);
            tempMap.put("oquery", preKeyword); // 전 검색어
            tempMap.put("query", keyword); // 현 검색어
            tempMap.put("re", re);
            tempMap.put("referer", "none"); // 전 url
            tempMap.put("sort", sort);
            tempMap.put("user", user);
            System.out.println("tempMap = " + tempMap);
            // IndexRequest 객체 생성 및 JSON 문서 추가
            IndexRequest indexRequest = new IndexRequest("search-log") // 인덱스 이름
                    .source(tempMap);

            // IndexRequest를 Elasticsearch에 전송하여 데이터를 인덱스에 추가
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            log.debug("indexResponse");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<String> topWord() throws IOException {

        SearchRequest searchRequest = new SearchRequest("search-log");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();


        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("createdDate")
                .gte("now-7d/d");

        ExistsQueryBuilder existsQuery = QueryBuilders.existsQuery("query");

        boolQuery.filter(rangeQuery);
        boolQuery.filter(existsQuery);

        sourceBuilder.query(boolQuery);

        TermsAggregationBuilder termsAggregation = AggregationBuilders.terms("top_queries")
                .field("query")
                .size(11);


        sourceBuilder.aggregation(termsAggregation);

        sourceBuilder.size(0);

        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        List<String> keyList = new ArrayList<>();
        Terms topQueries = searchResponse.getAggregations().get("top_queries");
        for (Terms.Bucket bucket : topQueries.getBuckets()) {
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();

            if (!key.trim().isEmpty()) {
                keyList.add(key + " (" + docCount + ")");
            }
        }


        return keyList;
    }

    public List<String> autoRecommendWord(String keyword) {
        // BoolQuery 안에 들어갈 TermQuery와 ExistsQuery 생성
        TermQueryBuilder termQuery = QueryBuilders.termQuery("query", keyword);
        ExistsQueryBuilder existsQuery = QueryBuilders.existsQuery("oquery");

        // BoolQuery를 생성하고 must 조건을 추가
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(termQuery).must(existsQuery);

        // Source 필드 설정
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        sourceBuilder.fetchSource(new String[]{"query", "oquery"}, null);

        // SearchRequest 설정
        SearchRequest searchRequest = new SearchRequest("search-log");
        searchRequest.source(sourceBuilder);

        try {
            // 검색 요청 전송
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            Set<String> autoRecommend = new HashSet<>();
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                String oquery = (String) hit.getSourceAsMap().get("oquery");
                if (oquery != null && !oquery.isEmpty()) {
                    autoRecommend.add(oquery);
                }
            }

            // Set을 List로 변환
            List<String> autoRecommendList = new ArrayList<>(autoRecommend);
            Set<String> uniqueItems = new HashSet<>();

            for (String item : autoRecommendList) {
                String[] splitItems = item.replaceAll("\\s", "").split(",");
                for (String splitItem : splitItems) {
                    uniqueItems.add(splitItem);
                }
            }
            // 결과 출력
            List<String> result = new ArrayList<>(uniqueItems);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

}
