package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class SearchResultDto {
    private List<YhnDto> yhnMainList;
    private List<MoisDto> moisMainList;
}
