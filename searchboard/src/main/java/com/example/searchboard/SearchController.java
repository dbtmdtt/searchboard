package com.example.searchboard;

import com.example.searchboard.domain.MoisPagination;
import com.example.searchboard.domain.ReSearchKeyword;
import com.example.searchboard.domain.SearchMainDto;
import com.example.searchboard.domain.YhnPagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

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
                       @RequestParam(required = false) List<String> searchCategory, @ModelAttribute ReSearchKeyword searchRequestDto) throws IOException {
        SearchMainDto mainList = searchService.mainList(keyword, searchCategory);

        List<String> domains = Arrays.asList("mois_photo", "mois_attach");
        List<String> categories = Arrays.asList("정치", "전체기사", "경제", "산업", "사회", "전국", "세계", "문화", "라이프","연예", "스포츠", "오피니언", "사람들");
        model.addAttribute("domains", domains);
        model.addAttribute("categories", categories);
        model.addAttribute("yhnMain", mainList.getYhnMainList());
        model.addAttribute("moisMain", mainList.getMoisMainList());
        model.addAttribute("pagination",mainList.getPagination());

        return "searchMain";
    }
    @GetMapping("/moisAttach")
    public String searchMoisAttach(Model model, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page,
                                   @RequestParam(defaultValue = "dateDesc") String sortOrder) throws IOException {
        String domain1 = "mois_attach";
        MoisPagination moisList = searchService.moisPageList(keyword, searchCategory, domain1, page, sortOrder);
        model.addAttribute("moisList", moisList.getMoisMainList());
        model.addAttribute("pagination",moisList.getPagination());
        return "moisAttach";
    }

//    @GetMapping("/moisPhoto")
//    public String searchMoisPhoto(Model model, @RequestParam(required = false) String keyword,
//                                   @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
//        String domain1 = "mois_photo";
//        MoisPagination moisList = searchService.moisPageList(keyword, searchCategory, domain1, page);
//        model.addAttribute("moisList", moisList.getMoisMainList());
//        model.addAttribute("pagination",moisList.getPagination());
//        return "moisPhoto";
//    }

    @GetMapping("/yhnPolitics")
    public String searchyhnPolitics(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "정치";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnPolitics";
    }
    @GetMapping("/yhnCulture")
    public String searchyhnCulture(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "문화";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnCulture";
    }
    @GetMapping("/yhnEconomy")
    public String searchyhnEconomy(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "경제";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnEconomy";
    }
    @GetMapping("/yhnEnter")
    public String searchyhnEnter(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "연예";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnEnter";
    }
    @GetMapping("/yhnIndustry")
    public String searchyhnIndustry(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "산업";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnIndustry";
    }
    @GetMapping("/yhnKorea")
    public String searchyhnKorea(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "전국";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnKorea";
    }
    @GetMapping("/yhnLife")
    public String searchyhnLife(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "라이프";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnLife";
    }
    @GetMapping("/yhnNorth")
    public String searchyhnNorth(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "전체기사";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnNorth";
    }
    @GetMapping("/yhnOpinion")
    public String searchyhnOpinion(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "오피니언";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnOpinion";
    }
    @GetMapping("/yhnPeople")
    public String searchyhnPeople(Model model, @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "사람들";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnPeople";
    }
    @GetMapping("/yhnSocial")
    public String searchyhnSocial(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "사회";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnSocial";
    }
    @GetMapping("/yhnSports")
    public String searchyhnSports(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "스포츠";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnSports";
    }
    @GetMapping("/yhnWorld")
    public String searchyhnWorld(Model model, @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) List<String> searchCategory, @RequestParam(defaultValue = "1")int page) throws IOException {
        String category = "세계";
        YhnPagination yhnList = searchService.yhnPageList(keyword, searchCategory, category, page);
        model.addAttribute("yhnList", yhnList.getYhnMainList());
        model.addAttribute("pagination",yhnList.getPagination());
        return "yhnWorld";
    }



}
