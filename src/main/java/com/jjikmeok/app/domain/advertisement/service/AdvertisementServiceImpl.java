package com.jjikmeok.app.domain.advertisement.service;

import com.jjikmeok.app.domain.advertisement.converter.AdvertisementConverter;
import com.jjikmeok.app.domain.advertisement.dto.request.AdvertisementRequest;
import com.jjikmeok.app.domain.advertisement.dto.response.AdvertisementResponse;
import com.jjikmeok.app.domain.advertisement.entity.Advertisement;
import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import com.jjikmeok.app.domain.advertisement.repository.AdvertisementRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertisementServiceImpl implements AdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    @Override
    public List<AdvertisementResponse> getVisibleAdvertisements(AdvertisementPosition position) {
        return advertisementRepository.findVisibleAdvertisements(position, LocalDateTime.now()).stream()
                .map(AdvertisementConverter::toResponse)
                .toList();
    }

    @Override
    public AdvertisementResponse getAdvertisement(Long id) {
        return AdvertisementConverter.toResponse(findVisibleAdvertisementOrThrow(id));
    }

    @Override
    @Transactional
    public AdvertisementResponse createAdvertisement(AdvertisementRequest request) {
        validateRequest(request);
        Advertisement advertisement = AdvertisementConverter.toEntity(request);
        return AdvertisementConverter.toResponse(advertisementRepository.save(advertisement));
    }

    @Override
    @Transactional
    public AdvertisementResponse updateAdvertisement(Long id, AdvertisementRequest request) {
        validateRequest(request);
        Advertisement advertisement = findAdvertisementOrThrow(id);
        advertisement.update(
                request.title().trim(),
                request.imageUrl().trim(),
                request.redirectUrl().trim(),
                request.position(),
                request.sortOrder(),
                request.startAt(),
                request.endAt(),
                request.isActive()
        );
        return AdvertisementConverter.toResponse(advertisement);
    }

    @Override
    @Transactional
    public void deleteAdvertisement(Long id) {
        Advertisement advertisement = findAdvertisementOrThrow(id);
        advertisement.deactivate();
    }

    private Advertisement findAdvertisementOrThrow(Long id) {
        return advertisementRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ADVERTISEMENT_NOT_FOUND));
    }

    private Advertisement findVisibleAdvertisementOrThrow(Long id) {
        return advertisementRepository.findVisibleAdvertisementById(id, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.ADVERTISEMENT_NOT_FOUND));
    }

    private void validateRequest(AdvertisementRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        validateUrl(request.imageUrl());
        validateUrl(request.redirectUrl());
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new CustomException(ErrorCode.ADVERTISEMENT_INVALID_PERIOD);
        }
    }

    private void validateUrl(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.ADVERTISEMENT_INVALID_URL);
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!validScheme || uri.getHost() == null) {
                throw new CustomException(ErrorCode.ADVERTISEMENT_INVALID_URL);
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ADVERTISEMENT_INVALID_URL);
        }
    }
}
