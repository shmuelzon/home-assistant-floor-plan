// Copyright (c) 2025, Ferry Cools
// All rights reserved.

// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:

// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// 3. Neither the name of the copyright holder nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.

// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.shmuelzon.HomeAssistantFloorPlan;

import javax.vecmath.Point2d;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class for generating predefined geometric patterns of points which represent the center position of a
 * circle.
 */
public class CircleCenterPositioner {
    private static final double SQRT_3 = Math.sqrt(3);

    /**
     * Generates a predefined geometric pattern of points which represent the center position of a circle.
     * The arrangements are centered at the given 'center' point.
     * The y-axis is inverted after the initial arrangement calculations.
     *
     * @param circleCount    The number of circles to arrange (1-7 inclusive).
     * @param circleDiameter The diameter of each circle.
     * @param center         The center point for the arrangement.
     * @param margin         The margin (gap) between the edges of the circles.
     * @return A list of Point2d objects representing the center positions of the arranged circles.
     * @throws IllegalArgumentException If the number of circles is not between 1 and 7 (inclusive), the diameter is not
     *                                  positive, or the margin is negative.
     */
    public static List<Point2d> generatePositions(int circleCount, double circleDiameter, Point2d center, double margin) {
        if (circleCount < 1 || circleCount > 7 || circleDiameter <= 0 || margin < 0) {
            throw new IllegalArgumentException(
                "Value of circleCount must be between 1 and 7 (inclusive), diameter must be positive, and margin must be non-negative."
            );
        }

        double effectiveDiameter = circleDiameter + margin;

        List<Point2d> circlePositions = new ArrayList<>();

        switch (circleCount) {
            case 1:
                circlePositions.add(new Point2d(0, 0));
                break;
            case 2:
                circlePositions.addAll(
                    Arrays.asList(new Point2d(-effectiveDiameter / 2, 0), new Point2d(effectiveDiameter / 2, 0))
                );
                break;
            case 3:
                circlePositions.addAll(createTriangle3(effectiveDiameter));
                break;
            case 4:
                circlePositions.addAll(createSquare(effectiveDiameter));
                break;
            case 5:
                circlePositions.addAll(createTrapezoid5Isosceles(effectiveDiameter));
                break;
            case 6:
                circlePositions.addAll(createTriangle6(effectiveDiameter));
                break;
            case 7:
                circlePositions.addAll(createCentralPlusSurrounding(effectiveDiameter, circleCount));
                break;
            default:
                throw new IllegalStateException("Unexpected number of circles: " + circleCount);
        }

        for (Point2d position : circlePositions) {
            position.y = -position.y;
            position.add(center);
        }

        return circlePositions;
    }

    /**
     * Creates a triangular arrangement of three circles.
     *
     * @param diameter The diameter of each circle.
     * @return A list of Point2d objects representing the center positions of the circles.
     */
    private static List<Point2d> createTriangle3(double diameter) {
        double height = (diameter * SQRT_3) / 2;

        return Arrays.asList(
            new Point2d(0, -height / 2),
            new Point2d(-diameter / 2, height / 2),
            new Point2d(diameter / 2, height / 2)
        );
    }

    /**
     * Creates a triangular arrangement of six circles.
     *
     * @param diameter The diameter of each circle.
     * @return A list of Point2d objects representing the center positions of the circles.
     */
    private static List<Point2d> createTriangle6(double diameter) {
        double height = (diameter * SQRT_3) / 2;

        return Arrays.asList(
            new Point2d(-diameter, -height),
            new Point2d(0, -height),
            new Point2d(diameter, -height),
            new Point2d(-diameter / 2, 0),
            new Point2d(diameter / 2, 0),
            new Point2d(0, height)
        );
    }

    /**
     * Creates a square arrangement of four circles.
     *
     * @param diameter The diameter of each circle.
     * @return A list of Point2d objects representing the center positions of the circles.
     */
    private static List<Point2d> createSquare(double diameter) {
        return Arrays.asList(
            new Point2d(-diameter / 2, -diameter / 2),
            new Point2d(diameter / 2, -diameter / 2),
            new Point2d(diameter / 2, diameter / 2),
            new Point2d(-diameter / 2, diameter / 2)
        );
    }

    /**
     * Creates an isosceles trapezoid arrangement of 5 circles.
     *
     * @param diameter The diameter of each circle.
     * @return A list of Point2d objects representing the center positions of the circles.
     */
    private static List<Point2d> createTrapezoid5Isosceles(double diameter) {
        double verticalOffset = (diameter / 2) * SQRT_3;

        return Arrays.asList(
            new Point2d(-diameter, 0),
            new Point2d(0, 0),
            new Point2d(diameter, 0),
            new Point2d(-diameter / 2, verticalOffset),
            new Point2d(diameter / 2, verticalOffset)
        );
    }

    /**
     * Creates an arrangement of circles with one circle in the center and the others surrounding it.
     *
     * @param diameter   The diameter of each circle.
     * @param circleCount The number of circles.
     * @return A list of Point2d objects representing the center positions of the circles.
     */
    private static List<Point2d> createCentralPlusSurrounding(double diameter, int circleCount) {
        if (circleCount > 7) {
            System.err.println(
                "Warning: Number of circles requested ("
                + circleCount
                + ") is greater than the maximum supported (7). The arrangement might be unexpected."
            );
        }

        List<Point2d> points = new ArrayList<>();
        points.add(new Point2d(0, 0));

        for (int i = 0; i < circleCount - 1; i++) {
            double angle = 2 * Math.PI * i / (circleCount - 1);
            points.add(new Point2d(diameter * Math.cos(angle), diameter * Math.sin(angle)));
        }

        return points;
    }

    public static void main(String[] args) {
        Point2d center = new Point2d(5, 10);
        double diameter = 5;
        double margin = 1;

        for (int i = 1; i <= 7; i++) {
            List<Point2d> positions = generatePositions(i, diameter, center, margin);
            System.out.println("Having a center position at " + center.x + ", " + center.y + "...");
            System.out.println("The center positions for " + i + " distributed circles are:");
            for (int j = 0; j < positions.size(); j++) {
                System.out.println("Circle " + (j + 1) + ": " + positions.get(j));
            }
            System.out.println();
        }
    }
}