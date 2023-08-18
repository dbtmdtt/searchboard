package com.example.searchboard;

import com.example.searchboard.domain.MoisDto;
import com.example.searchboard.domain.MoisPagination;
import com.example.searchboard.domain.SearchMainDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
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

//    @GetMapping("/")
//    public String searchMain() throws IOException {
//        return "searchMain";
//    }
//
//    @GetMapping("/main")
//    public String main(Model model, @RequestParam(required = false) String keyword,
//                             @RequestParam(required = false) List<String> searchCategory) throws IOException {
//        SearchMainDto mainList = searchService.mainList(keyword, searchCategory);
//
//        List<String> domains = Arrays.asList("mois_photo", "mois_attach");
//        List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프","연예", "스포츠", "오피니언", "사람들");
//        model.addAttribute("domains", domains);
//        model.addAttribute("categories", categories);
//        model.addAttribute("yhnMain", mainList.getYhnMainList());
//        model.addAttribute("moisMain", mainList.getMoisMainList());
//        model.addAttribute("pagination",mainList.getPagination());
//
//        return "main";
//    }

    @GetMapping("/")
    public String main(Model model, @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) List<String> searchCategory,@RequestParam(defaultValue = "1") int page) throws IOException {
        SearchMainDto mainList = searchService.mainList(keyword, searchCategory);

        List<String> domains = Arrays.asList("mois_photo", "mois_attach");
        List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프","연예", "스포츠", "오피니언", "사람들");
        model.addAttribute("domains", domains);
        model.addAttribute("categories", categories);
        model.addAttribute("yhnMain", mainList.getYhnMainList());
        model.addAttribute("moisMain", mainList.getMoisMainList());
        model.addAttribute("pagination",mainList.getPagination());

        return "realMain";
    }
    @GetMapping("/moisAttach")
    public String searchMoisAttach(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String domain1 = "mois_attach";
        MoisPagination moisList = searchService.moisList(keyword, searchCategory, domain1, page);
        model.addAttribute("moisList", moisList.getMoisMainList());
        model.addAttribute("pagination",moisList.getPagination());
        return "realAttach";
    }

//    @GetMapping("/moisPhoto")
//    public String searchMoisPhoto(Model model, @RequestParam(required = false) String keyword,
//                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
//        String domain1 = "mois_photo";
//        MoisPagination moisList = searchService.moisList(keyword, searchCategory, domain1, page);
//        model.addAttribute("moisList", moisList.getMoisMainList());
//        model.addAttribute("pagination",moisList.getPagination());
//        return "realAttach";
//    }



}
