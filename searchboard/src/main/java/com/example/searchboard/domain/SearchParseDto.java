package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;
@Data
public class SearchParseDto {
    List<String> must[];
    List<String> mustNot[];
    List<String> matchPhrase[];
}
