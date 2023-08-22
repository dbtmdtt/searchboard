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
    private List<String> forbiddenWord;
    private String keyword;




}
