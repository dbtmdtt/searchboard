package com.example.searchboard.service;

import com.example.searchboard.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final MemberService memberService;


    @Override
    public UserDetails loadUserByUsername(String insertedUserId) throws UsernameNotFoundException {

        Member member =  memberService.findMember(insertedUserId);
        return User.builder()
                .username(member.getId())
                .password(member.getPw())
                .roles(member.getRoles())
                .build();
    }
}