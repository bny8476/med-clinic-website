package com.healthcare.clinic.integration.pharmacy.client;

import com.healthcare.clinic.integration.pharmacy.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class PharmacyClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public PharmacyClient(
            @Value("${pharmacy.service.url:https://pms-pharmadesk.onrender.com}") String baseUrl,
            @Value("${pharmacy.api.key:}") String apiKey,
            @Value("${pharmacy.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${pharmacy.read-timeout-ms:10000}") int readTimeout) {
        
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }
        return headers;
    }

    public List<PharmacyMedicineDto> searchMedicines(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String url = baseUrl + "/api/v1/medicines/search?keyword=" + keyword.trim();
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<List<PharmacyMedicineDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<PharmacyMedicineDto>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("External Pharmacy service error searching medicines (url={}): {}", url, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error searching medicines on external Pharmacy service", e);
        }
        return Collections.emptyList();
    }

    public PharmacyMedicineDto getMedicineById(String medicineId) {
        String url = baseUrl + "/api/v1/medicines/" + medicineId;
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<PharmacyMedicineDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PharmacyMedicineDto.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch medicine #{} from external Pharmacy: {}", medicineId, e.getMessage());
        }
        return null;
    }

    public PharmacyStockAvailabilityDto checkStockAvailability(String medicineId) {
        String url = baseUrl + "/api/v1/medicines/" + medicineId + "/availability";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<PharmacyStockAvailabilityDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PharmacyStockAvailabilityDto.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed stock availability check for medicine #{}: {}", medicineId, e.getMessage());
        }
        return PharmacyStockAvailabilityDto.builder()
                .medicineId(medicineId)
                .inStock(false)
                .availableQuantity(0)
                .build();
    }

    public PharmacyOrderResponseDto sendPrescriptionOrder(PharmacyOrderRequestDto orderRequest) {
        String url = baseUrl + "/api/v1/orders";
        try {
            HttpEntity<PharmacyOrderRequestDto> entity = new HttpEntity<>(orderRequest, createHeaders());
            ResponseEntity<PharmacyOrderResponseDto> response = restTemplate.postForEntity(
                    url, entity, PharmacyOrderResponseDto.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("External Pharmacy service returned error for order submission (Rx #{}): {}", 
                    orderRequest.getClinicPrescriptionId(), e.getMessage());
            throw e;
        }
        throw new RuntimeException("External Pharmacy service order submission failed");
    }

    public PharmacyOrderResponseDto getOrderStatus(String referenceId) {
        String url = baseUrl + "/api/v1/orders/" + referenceId + "/status";
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<PharmacyOrderResponseDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PharmacyOrderResponseDto.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to check status for external order #{}: {}", referenceId, e.getMessage());
        }
        return null;
    }
}
