package com.example.searchboard;

import com.example.searchboard.domain.MoisAttachDto;
import com.example.searchboard.domain.MoisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@Slf4j
public class SearchController {
    private final SearchService searchService;
    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String searchMain(Model model) {
        MoisDto searchResults = searchService.mapSearchResults();
        model.addAttribute("searchResult", searchResults);
        return "searchMain";
    }
}
