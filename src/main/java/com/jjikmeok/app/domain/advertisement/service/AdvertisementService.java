package com.jjikmeok.app.domain.advertisement.service;

import com.jjikmeok.app.domain.advertisement.dto.request.AdvertisementRequest;
import com.jjikmeok.app.domain.advertisement.dto.response.AdvertisementResponse;
import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;

import java.util.List;

public interface AdvertisementService {

    List<AdvertisementResponse> getVisibleAdvertisements(AdvertisementPosition position);

    AdvertisementResponse getAdvertisement(Long id);

    AdvertisementResponse createAdvertisement(AdvertisementRequest request);

    AdvertisementResponse updateAdvertisement(Long id, AdvertisementRequest request);

    void deleteAdvertisement(Long id);
}
