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
        return resolve(null, title, address, defaultRegionId);
    }

    public Region resolve(String regionName, String title, String address, Long defaultRegionId) {
        String source = join(regionName, title, address);
        List<Region> regions = regionRepository.findAll();

        Region district = regions.stream()
                .filter(r -> r.getParent() != null)
                .filter(r -> matches(source, r.getName()))
                .findFirst()
                .orElse(null);
        if (district != null) {
            return district;
        }

        Region province = regions.stream()
                .filter(r -> r.getParent() == null)
                .filter(r -> matches(source, r.getName()))
                .findFirst()
                .orElse(null);
        if (province != null) {
            return province;
        }

        return regionRepository.findById(defaultRegionId)
                .orElseGet(() -> regions.stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("기본 지역을 찾을 수 없습니다. id=" + defaultRegionId)));
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private boolean matches(String source, String name) {
        if (source == null || source.isBlank() || name == null || name.isBlank()) {
            return false;
        }

        String normalizedSource = normalize(source);
        String normalizedName = normalize(name);
        if (normalizedSource.contains(normalizedName)) {
            return true;
        }

        return normalizedSource.contains(stripSuffix(normalizedName));
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", "").replace("-", "").trim().toLowerCase();
    }

    private String stripSuffix(String value) {
        return value
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("특별자치시", "")
                .replace("특별자치도", "")
                .replace("자치시", "")
                .replace("자치도", "")
                .replace("시", "")
                .replace("군", "")
                .replace("구", "");
    }
}
