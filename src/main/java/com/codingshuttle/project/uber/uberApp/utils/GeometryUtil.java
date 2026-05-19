package com.codingshuttle.project.uber.uberApp.utils;

import com.codingshuttle.project.uber.uberApp.dto.PointDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeometryUtil {

    public static Point createPoint(PointDto pointDto) {
        if (pointDto == null || pointDto.getCoordinates() == null || pointDto.getCoordinates().length != 2) {
            throw new IllegalArgumentException("Coordinates must be [longitude, latitude]");
        }
        double longitude = pointDto.getCoordinates()[0];
        double latitude = pointDto.getCoordinates()[1];
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Coordinates are outside valid longitude/latitude ranges");
        }
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Coordinate coordinate = new Coordinate(longitude, latitude);
        return geometryFactory.createPoint(coordinate);
    }
}
