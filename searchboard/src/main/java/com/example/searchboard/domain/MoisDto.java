package com.example.searchboard.domain;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
public class MoisDto {
    private String writeDate;
    private String thumbnailImg;
    private String title;
    private String content;
    private String domain;
    private String writer;
    private String url;
    private String file_name;
    private String file_content;
    private Long count;
}
