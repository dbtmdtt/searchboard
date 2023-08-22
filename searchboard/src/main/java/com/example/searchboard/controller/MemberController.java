package com.example.searchboard.controller;

import com.example.searchboard.service.MemberService;
import com.example.searchboard.domain.Member;
import com.example.searchboard.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@Slf4j
public class MemberController {
    private final MemberService memberService;
    private final MemberRepository repository;

    public MemberController(MemberService memberService, MemberRepository repository) {
        this.memberService = memberService;
        this.repository = repository;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "exception", required = false) String exception, 	Model model) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);
        return "login";
    }

    @GetMapping("/SignUp")
    public String join(Model model) {
        model.addAttribute("SignUp", new Member());
        return "signUp";
    }
    @PostMapping("/SignUp")
    public String join(@ModelAttribute  Member member, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        member.setId(member.getId().trim());
        member.setPw(member.getPw().trim());
        int idchk = memberService.duplicatedIdCheck(member.getId());
        if(idchk > 0){
            redirectAttributes.addFlashAttribute("msg", "중복된 아이디가 존재합니다.");
            return "redirect:/SignUp";
        }
        if(bindingResult.hasErrors()){
            log.error("입력값 오류 에러");
            redirectAttributes.addFlashAttribute("msg", "아이디와 비밀번호는 필수입니다.");
            return "redirect:/SignUp";
        }else{
            memberService.join(member);
        }

        return "redirect:/login";
    }

}
