package com.example.searchboard.domain;

import lombok.Data;

@Data
public class MoisPhotoDto {
    private String writeDate;
    private String thumbnailImg;
    private String crawlDate;
    private String title;
    private String fileOrgName;
    private String nttId;
    private String content;
    private String domain;
    private String writer;
}
