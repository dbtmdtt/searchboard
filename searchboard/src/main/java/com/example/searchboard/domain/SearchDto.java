package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class SearchDto {
    String keyword;
    String category;
    List<String> searchCategory;
    int page;
    String sortOrder;
    String periodStart;
    String periodEnd;
    String reKeyword;
    SearchParseDto searchParseDto;
}
