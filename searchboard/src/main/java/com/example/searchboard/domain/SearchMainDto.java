package com.example.searchboard.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SearchMainDto {
    private List<YhnDto> yhnMainList;
    private List<MoisDto> moisMainList;
    private Map<String, Long> domainMoisHitsCountMap;
    private Map<String, Long> domainYhnHitsCountMap;
    private Pagination pagination;


    public void setDomainMoisHitsCount(String domain, Long count) {
        if (domainMoisHitsCountMap == null) {
            domainMoisHitsCountMap = new HashMap<>();
        }
        domainMoisHitsCountMap.put(domain, count);
    }

    public void setDomainYhnHitsCount(String domain, Long count) {
        if (domainYhnHitsCountMap == null) {
            domainYhnHitsCountMap = new HashMap<>();
        }
        domainYhnHitsCountMap.put(domain, count);
    }


}
