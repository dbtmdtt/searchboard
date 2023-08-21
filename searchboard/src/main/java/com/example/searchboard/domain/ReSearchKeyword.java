package com.example.searchboard.domain;

import lombok.Data;

import java.util.List;

@Data
public class ReSearchKeyword {
    private String keyword;
    private List<String> searchCategory;
}
