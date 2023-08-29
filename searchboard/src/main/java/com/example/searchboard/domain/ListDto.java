package com.example.searchboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListDto {
    private String writeDate;
    private String thumbnailImg;
    private String title;
    private String content;
    private String domain;
    private String writer;
    private String category_one_depth;
    private String url;
    private String file_name;
    private String file_content;

}
