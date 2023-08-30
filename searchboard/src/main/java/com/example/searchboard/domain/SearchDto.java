//if (!ObjectUtils.isEmpty(searchRequest_mois)) {
//        for (String domain : moisDomain) {
//        // Reset boolQuery for each domain iteration
//        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//        setKeyword(keyword, searchCategory, boolQuery);
//
//        // Create a filter for the given domain using match query
//        TermQueryBuilder domainMatchQuery = QueryBuilders.termQuery("domain", domain);
//        boolQuery.filter(domainMatchQuery);
//
//        // Apply date range query if applicable
//        if (dateRangeQuery != null) {
//        boolQuery.filter(dateRangeQuery);
//        }
//
//        mainBuilder.query(boolQuery);
//        mainBuilder.highlighter(createHighlightBuilder());
//        searchRequest_mois.source(mainBuilder);
//
//        SearchHit[] hits = client.search(searchRequest_mois, RequestOptions.DEFAULT).getHits().getHits();
//
//        moisMain.addAll(totalList(hits));
//        resultDto.setMoisMainList(moisMain);
//        }
//        }
//}