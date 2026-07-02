package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivityRegionResolver {

    private final RegionRepository regionRepository;

    public Region resolve(String title, String address, Long defaultRegionId) {
        String source = ((address == null ? "" : address) + " " + (title == null ? "" : title));

        List<Region> regions = regionRepository.findAll();

        Region district = regions.stream()
                .filter(r -> r.getParent() != null)
                .filter(r -> containsDistrict(source, r.getName()))
                .findFirst()
                .orElse(null);

        if (district != null) return district;

        Region province = regions.stream()
                .filter(r -> r.getParent() == null)
                .filter(r -> containsProvince(source, r.getName()))
                .findFirst()
                .orElse(null);

        if (province != null) return province;

        return regionRepository.findById(defaultRegionId)
                .orElseThrow(() -> new IllegalStateException("기본 지역을 찾을 수 없습니다. id=" + defaultRegionId));
    }

    private boolean containsDistrict(String source, String name) {
        if (source == null || name == null) return false;
        if (name.endsWith("전체")) return false;

        return source.contains(name + "구")
                || source.contains(name + "시")
                || source.contains(name + "군")
                || source.contains(name);
    }

    private boolean containsProvince(String source, String name) {
        if (source == null || name == null) return false;

        return switch (name) {
            case "서울" -> source.contains("서울");
            case "경기" -> source.contains("경기") || containsAny(source, "수원", "성남", "고양", "용인", "부천", "안산", "안양", "화성", "평택");
            case "인천" -> source.contains("인천");
            case "강원" -> source.contains("강원") || containsAny(source, "춘천", "원주", "강릉");
            case "충북" -> source.contains("충북") || source.contains("청주");
            case "충남" -> source.contains("충남") || containsAny(source, "천안", "아산", "당진");
            case "세종" -> source.contains("세종");
            case "대전" -> source.contains("대전");
            case "광주" -> source.contains("광주");
            case "전북" -> source.contains("전북") || source.contains("전주");
            case "전남" -> source.contains("전남") || containsAny(source, "순천", "여수", "목포");
            case "경북" -> source.contains("경북") || containsAny(source, "포항", "경주", "구미");
            case "대구" -> source.contains("대구");
            case "제주" -> source.contains("제주");
            case "경남/울산" -> source.contains("경남") || source.contains("울산") || containsAny(source, "창원", "김해", "진주");
            case "부산" -> source.contains("부산");
            default -> source.contains(name);
        };
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }
}