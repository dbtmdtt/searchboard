package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class MoisPagination {
    private List<MoisDto> moisMainList;
    private Pagination pagination;
    public MoisPagination(){

    }
    public  MoisPagination(List<MoisDto> moisMainList,Pagination pagination){
        this.moisMainList = moisMainList;
        this.pagination = pagination;
    }
}
