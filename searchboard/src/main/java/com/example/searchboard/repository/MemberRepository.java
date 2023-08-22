package com.example.searchboard.repository;

import com.example.searchboard.domain.Member;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository {
    public Member findMember(String id);

    public boolean saveMember(Member member);


    public int duplicateMember(String id);
}
