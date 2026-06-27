package com.jjikmeok.app.domain.discovery.search;

import com.jjikmeok.app.domain.discovery.dto.SearchResultDto;

import java.util.List;

public interface SearchService {

    List<SearchResultDto> search(String keyword, int limit);
}
