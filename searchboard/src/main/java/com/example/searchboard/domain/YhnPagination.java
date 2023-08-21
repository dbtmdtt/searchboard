package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class YhnPagination {
    private List<YhnDto> yhnMainList;
    private Pagination pagination;
    private String keyword;
    public YhnPagination(){

    }
    public YhnPagination(List<YhnDto> yhnMainList, Pagination pagination,String keyword){
        this.yhnMainList = yhnMainList;
        this.pagination = pagination;
        this.keyword = keyword;
    }
}
