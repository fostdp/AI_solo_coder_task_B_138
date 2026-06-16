package com.garden.icecrack.common.pattern;

import com.garden.icecrack.common.entity.Pavement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PavementPatternFactory {

    public static List<double[][]> generateSegments(Pavement.PavementStyle style,
                                                    double areaLength,
                                                    double areaWidth,
                                                    String patternJson) {
        switch (style) {
            case HERRINGBONE:
                return generateHerringbone(areaLength, areaWidth, patternJson);
            case BASKETWEAVE:
                return generateBasketweave(areaLength, areaWidth, patternJson);
            case PERMEABLE_BRICK:
                return generatePermeableBrick(areaLength, areaWidth, patternJson);
            case CUSTOM:
                return parseCustomSegments(patternJson);
            case ICE_CRACK:
            default:
                return generateIceCrackSegments(areaLength, areaWidth, patternJson);
        }
    }

    public static List<double[][]> generateHerringbone(double L, double W, String patternJson) {
        List<double[][]> segments = new ArrayList<>();
        long seed = 2048L;
        double spacing = 0.4;
        double angleDeg = 45.0;
        double irregularity = 0.15;
        if (patternJson != null && !patternJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<?, ?> cfg = om.readValue(patternJson, java.util.Map.class);
                if (cfg.get("seed") instanceof Number) seed = ((Number) cfg.get("seed")).longValue();
                if (cfg.get("spacing") instanceof Number) spacing = ((Number) cfg.get("spacing")).doubleValue();
                if (cfg.get("angle") instanceof Number) angleDeg = ((Number) cfg.get("angle")).doubleValue();
                if (cfg.get("irregularity") instanceof Number) irregularity = ((Number) cfg.get("irregularity")).doubleValue();
            } catch (Exception ignored) {}
        }
        Random rng = new Random(seed);
        double angle = Math.toRadians(angleDeg);
        double diagX = Math.cos(angle);
        double diagY = Math.sin(angle);
        double perpX = -diagY;
        double perpY = diagX;
        double extent = Math.max(L, W) * 1.5;

        for (double t = -extent; t < extent; t += spacing) {
            double ox = perpX * t;
            double oy = perpY * t;
            for (double s = -extent; s < extent; s += spacing * 2) {
                double sx1 = ox + diagX * s;
                double sy1 = oy + diagY * s;
                double sx2 = ox + diagX * (s + spacing);
                double sy2 = oy + diagY * (s + spacing);
                addSegmentInBounds(segments, sx1, sy1, sx2, sy2, L, W, irregularity, rng);
            }
            double altAngle = angle + Math.PI / 2.0;
            double adx = Math.cos(altAngle);
            double ady = Math.sin(altAngle);
            for (double s = -extent; s < extent; s += spacing * 2) {
                double sx1 = ox + adx * s + diagX * spacing;
                double sy1 = oy + ady * s + diagY * spacing;
                double sx2 = ox + adx * (s + spacing) + diagX * spacing;
                double sy2 = oy + ady * (s + spacing) + diagY * spacing;
                addSegmentInBounds(segments, sx1, sy1, sx2, sy2, L, W, irregularity, rng);
            }
        }
        return segments;
    }

    public static List<double[][]> generateBasketweave(double L, double W, String patternJson) {
        List<double[][]> segments = new ArrayList<>();
        long seed = 4096L;
        double tileW = 0.5;
        double tileH = 0.25;
        double irregularity = 0.1;
        if (patternJson != null && !patternJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<?, ?> cfg = om.readValue(patternJson, java.util.Map.class);
                if (cfg.get("seed") instanceof Number) seed = ((Number) cfg.get("seed")).longValue();
                if (cfg.get("tileWidth") instanceof Number) tileW = ((Number) cfg.get("tileWidth")).doubleValue();
                if (cfg.get("tileHeight") instanceof Number) tileH = ((Number) cfg.get("tileHeight")).doubleValue();
                if (cfg.get("irregularity") instanceof Number) irregularity = ((Number) cfg.get("irregularity")).doubleValue();
            } catch (Exception ignored) {}
        }
        Random rng = new Random(seed);
        double cellW = tileW + tileH;

        for (double y = 0; y < W; y += cellW) {
            for (double x = 0; x < L; x += cellW) {
                boolean horizontal = (((int) (x / cellW) + (int) (y / cellW)) % 2 == 0);
                if (horizontal) {
                    addSegmentInBounds(segments, x, y, x + tileW, y, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y + tileH, x + tileW, y + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y, x, y + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileW, y, x + tileW, y + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileW, y, x + tileW + tileH, y, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileW, y + tileH, x + tileW + tileH, y + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileW, y, x + tileW, y + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileW + tileH, y, x + tileW + tileH, y + tileH, L, W, irregularity, rng);
                } else {
                    addSegmentInBounds(segments, x, y, x, y + tileW, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileH, y, x + tileH, y + tileW, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y, x + tileH, y, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y + tileW, x + tileH, y + tileW, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y + tileW, x, y + tileW + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x + tileH, y + tileW, x + tileH, y + tileW + tileH, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y + tileW, x + tileH, y + tileW, L, W, irregularity, rng);
                    addSegmentInBounds(segments, x, y + tileW + tileH, x + tileH, y + tileW + tileH, L, W, irregularity, rng);
                }
            }
        }
        return segments;
    }

    public static List<double[][]> generatePermeableBrick(double L, double W, String patternJson) {
        List<double[][]> segments = new ArrayList<>();
        long seed = 8192L;
        double brickSize = 0.2;
        double gapWidth = 0.015;
        if (patternJson != null && !patternJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<?, ?> cfg = om.readValue(patternJson, java.util.Map.class);
                if (cfg.get("seed") instanceof Number) seed = ((Number) cfg.get("seed")).longValue();
                if (cfg.get("brickSize") instanceof Number) brickSize = ((Number) cfg.get("brickSize")).doubleValue();
                if (cfg.get("gapWidth") instanceof Number) gapWidth = ((Number) cfg.get("gapWidth")).doubleValue();
            } catch (Exception ignored) {}
        }
        Random rng = new Random(seed);
        double cell = brickSize + gapWidth;

        for (double y = 0; y < W; y += cell) {
            for (double x = 0; x < L; x += cell) {
                double jitterX = gapWidth * 0.2 * (rng.nextDouble() - 0.5);
                double jitterY = gapWidth * 0.2 * (rng.nextDouble() - 0.5);
                double bx = x + jitterX;
                double by = y + jitterY;
                addSegmentInBounds(segments, bx, by, bx + brickSize, by, L, W, 0.02, rng);
                addSegmentInBounds(segments, bx, by + brickSize, bx + brickSize, by + brickSize, L, W, 0.02, rng);
                addSegmentInBounds(segments, bx, by, bx, by + brickSize, L, W, 0.02, rng);
                addSegmentInBounds(segments, bx + brickSize, by, bx + brickSize, by + brickSize, L, W, 0.02, rng);
            }
        }
        int microCracks = (int) (L * W * 3);
        for (int i = 0; i < microCracks; i++) {
            double cx = rng.nextDouble() * L;
            double cy = rng.nextDouble() * W;
            double len = brickSize * (0.2 + rng.nextDouble() * 0.6);
            double ang = rng.nextDouble() * Math.PI;
            addSegmentInBounds(segments, cx, cy, cx + Math.cos(ang) * len, cy + Math.sin(ang) * len, L, W, 0.0, rng);
        }
        return segments;
    }

    private static List<double[][]> generateIceCrackSegments(double L, double W, String patternJson) {
        long seed = 42L;
        int targetSegments = 35;
        double irregularity = 0.7;
        if (patternJson != null && !patternJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<?, ?> cfg = om.readValue(patternJson, java.util.Map.class);
                if (cfg.get("seed") instanceof Number) seed = ((Number) cfg.get("seed")).longValue();
                if (cfg.get("segments") instanceof Number) targetSegments = ((Number) cfg.get("segments")).intValue();
                if (cfg.get("irregularity") instanceof Number) irregularity = ((Number) cfg.get("irregularity")).doubleValue();
            } catch (Exception ignored) {}
        }
        List<double[][]> segments = new ArrayList<>();
        Random random = new Random(seed);
        int numPoints = 15 + random.nextInt(10);
        double[][] points = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            points[i][0] = random.nextDouble();
            points[i][1] = random.nextDouble();
        }
        for (int i = 0; i < numPoints && segments.size() < targetSegments; i++) {
            double minDist1 = Double.MAX_VALUE, minDist2 = Double.MAX_VALUE, minDist3 = Double.MAX_VALUE;
            int nearest1 = -1, nearest2 = -1, nearest3 = -1;
            for (int j = 0; j < numPoints; j++) {
                if (i == j) continue;
                double ddx = points[i][0] - points[j][0];
                double ddy = points[i][1] - points[j][1];
                double dist = Math.sqrt(ddx * ddx + ddy * ddy);
                if (dist < minDist1) {
                    minDist3 = minDist2; nearest3 = nearest2;
                    minDist2 = minDist1; nearest2 = nearest1;
                    minDist1 = dist; nearest1 = j;
                } else if (dist < minDist2) {
                    minDist3 = minDist2; nearest3 = nearest2;
                    minDist2 = dist; nearest2 = j;
                } else if (dist < minDist3) {
                    minDist3 = dist; nearest3 = j;
                }
            }
            double irr = irregularity * 0.1;
            addCrackSegment(segments, points[i], points[nearest1], irr, random, L, W);
            if (nearest2 >= 0 && random.nextDouble() < 0.7 && segments.size() < targetSegments) {
                addCrackSegment(segments, points[i], points[nearest2], irr, random, L, W);
            }
            if (nearest3 >= 0 && random.nextDouble() < 0.4 && segments.size() < targetSegments) {
                addCrackSegment(segments, points[i], points[nearest3], irr, random, L, W);
            }
        }
        return segments;
    }

    private static void addCrackSegment(List<double[][]> segments, double[] pA, double[] pB,
                                         double irr, Random random, double L, double W) {
        double x1 = (pA[0] + irr * random.nextGaussian()) * L;
        double y1 = (pA[1] + irr * random.nextGaussian()) * W;
        double x2 = (pB[0] + irr * random.nextGaussian()) * L;
        double y2 = (pB[1] + irr * random.nextGaussian()) * W;
        if (random.nextDouble() < 0.35) {
            double tx = (pA[0] + pB[0]) / 2 + irr * random.nextGaussian();
            double ty = (pA[1] + pB[1]) / 2 + irr * random.nextGaussian();
            double mx = tx * L, my = ty * W;
            segments.add(new double[][]{{x1, y1}, {mx, my}});
            segments.add(new double[][]{{mx, my}, {x2, y2}});
        } else {
            segments.add(new double[][]{{x1, y1}, {x2, y2}});
        }
    }

    private static void addSegmentInBounds(List<double[][]> segments,
                                            double x1, double y1, double x2, double y2,
                                            double L, double W, double irregularity, Random rng) {
        if (x1 < 0 || x1 > L || y1 < 0 || y1 > W || x2 < 0 || x2 > L || y2 < 0 || y2 > W) {
            return;
        }
        double j1 = irregularity * (rng.nextDouble() - 0.5);
        double j2 = irregularity * (rng.nextDouble() - 0.5);
        double j3 = irregularity * (rng.nextDouble() - 0.5);
        double j4 = irregularity * (rng.nextDouble() - 0.5);
        segments.add(new double[][]{
                {Math.max(0, Math.min(L, x1 + j1)), Math.max(0, Math.min(W, y1 + j2))},
                {Math.max(0, Math.min(L, x2 + j3)), Math.max(0, Math.min(W, y2 + j4))}
        });
    }

    private static List<double[][]> parseCustomSegments(String patternJson) {
        List<double[][]> segments = new ArrayList<>();
        if (patternJson == null || patternJson.isBlank()) return segments;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Object raw = om.readValue(patternJson, Object.class);
            if (raw instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item instanceof List<?> coords) {
                        double[][] segArr = new double[2][2];
                        if (coords.get(0) instanceof List<?> p0 && coords.get(1) instanceof List<?> p1) {
                            segArr[0][0] = ((Number) p0.get(0)).doubleValue();
                            segArr[0][1] = ((Number) p0.get(1)).doubleValue();
                            segArr[1][0] = ((Number) p1.get(0)).doubleValue();
                            segArr[1][1] = ((Number) p1.get(1)).doubleValue();
                            segments.add(segArr);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return segments;
    }
}
