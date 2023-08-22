package com.example.searchboard.service;

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

import java.io.IOException;
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
            // Check if keyword is in forbiddenWords list
            recommendWord(keyword);

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
    //오타교정
    public String typoCorrect(String search) throws IOException {
        if(search.isEmpty() == true || search == null){
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
        } catch (NullPointerException e){
            return null;
        }
//        System.out.println(options.size());
        if(options.size() == 0){    // 오타제안이 없다면 (검색결과가 존재한다면) null Return
            return null;
        }
        String lastOptionText = options.get(0).get("text").asText();
        System.out.println("오타교정 : " + lastOptionText);
        String normalizedText = Normalizer.normalize(lastOptionText, Normalizer.Form.NFC);


        return normalizedText;      // 오타 교정 제안이 있다면 해당 String Return (option 마지막 인덱스의 Score가 가장 높은 String)
    }

    public static String translateToKorean(String input) {
        String[] englishKeys = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "g", "h", "j", "k", "l", "z", "x", "c", "v", "b", "n", "m"};
        String[] koreanCharacters = {"ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ", "ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ", "ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ"};

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int index = -1;

            // 영어 키보드 문자의 인덱스를 찾습니다.
            for (int j = 0; j < englishKeys.length; j++) {
                if (englishKeys[j].equalsIgnoreCase(String.valueOf(c))) {
                    index = j;
                    break;
                }
            }

            // 해당 인덱스에 대응하는 한글 문자를 추가합니다.
            if (index != -1) {
                result.append(koreanCharacters[index]);
            } else {
                result.append(c);
            }
        }
        String asdf="ㄱㅏㄴㅏ";
        String normalizedResult = "";
        normalizedResult = Normalizer.normalize(result.toString(), Normalizer.Form.NFC);
         log.debug("english to korean normalized: {}", normalizedResult);
        log.debug("english to korean normalized: {}1", Normalizer.normalize(asdf, Normalizer.Form.NFC));
        return normalizedResult;
    }

    ////통합검색 메인 출력
    private List<MoisDto> getMoisMainList(SearchRequest searchRequest_mois, SearchSourceBuilder mainBuilder, String[] moisDomain) throws IOException {
        List<MoisDto> moisMainList = new ArrayList<>();

        for (String domain : moisDomain) {
            mainBuilder.query(QueryBuilders.matchQuery("domain", domain));
            SearchResponse moisResponse = client.search(searchRequest_mois.source(mainBuilder), RequestOptions.DEFAULT);
            log.debug("filecontet:{}", searchRequest_mois);
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
                /* Create a multi-match query for the given keyword and specific fields */
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
            log.debug("asdf:{}",keywordList);
            return keywordList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<String>  searchForbiddenWords( ) {
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

    public List<String> recommendWord(String keyword){
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
            }else{
                return null;
            }

            return recommendList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }






}
