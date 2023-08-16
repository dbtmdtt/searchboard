package com.example.searchboard;

import com.example.searchboard.domain.SearchMainDto;
import javassist.compiler.ast.Keyword;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
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

    @GetMapping("/")
    public String searchMain(Model model, @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) List<String> searchCategory) throws IOException {
        SearchMainDto mainList = searchService.mainList(keyword, searchCategory);
        List<String> domains = Arrays.asList("mois_photo", "mois_attach");
        List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프","연예", "스포츠", "오피니언", "사람들");
        model.addAttribute("domains", domains);
        model.addAttribute("categories", categories);
        model.addAttribute("yhnMain", mainList.getYhnMainList());
        model.addAttribute("moisMain", mainList.getMoisMainList());
//        model.addAttribute("moisCount", mainList.getDomainMoisHitsCountMap());
//        model.addAttribute("yhnCount", mainList.getDomainYhnHitsCountMap());
//        log.debug("asdf:{}, {} ",mainList.getYhnMainList());

        return "searchMain";
    }
    @GetMapping("/moisAttach")
    public String searchAttach(Model model, @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) List<String> searchCategory) throws IOException {
        SearchMainDto mainList = searchService.mainList(keyword, searchCategory);
        model.addAttribute("moisMain", mainList.getMoisMainList());
        return "moisAttach";
    }


}
