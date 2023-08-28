package com.example.searchboard.controller;

import com.example.searchboard.domain.SearchMainDto;
import com.example.searchboard.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("")
public class PageController {
    List<String> domains = Arrays.asList("mois_photo", "mois_attach");
    List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프", "연예", "스포츠", "오피니언", "사람들");
    private final SearchService searchService;

    @Autowired
    public PageController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) String keyword, @RequestParam (defaultValue = "integration") String category,
                        @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1") int page,
                        @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                        @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd, @RequestParam(required = false) String reKeyword) throws IOException {
        SearchMainDto mainList;
        if (category.equals("integration") || category.isEmpty()) {
            mainList = searchService.mainList(keyword, searchCategory, sortOrder, periodStart, periodEnd, reKeyword, user.getUsername());
        } else if (category.equals("mois_photo") || category.equals("mois_attach")) {
            mainList = searchService.moisPageList(keyword, searchCategory, category, page, sortOrder, periodStart, periodEnd, reKeyword);
        } else {
            mainList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder, periodStart, periodEnd, reKeyword);
        }

        model.addAttribute("domains", domains);
        model.addAttribute("categories", categories);
        model.addAttribute("moisMain", mainList.getMoisMainList());
        model.addAttribute("yhnMain", mainList.getYhnMainList());
        model.addAttribute("pagination", mainList.getPagination());
        model.addAttribute("user", user.getUsername());
        switch (category) {
            case "integration":
                return "page/searchMain";
            case "mois_attach":
                return "page/moisAttach";
            case "mois_photo":
                return "page/moisPhoto";
            case "정치":
                return "page/yhnPolitics";
            case "문화":
                return "page/yhnCulture";
            case "경제":
                return "page/yhnEconomy";
            case "연예":
                return "page/yhnEnter";
            case "산업":
                return "page/yhnIndustry";
            case "전국":
                return "page/yhnKorea";
            case "라이프":
                return "page/yhnLife";
            case "전체기사":
                return "page/yhnNorth";
            case "오피니언":
                return "page/yhnOpinion";
            case "사람들":
                return "page/yhnPeople";
            case "사회":
                return "page/yhnSocial";
            case "스포츠":
                return "page/yhnSports";
            case "세계":
                return "page/yhnWorld";
            default:
                return "page/searchMain";
        }
    }

}
