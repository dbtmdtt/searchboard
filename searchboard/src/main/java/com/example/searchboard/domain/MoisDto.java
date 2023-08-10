package com.example.searchboard.domain;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
public class MoisDto {
    private List<MoisAttachDto> moisAttachDto;
    private List<MoisPhotoDto> moisPhotoDto;
    public MoisDto(List<MoisAttachDto> moisAttachDto,List<MoisPhotoDto> moisPhotoDto){
        this.moisAttachDto = moisAttachDto;
        this.moisPhotoDto = moisPhotoDto;
    }
}
