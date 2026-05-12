package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.services.DistanceService;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class DistanceServiceOSRMImpl implements DistanceService {

    private static final String OSRM_API_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    @Override
    public double calculateDistance(Point src, Point dest) {
        try {
            if (src == null || dest == null) {
                throw new RuntimeException("Coordinates are null");
            }
            String uri = src.getX() + "," + src.getY() + ";" + dest.getX() + "," + dest.getY();
            System.out.println("OSRM Request: " + uri);

            OSRMResponseDto responseDto = RestClient.builder()
                    .baseUrl(OSRM_API_BASE_URL)
                    .build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(OSRMResponseDto.class);

            double distance = responseDto.getRoutes().get(0).getDistance() / 1000.0;
            System.out.println("Calculated distance: " + distance);
            return distance;
        } catch (Exception e) {
            e.printStackTrace();  // Add this to see the actual error
            throw new RuntimeException("Error getting data from OSRM: " + e.getMessage());
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