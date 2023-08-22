package com.example.searchboard.service;

import com.example.searchboard.domain.Member;
import com.example.searchboard.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository){
        this.memberRepository = memberRepository;

    }

    public Member findMember(String memberId) {

        return memberRepository.findMember(memberId);
    }
    public int duplicatedIdCheck(String id){
        int idCheck = memberRepository.duplicateMember(id);
        return idCheck;
    }


    public boolean join(Member member){

        String encodedPassword = passwordEncoder.encode(member.getPw());
        member.setPw((encodedPassword));

        return memberRepository.saveMember(member);




    }
}
