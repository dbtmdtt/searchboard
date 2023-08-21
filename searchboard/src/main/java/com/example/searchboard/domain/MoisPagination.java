package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class MoisPagination {
    private List<MoisDto> moisMainList;
    private Pagination pagination;
    private String keyword;
    public MoisPagination(){

    }
    public  MoisPagination(List<MoisDto> moisMainList,Pagination pagination, String keyword){
        this.moisMainList = moisMainList;
        this.pagination = pagination;
        this.keyword = keyword;
    }
}
