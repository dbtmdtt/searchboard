package com.example.searchboard.repository;

import com.example.searchboard.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class JdbcTemplateRepository implements MemberRepository {
    private final JdbcTemplate jdbcTemplate;
    @Override
    public boolean saveMember(Member member) {
        try {
            String sql = "insert into member(id, pw, roles) values (?, ?, 'User')";
            int n =  jdbcTemplate.update(sql, member.getId(), member.getPw());
            return n==1;
        } catch (DataAccessException e) {
            throw new RuntimeException("회원가입에 실패했습니다.", e);
        }

    }
    @Override
    public Member findMember(String id) {
        String sql ="select * from member where id=?";
        return jdbcTemplate.queryForObject(sql,(rs,rn) ->new Member(rs),id);

    }
    @Override
    public int duplicateMember(String id){
        String sql = "select count(*) from member where id = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count;
    }
}
