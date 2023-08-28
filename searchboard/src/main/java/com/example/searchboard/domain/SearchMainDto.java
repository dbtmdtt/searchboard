package com.example.searchboard.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SearchMainDto {
    private List<YhnDto> yhnMainList;
    private List<MoisDto> moisMainList;
    private Pagination pagination;
    public SearchMainDto(){
        
    }
    public SearchMainDto(List<MoisDto> moisMainList, List<YhnDto> yhnMainList, Pagination pagination){
        this.moisMainList = moisMainList;
        this.yhnMainList = yhnMainList;
        this.pagination = pagination;

    }




}
