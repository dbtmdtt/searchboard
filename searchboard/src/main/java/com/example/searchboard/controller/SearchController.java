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
import org.springframework.web.bind.annotation.ModelAttribute;
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


    @GetMapping("/")
    public String main(Model model, @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) List<String> searchCategory,
                       @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                       @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd, @RequestParam(required = false) String preKeyword) throws IOException {
        SearchMainDto mainList = searchService.mainList(keyword, searchCategory,sortOrder,periodStart, periodEnd, preKeyword, user.getUsername());
        List<String> domains = Arrays.asList("mois_photo", "mois_attach");
        List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프","연예", "스포츠", "오피니언", "사람들");
        model.addAttribute("domains", domains);
        model.addAttribute("categories", categories);
        model.addAttribute("yhnMain", mainList.getYhnMainList());
        model.addAttribute("moisMain", mainList.getMoisMainList());
        model.addAttribute("pagination",mainList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "searchMain";
    }
    @GetMapping("/moisAttach")
    public String searchMoisAttach(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                   @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                   @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String domain1 = "mois_attach";

        MoisPagination moisList = searchService.moisPageList(keyword, searchCategory, domain1, page, sortOrder ,periodStart, periodEnd, user.getUsername());
        model.addAttribute("moisList", moisList.getMoisMainList());
        model.addAttribute("pagination",moisList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "moisAttach";
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



    @GetMapping("/moisPhoto")
    public String searchMoisPhoto(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                  @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                  @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String domain1 = "mois_photo";
        MoisPagination moisList = searchService.moisPageList(keyword, searchCategory, domain1, page, sortOrder ,periodStart, periodEnd, user.getUsername());
        model.addAttribute("moisList", moisList.getMoisMainList());
        model.addAttribute("pagination",moisList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "moisPhoto";
    }



    @GetMapping("/yhnPolitics")
    public String searchyhnPolitics(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                    @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                    @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "정치";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnPolitics";
    }
    @GetMapping("/yhnCulture")
    public String searchyhnCulture(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                   @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                   @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "문화";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnCulture";
    }
    @GetMapping("/yhnEconomy")
    public String searchyhnEconomy(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                   @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                   @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "경제";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnEconomy";
    }
    @GetMapping("/yhnEnter")
    public String searchyhnEnter(Model model, @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                 @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                 @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "연예";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnEnter";
    }
    @GetMapping("/yhnIndustry")
    public String searchyhnIndustry(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                    @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                    @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "산업";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnIndustry";
    }
    @GetMapping("/yhnKorea")
    public String searchyhnKorea(Model model, @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                 @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                 @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "전국";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnKorea";
    }
    @GetMapping("/yhnLife")
    public String searchyhnLife(Model model, @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "라이프";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnLife";
    }
    @GetMapping("/yhnNorth")
    public String searchyhnNorth(Model model, @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                 @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                 @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "전체기사";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnNorth";
    }
    @GetMapping("/yhnOpinion")
    public String searchyhnOpinion(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                   @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                   @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "오피니언";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnOpinion";
    }
    @GetMapping("/yhnPeople")
    public String searchyhnPeople(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                  @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                  @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "사람들";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnPeople";
    }
    @GetMapping("/yhnSocial")
    public String searchyhnSocial(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                  @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                  @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "사회";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnSocial";
    }
    @GetMapping("/yhnSports")
    public String searchyhnSports(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                  @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                  @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "스포츠";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnSports";
    }
    @GetMapping("/yhnWorld")
    public String searchyhnWorld(Model model, @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                 @AuthenticationPrincipal User user, @RequestParam(defaultValue = "dateDesc") String sortOrder,
                                 @RequestParam(required = false) String periodStart, @RequestParam(required = false) String periodEnd) throws IOException {
        String category = "세계";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page, sortOrder ,periodStart, periodEnd);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        model.addAttribute("user",user.getUsername());
        return "yhnWorld";
    }

}
