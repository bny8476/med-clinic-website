package com.healthcare.clinic.doctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.healthcare.clinic.doctor.dto.ExternalMedicineDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalMedicineSearchService {

    private static final Logger log = LoggerFactory.getLogger(ExternalMedicineSearchService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Bounded Caffeine cache (500 max entries, 3 hours TTL) to prevent heap accumulation
    private final Cache<String, List<ExternalMedicineDto>> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(3, TimeUnit.HOURS)
            .build();

    public ExternalMedicineSearchService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<ExternalMedicineDto> searchMedicines(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String key = name.trim().toLowerCase();

        return cache.get(key, k -> fetchFromRxNorm(k));
    }

    private List<ExternalMedicineDto> fetchFromRxNorm(String term) {
        String url = "https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term={term}&maxEntries=10";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, term);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseRxNormResponse(response.getBody());
            }
        } catch (RestClientException e) {
            log.warn("Error calling RxNorm API for term '{}': {}", term, e.getMessage());
        } catch (Exception e) {
            log.error("Error parsing RxNorm response for term '{}'", term, e);
        }
        return Collections.emptyList();
    }

    private List<ExternalMedicineDto> parseRxNormResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode approximateGroup = root.path("approximateGroup");
        JsonNode candidateArray = approximateGroup.path("candidate");

        if (candidateArray.isMissingNode() || !candidateArray.isArray()) {
            return Collections.emptyList();
        }

        Map<String, ExternalMedicineDto> deduped = new LinkedHashMap<>();
        for (JsonNode node : candidateArray) {
            String rxcui = node.path("rxcui").asText(null);
            String candidateName = node.path("name").asText(null);

            if (rxcui != null && candidateName != null && !candidateName.trim().isEmpty()) {
                String lowercaseName = candidateName.toLowerCase();
                deduped.putIfAbsent(lowercaseName, ExternalMedicineDto.builder()
                        .name(candidateName)
                        .rxcui(rxcui)
                        .build());
            }
        }

        return new ArrayList<>(deduped.values());
    }
}
