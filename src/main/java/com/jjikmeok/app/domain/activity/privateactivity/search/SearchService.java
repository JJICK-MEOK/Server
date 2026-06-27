package com.jjikmeok.app.domain.activity.privateactivity.search;

import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;

import java.util.List;

public interface SearchService {

    List<SearchResultDto> search(String keyword, int limit);
}
