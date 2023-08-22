package com.example.searchboard.domain;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
@Data
public class Member {
    private Long memberId;

    private String id;

    private String pw;
    private String roles;
    public Member(){

    }
    public Member(ResultSet rs) throws SQLException {
        this.memberId = rs.getLong("memberId");
        this.id = rs.getString("id");
        this.pw = rs.getString("pw");
        this.roles = rs.getString("roles");

    }
}
