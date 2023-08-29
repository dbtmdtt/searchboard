package com.example.searchboard.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SearchMainDto {
    private List<ListDto> yhnMainList;
    private List<ListDto> moisMainList;
    private List<ListDto> mainList;
    private Pagination pagination;
    public SearchMainDto(){
        
    }
    public SearchMainDto(List<ListDto> moisMainList, List<ListDto> yhnMainList, Pagination pagination,  List<ListDto> mainList){
        this.moisMainList = moisMainList;
        this.yhnMainList = yhnMainList;
        this.pagination = pagination;
        this.mainList = mainList;

    }




}
