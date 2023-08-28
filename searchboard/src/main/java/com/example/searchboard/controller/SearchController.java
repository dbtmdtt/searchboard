package com.example.searchboard.controller;

import com.example.searchboard.domain.*;
import com.example.searchboard.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
@Slf4j
public class SearchController {
    private final SearchService searchService;
    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    @GetMapping("/forbiddenWord")
    @ResponseBody
    public List<String> forbiddenWords(){
        return searchService.searchForbiddenWords();
    }
    //수동 추천 ajax
    @GetMapping("/recommend")
    @ResponseBody
    public List<String> recommend(String keyword) {
        return searchService.recommendWord(keyword);
    }
    // 오타 교정 ajax
    @GetMapping("/getTypoSuggest")
    @ResponseBody
    public String getTypoSuggest(String search) throws IOException {
        return searchService.typoCorrect(search);
    }

    //자동 완성 ajax
    @GetMapping("/search")
    public ResponseEntity<List<String>> searchAutocomplete(@RequestParam String keyword) {
        List<String> keywordList = searchService.searchAutocomplete(keyword);
        if (keywordList != null) {
            return ResponseEntity.ok(keywordList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/engToKor")
    public ResponseEntity<List<String>> searchEngToKor(@RequestParam String keyword) {
        List<String> keywordList = searchService.searchAutocomplete(keyword);
        if (keywordList != null) {
            return ResponseEntity.ok(keywordList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/topWord")
    public ResponseEntity<List<String>> topWord() throws IOException {
        List<String> topWord = searchService.topWord();
        if (topWord != null) {
            return ResponseEntity.ok(topWord);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/autoRecommend")
    public ResponseEntity<List<String>> autoRecommend(@RequestParam String keyword) throws IOException {
        List<String> autoRecommendList = searchService.autoRecommendWord(keyword);
        log.debug("auto:{}",autoRecommendList );
        if (autoRecommendList != null) {
            return ResponseEntity.ok(autoRecommendList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
