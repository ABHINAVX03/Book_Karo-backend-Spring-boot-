package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.services.DistanceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class DistanceServiceOSRMImpl implements DistanceService {

    private static final String OSRM_API_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";
    private final RestClient restClient;

    public DistanceServiceOSRMImpl() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .baseUrl(OSRM_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public double calculateDistance(Point src, Point dest) {
        try {
            if (src == null || dest == null) {
                throw new IllegalArgumentException("Coordinates are required");
            }
            String uri = src.getX() + "," + src.getY() + ";" + dest.getX() + "," + dest.getY();
            log.debug("OSRM route request: {}", uri);

            OSRMResponseDto responseDto = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OSRMResponseDto.class);

            if (responseDto == null || responseDto.getRoutes() == null || responseDto.getRoutes().isEmpty()
                    || responseDto.getRoutes().get(0).getDistance() == null) {
                throw new IllegalStateException("OSRM did not return a valid route");
            }
            double distance = responseDto.getRoutes().get(0).getDistance() / 1000.0;
            log.debug("OSRM calculated distance={}km", distance);
            return distance;
        } catch (Exception e) {
            log.warn("OSRM distance calculation failed: {}", e.getMessage());
            throw new RuntimeException("Unable to calculate route distance right now");
        }
    }
}

@Data
class OSRMResponseDto {
    private List<OSRMRoute> routes;
}

@Data
class OSRMRoute {
    private Double distance;
}
