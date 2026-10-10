package com.shmuelzon.HomeAssistantFloorPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedGeometryStripArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.IndexedTriangleFanArray;
import javax.media.j3d.IndexedTriangleStripArray;
import javax.media.j3d.Link;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.TriangleFanArray;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import com.eteks.sweethome3d.j3d.Ground3D;
import com.eteks.sweethome3d.j3d.Object3DBranchFactory;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeDoorOrWindow;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.LightSource;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.Wall;


/* Detects which entities are seen in a render made with a given camera, i.e., entities which are within the camera's
 * field of view and aren't hidden behind walls, floors, ceilings or (the opaque parts of) doors and windows. Other
 * furniture isn't considered as an obstacle. Lights that aren't seen but do light up surfaces that are seen, e.g., a
 * ceiling light above the camera, are detected as well since they affect the render.
 *
 * Each entity's pieces are sampled with a grid of points inside their bounding box and a piece is seen if any of these
 * points is inside the field of view and the segment from the camera to it doesn't cross an obstacle. The obstacles are
 * the triangles of the same 3D objects the photo renderers export, so wall heights, sloping walls, round walls, wall
 * openings, stairs openings and so on are taken into account. Like the renderers, a triangle is only an obstacle when
 * looked at from its front side, e.g., ceilings are seen through from above. Only the levels which are visible when
 * creating this object are considered, so the levels' visibility needs to be set as it will be when rendering.
 *
 * Without a home, there are no obstacles and only the camera's field of view is taken into account.
 *
 * Coordinates are kept in the 3D (Java3D) space, i.e., (x, elevation, y) of the plan */
public class CameraVisibility {
    public enum Status {VISIBLE, ILLUMINATES_VIEW, OCCLUDED, OUT_OF_VIEW}
    public enum SampleStatus {VISIBLE, OCCLUDED, OUT_OF_VIEW}

    /* Fractions of a piece's bounding box, along each of its axes, where it's sampled */
    private static final double[] SAMPLE_FRACTIONS = {0.1, 0.5, 0.9};
    /* Columns of the grid of rays used to find the surfaces seen by the camera, rows are set by the aspect ratio */
    private static final int VIEW_RAYS_COLUMNS = 96;
    /* The minimal fraction of the view a light needs to light up to be considered as affecting the render */
    private static final double MIN_ILLUMINATED_VIEW_FRACTION = 0.01;
    /* Distance in cm by which rays leaving a surface are moved away from it, to avoid hitting the surface itself */
    private static final double SURFACE_OFFSET = 0.5;
    /* Margin in cm around the home of the ground taken into account */
    private static final float GROUND_MARGIN = 2000;

    public static class Sample {
        private final Point3d location;
        private final Point2d position;
        private final SampleStatus status;

        private Sample(Point3d location, Point2d position, SampleStatus status) {
            this.location = location;
            this.position = position;
            this.status = status;
        }

        /* The location in the 3D space, i.e., (x, elevation, y) of the plan */
        public Point3d getLocation() {
            return new Point3d(location);
        }

        /* The position in the render in percents, or null if it's behind the camera */
        public Point2d getPosition() {
            return position != null ? new Point2d(position) : null;
        }

        public SampleStatus getStatus() {
            return status;
        }
    }

    public static class Result {
        private final Status status;
        private final List<Sample> samples;
        private final int illuminatedViewRays;
        private final int viewRays;
        private final boolean isIconInRender;
        private final Point2d position;

        private Result(Status status, List<Sample> samples, int illuminatedViewRays, int viewRays, boolean isIconInRender, Point2d position) {
            this.status = status;
            this.samples = Collections.unmodifiableList(samples);
            this.illuminatedViewRays = illuminatedViewRays;
            this.viewRays = viewRays;
            this.isIconInRender = isIconInRender;
            this.position = position;
        }

        public Status getStatus() {
            return status;
        }

        /* Whether the entity affects the render, i.e., it's seen or, for lights, it lights up what's seen */
        public boolean isRelevant() {
            return status == Status.VISIBLE || status == Status.ILLUMINATES_VIEW;
        }

        public List<Sample> getSamples() {
            return samples;
        }

        public int getNumberOfVisibleSamples() {
            return (int)samples.stream().filter(sample -> sample.getStatus() == SampleStatus.VISIBLE).count();
        }

        /* The fraction of the view lit by the entity, only computed for lights that aren't seen */
        public double getIlluminatedViewFraction() {
            return viewRays > 0 ? (double)illuminatedViewRays / viewRays : 0;
        }

        /* Whether the entity's state icon should be displayed by default: when the entity is seen or, for lights that
         * only light up what's seen, when one of their light sources is in front of the camera and not hidden, just
         * outside of the render, e.g., a ceiling light right above the render. Lights behind the camera or hidden still
         * affect the render, but their icon isn't displayed by default */
        public boolean isIconInRender() {
            return isIconInRender;
        }

        /* Where to place the entity's state icon, in percents, when the default position, i.e., the average projection
         * of its pieces' centers, is outside of the render or behind the camera. It's then the center of the parts of
         * the entity that are seen or, for lights with their light sources just outside of the render, the projection
         * of these light sources. Otherwise, it's in the direction of the entity, outside of the render, for when the
         * icon is displayed anyway. Positions outside of the render need to be moved inside by the icon's size. Returns
         * null if the default position should be used */
        public Point2d getPosition() {
            return position != null ? new Point2d(position) : null;
        }
    }

    /* A surface point seen by the camera, along with the surface's normal facing the camera */
    private static class ViewPoint {
        final double[] location;
        final double[] normal;
        final Point2d position;

        ViewPoint(double[] location, double[] normal, Point2d position) {
            this.location = location;
            this.normal = normal;
            this.position = position;
        }
    }

    private final Camera camera;
    private final Obstacles obstacles = new Obstacles();
    private final Map<HomePieceOfFurniture, Integer> pieceOwners = new HashMap<>();
    private double aspectRatio;
    private double[] eye;
    private double[] forward;
    private double[] right;
    private double[] up;
    private double tanHalfHorizontalFieldOfView;
    private double tanHalfVerticalFieldOfView;
    private List<ViewPoint> viewPoints;
    private int viewRays;

    public CameraVisibility(Home home, Camera camera, double aspectRatio) {
        this.camera = camera;
        buildObstacles(home);
        setAspectRatio(aspectRatio);
    }

    /* Only the camera's field of view is taken into account, nothing hides entities and lights don't light up anything */
    public CameraVisibility(Camera camera, double aspectRatio) {
        this.camera = camera;
        obstacles.build();
        setAspectRatio(aspectRatio);
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    /* The render's width / height, it changes the vertical field of view */
    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
        buildCameraFrame();
        viewPoints = null;
    }

    public int getNumberOfObstacleTriangles() {
        return obstacles.size();
    }

    public Result getVisibility(Entity entity) {
        Set<Integer> ignoredOwners = new HashSet<>();
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture())
            if (pieceOwners.containsKey(piece))
                ignoredOwners.add(pieceOwners.get(piece));

        List<Sample> samples = new ArrayList<>();
        List<Point2d> piecesPositions = new ArrayList<>();
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture()) {
            Sample center = null;
            List<Point2d> visiblePositions = new ArrayList<>();
            for (double[] location : getSampleLocations(piece)) {
                Sample sample = getSample(location, ignoredOwners);
                samples.add(sample);
                if (sample.getStatus() == SampleStatus.VISIBLE)
                    visiblePositions.add(sample.getPosition());
                if (isCenter(location, piece))
                    center = sample;
            }
            if (center != null && center.getStatus() == SampleStatus.VISIBLE)
                piecesPositions.add(center.getPosition());
            else if (!visiblePositions.isEmpty())
                piecesPositions.add(average(visiblePositions));
        }

        boolean isDefaultPositionInView = isDefaultPositionInView(entity);
        if (!piecesPositions.isEmpty())
            return new Result(Status.VISIBLE, samples, 0, 0, true, isDefaultPositionInView ? null : average(piecesPositions));

        Status status = samples.stream().anyMatch(sample -> sample.getStatus() == SampleStatus.OCCLUDED) ? Status.OCCLUDED : Status.OUT_OF_VIEW;
        Point2d direction = isDefaultPositionInView ? null : getDirectionPosition(entity);
        if (!entity.getIsLight())
            return new Result(status, samples, 0, 0, false, direction);

        List<Point2d> illuminatedPositions = getIlluminatedViewPositions(entity, ignoredOwners);
        if (illuminatedPositions.size() < Math.max(1, MIN_ILLUMINATED_VIEW_FRACTION * viewRays))
            return new Result(status, samples, illuminatedPositions.size(), viewRays, false, direction);

        List<Point2d> lightSourcesPositions = getUnobstructedLightSourcesPositions(entity, ignoredOwners);
        if (lightSourcesPositions.isEmpty())
            return new Result(Status.ILLUMINATES_VIEW, samples, illuminatedPositions.size(), viewRays, false, direction);
        return new Result(Status.ILLUMINATES_VIEW, samples, illuminatedPositions.size(), viewRays, true,
            isDefaultPositionInView ? null : average(lightSourcesPositions));
    }

    /* The default position of an entity's state icon is the average projection of its pieces' centers */
    private boolean isDefaultPositionInView(Entity entity) {
        List<Point2d> positions = new ArrayList<>();
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture()) {
            Point2d position = project(getPieceLocation(piece, 0.5, 0.5, 0.5));
            if (position == null)
                return false;
            positions.add(position);
        }
        return isInView(average(positions));
    }

    /* The positions, possibly outside of the render, of the light sources that are in front of the camera and that
     * aren't hidden, i.e., that would be seen if the render was larger */
    private List<Point2d> getUnobstructedLightSourcesPositions(Entity entity, Set<Integer> ignoredOwners) {
        List<Point2d> positions = new ArrayList<>();
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture()) {
            if (!(piece instanceof HomeLight))
                continue;
            for (double[] location : getLightSourceLocations((HomeLight)piece)) {
                Point2d position = project(location);
                double[] direction = subtract(location, eye);
                double distance = length(direction);
                if (position != null && distance > 0 && !obstacles.isOccluded(eye, scale(direction, 1 / distance), distance, ignoredOwners))
                    positions.add(position);
            }
        }
        return positions;
    }

    /* A position outside of the render in the direction of the entity's pieces, even when they're behind the camera */
    private Point2d getDirectionPosition(Entity entity) {
        double[] center = new double[3];
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture())
            center = add(center, getPieceLocation(piece, 0.5, 0.5, 0.5));
        center = scale(center, 1.0 / entity.getPiecesOfFurniture().size());

        Point2d position = project(center);
        if (position != null)
            return position;
        double[] direction = subtract(center, eye);
        double horizontal = dot(direction, right);
        double vertical = dot(direction, up);
        double length = Math.hypot(horizontal, vertical);
        if (length == 0)
            return new Point2d(50, 50);
        return new Point2d(50 + 1000 * horizontal / length, 50 - 1000 * vertical / length);
    }

    /* The position in the render, in percents, of the given 3D location or null if it's behind the camera */
    public Point2d project(Point3d location) {
        return project(new double[] {location.x, location.y, location.z});
    }

    private void buildCameraFrame() {
        double yaw = camera.getYaw();
        double pitch = camera.getLens() == Camera.Lens.SPHERICAL ? 0 : camera.getPitch();

        /* Same as the photo renderers, the camera looks at (x - sin(yaw) * cos(pitch), z - sin(pitch), y + cos(yaw) * cos(pitch)) */
        eye = new double[] {camera.getX(), camera.getZ(), camera.getY()};
        forward = new double[] {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
        right = new double[] {-Math.cos(yaw), 0, -Math.sin(yaw)};
        up = cross(right, forward);
        /* The field of view is horizontal */
        tanHalfHorizontalFieldOfView = Math.tan(camera.getFieldOfView() / 2);
        tanHalfVerticalFieldOfView = tanHalfHorizontalFieldOfView / aspectRatio;
    }

    private Point2d project(double[] location) {
        double[] direction = subtract(location, eye);
        double depth = dot(direction, forward);
        double horizontal = dot(direction, right);
        double vertical = dot(direction, up);

        switch (camera.getLens()) {
            case SPHERICAL: {
                /* Equirectangular projection of the whole sphere around the camera */
                double longitude = Math.atan2(horizontal, depth);
                double latitude = Math.atan2(vertical, Math.hypot(horizontal, depth));
                return new Point2d((0.5 + longitude / (2 * Math.PI)) * 100, (0.5 - latitude / Math.PI) * 100);
            }
            case FISHEYE: {
                /* Equidistant projection of the half sphere in front of the camera on a circle fitting the render's height */
                double angle = Math.atan2(Math.hypot(horizontal, vertical), depth);
                if (angle > Math.PI / 2)
                    return null;
                double radius = angle / (Math.PI / 2) * 0.5;
                double planarDistance = Math.hypot(horizontal, vertical);
                double x = planarDistance > 0 ? horizontal / planarDistance * radius / aspectRatio : 0;
                double y = planarDistance > 0 ? vertical / planarDistance * radius : 0;
                return new Point2d((0.5 + x) * 100, (0.5 - y) * 100);
            }
            default:
                if (depth <= 1E-3)
                    return null;
                return new Point2d(
                    (0.5 + 0.5 * horizontal / depth / tanHalfHorizontalFieldOfView) * 100,
                    (0.5 - 0.5 * vertical / depth / tanHalfVerticalFieldOfView) * 100);
        }
    }

    private static boolean isInView(Point2d position) {
        return position != null && position.x >= 0 && position.x <= 100 && position.y >= 0 && position.y <= 100;
    }

    private Sample getSample(double[] location, Set<Integer> ignoredOwners) {
        Point2d position = project(location);
        if (!isInView(position))
            return new Sample(toPoint(location), position, SampleStatus.OUT_OF_VIEW);

        double[] direction = subtract(location, eye);
        double distance = length(direction);
        if (distance > 0 && obstacles.isOccluded(eye, scale(direction, 1 / distance), distance, ignoredOwners))
            return new Sample(toPoint(location), position, SampleStatus.OCCLUDED);
        return new Sample(toPoint(location), position, SampleStatus.VISIBLE);
    }

    /* A grid of points inside the piece's bounding box in the plan, which includes its pitch and roll rotations. Doors
     * and windows are also sampled in the middle of their wall opening, as their box may be mostly on one side of the
     * wall, e.g., with the door's swing, while the opening itself is seen from the other side */
    private static List<double[]> getSampleLocations(HomePieceOfFurniture piece) {
        List<double[]> locations = new ArrayList<>();
        for (double widthFraction : SAMPLE_FRACTIONS)
            for (double depthFraction : SAMPLE_FRACTIONS)
                for (double heightFraction : SAMPLE_FRACTIONS)
                    locations.add(getPieceLocation(piece, widthFraction, depthFraction, heightFraction));

        if (piece instanceof HomeDoorOrWindow) {
            HomeDoorOrWindow doorOrWindow = (HomeDoorOrWindow)piece;
            double depthFraction = 1 - (doorOrWindow.getWallDistance() + doorOrWindow.getWallThickness() / 2);
            for (double widthFraction : SAMPLE_FRACTIONS)
                for (double heightFraction : SAMPLE_FRACTIONS)
                    locations.add(getPieceLocation(piece,
                        doorOrWindow.getWallLeft() + doorOrWindow.getWallWidth() * widthFraction, depthFraction,
                        1 - (doorOrWindow.getWallTop() + doorOrWindow.getWallHeight() * heightFraction)));
        }
        return locations;
    }

    private static boolean isCenter(double[] location, HomePieceOfFurniture piece) {
        return Arrays.equals(location, getPieceLocation(piece, 0.5, 0.5, 0.5));
    }

    /* A location inside the piece's bounding box where the fractions are relative to its width (left to right), depth
     * (front to back) and height (bottom to top), similar to the location of a light source */
    private static double[] getPieceLocation(HomePieceOfFurniture piece, double widthFraction, double depthFraction, double heightFraction) {
        /* The points are the back left, back right, front right and front left corners */
        float[][] points = piece.getPoints();
        if (piece.isModelMirrored())
            widthFraction = 1 - widthFraction;
        double frontX = points[3][0] + (points[2][0] - points[3][0]) * widthFraction;
        double frontY = points[3][1] + (points[2][1] - points[3][1]) * widthFraction;
        double backX = points[0][0] + (points[1][0] - points[0][0]) * widthFraction;
        double backY = points[0][1] + (points[1][1] - points[0][1]) * widthFraction;
        return new double[] {
            frontX + (backX - frontX) * depthFraction,
            piece.getGroundElevation() + piece.getHeightInPlan() * heightFraction,
            frontY + (backY - frontY) * depthFraction};
    }

    private List<double[]> getLightSourceLocations(HomeLight light) {
        LightSource[] lightSources = light.getLightSources();
        if (lightSources == null || lightSources.length == 0)
            return Arrays.asList(getPieceLocation(light, 0.5, 0.5, 0.5));

        List<double[]> locations = new ArrayList<>();
        for (LightSource lightSource : lightSources)
            locations.add(getPieceLocation(light, lightSource.getX(), lightSource.getY(), lightSource.getZ()));
        return locations;
    }

    /* The positions in the render of the surfaces seen by the camera that are directly lit by the given light */
    private List<Point2d> getIlluminatedViewPositions(Entity entity, Set<Integer> ignoredOwners) {
        List<double[]> lightSourceLocations = new ArrayList<>();
        for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture())
            if (piece instanceof HomeLight && ((HomeLight)piece).getPower() > 0)
                lightSourceLocations.addAll(getLightSourceLocations((HomeLight)piece));

        List<Point2d> positions = new ArrayList<>();
        for (ViewPoint viewPoint : getViewPoints()) {
            for (double[] lightSourceLocation : lightSourceLocations) {
                double[] direction = subtract(lightSourceLocation, viewPoint.location);
                double distance = length(direction);
                if (distance <= SURFACE_OFFSET || dot(direction, viewPoint.normal) <= 0)
                    continue;
                direction = scale(direction, 1 / distance);
                double[] origin = add(viewPoint.location, scale(viewPoint.normal, SURFACE_OFFSET));
                if (!obstacles.isOccluded(origin, direction, distance - SURFACE_OFFSET, ignoredOwners)) {
                    positions.add(viewPoint.position);
                    break;
                }
            }
        }
        return positions;
    }

    /* The surfaces seen by the camera are found by casting a grid of rays through the render */
    private List<ViewPoint> getViewPoints() {
        if (viewPoints != null)
            return viewPoints;

        int columns = VIEW_RAYS_COLUMNS;
        int rows = Math.max(1, (int)Math.round(columns / aspectRatio));
        viewPoints = new ArrayList<>();
        viewRays = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Point2d position = new Point2d((column + 0.5) * 100.0 / columns, (row + 0.5) * 100.0 / rows);
                double[] direction = getViewDirection(position);
                if (direction == null)
                    continue;
                viewRays++;
                Obstacles.Hit hit = obstacles.getClosestHit(eye, direction, Double.POSITIVE_INFINITY);
                if (hit == null)
                    continue;
                double[] normal = hit.normal;
                if (dot(normal, direction) > 0)
                    normal = scale(normal, -1);
                viewPoints.add(new ViewPoint(add(eye, scale(direction, hit.distance)), normal, position));
            }
        }
        return viewPoints;
    }

    /* The direction of the ray going through the given position of the render, the inverse of project() */
    private double[] getViewDirection(Point2d position) {
        double x = position.x / 100 - 0.5;
        double y = 0.5 - position.y / 100;
        double[] direction;

        switch (camera.getLens()) {
            case SPHERICAL: {
                double longitude = x * 2 * Math.PI;
                double latitude = y * Math.PI;
                direction = add(add(
                    scale(forward, Math.cos(latitude) * Math.cos(longitude)),
                    scale(right, Math.cos(latitude) * Math.sin(longitude))),
                    scale(up, Math.sin(latitude)));
                break;
            }
            case FISHEYE: {
                double planarX = x * aspectRatio;
                double radius = Math.hypot(planarX, y);
                if (radius > 0.5)
                    return null;
                double angle = radius / 0.5 * (Math.PI / 2);
                double horizontal = radius > 0 ? planarX / radius * Math.sin(angle) : 0;
                double vertical = radius > 0 ? y / radius * Math.sin(angle) : 0;
                direction = add(add(scale(forward, Math.cos(angle)), scale(right, horizontal)), scale(up, vertical));
                break;
            }
            default:
                direction = add(add(forward,
                    scale(right, 2 * x * tanHalfHorizontalFieldOfView)),
                    scale(up, 2 * y * tanHalfVerticalFieldOfView));
        }
        return scale(direction, 1 / length(direction));
    }

    /* The obstacles are the walls, rooms, ground and doors or windows of the visible levels, as exported by the renderers */
    private void buildObstacles(Home home) {
        Object3DBranchFactory factory = new Object3DBranchFactory();
        int owner = 0;

        for (Wall wall : home.getWalls())
            if (isOnVisibleLevel(wall.getLevel()))
                addObstacle((Node)factory.createObject3D(home, wall, true), owner++, false);
        for (Room room : home.getRooms())
            if (isOnVisibleLevel(room.getLevel()))
                addObstacle((Node)factory.createObject3D(home, room, true), owner++, false);
        for (HomePieceOfFurniture piece : getDoorsAndWindows(home.getFurniture())) {
            pieceOwners.put(piece, owner);
            addObstacle((Node)factory.createObject3D(home, piece, true), owner++, true);
        }

        float[] bounds = getHomeBounds(home);
        if (bounds != null)
            addObstacle(new Ground3D(home, bounds[0] - GROUND_MARGIN, bounds[1] - GROUND_MARGIN,
                bounds[2] - bounds[0] + 2 * GROUND_MARGIN, bounds[3] - bounds[1] + 2 * GROUND_MARGIN, false), owner++, false);

        obstacles.build();
    }

    private static boolean isOnVisibleLevel(Level level) {
        return level == null || level.isViewableAndVisible();
    }

    private static List<HomePieceOfFurniture> getDoorsAndWindows(List<HomePieceOfFurniture> furniture) {
        List<HomePieceOfFurniture> doorsAndWindows = new ArrayList<>();
        for (HomePieceOfFurniture piece : furniture) {
            if (!piece.isVisible() || !isOnVisibleLevel(piece.getLevel()))
                continue;
            if (piece instanceof HomeFurnitureGroup)
                doorsAndWindows.addAll(getDoorsAndWindows(((HomeFurnitureGroup)piece).getFurniture()));
            else if (piece instanceof HomeDoorOrWindow)
                doorsAndWindows.add(piece);
        }
        return doorsAndWindows;
    }

    /* Returns {minX, minY, maxX, maxY} of the home's items in the plan */
    private static float[] getHomeBounds(Home home) {
        List<float[][]> itemsPoints = new ArrayList<>();
        for (Wall wall : home.getWalls())
            itemsPoints.add(wall.getPoints());
        for (Room room : home.getRooms())
            itemsPoints.add(room.getPoints());
        for (HomePieceOfFurniture piece : home.getFurniture())
            itemsPoints.add(piece.getPoints());

        float[] bounds = null;
        for (float[][] points : itemsPoints) {
            for (float[] point : points) {
                if (bounds == null)
                    bounds = new float[] {point[0], point[1], point[0], point[1]};
                bounds[0] = Math.min(bounds[0], point[0]);
                bounds[1] = Math.min(bounds[1], point[1]);
                bounds[2] = Math.max(bounds[2], point[0]);
                bounds[3] = Math.max(bounds[3], point[1]);
            }
        }
        return bounds;
    }

    private void addObstacle(Node node, int owner, boolean ignoreTranslucentShapes) {
        if (node != null)
            addObstacle(node, new Transform3D(), owner, ignoreTranslucentShapes);
    }

    private void addObstacle(Node node, Transform3D parentTransform, int owner, boolean ignoreTranslucentShapes) {
        if (node instanceof Group) {
            Transform3D transform = parentTransform;
            if (node instanceof TransformGroup) {
                Transform3D nodeTransform = new Transform3D();
                ((TransformGroup)node).getTransform(nodeTransform);
                transform = new Transform3D(parentTransform);
                transform.mul(nodeTransform);
            }
            Enumeration<?> children = ((Group)node).getAllChildren();
            while (children.hasMoreElements())
                addObstacle((Node)children.nextElement(), transform, owner, ignoreTranslucentShapes);
        } else if (node instanceof Link) {
            addObstacle(((Link)node).getSharedGroup(), parentTransform, owner, ignoreTranslucentShapes);
        } else if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D)node;
            Appearance appearance = shape.getAppearance();
            int cullFace = PolygonAttributes.CULL_BACK;
            if (appearance != null) {
                /* Same as the renderers, invisible and fully transparent shapes are ignored. Translucent shapes, e.g.,
                 * glass, are ignored as well since they let light through */
                RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
                if (renderingAttributes != null && !renderingAttributes.getVisible())
                    return;
                TransparencyAttributes transparencyAttributes = appearance.getTransparencyAttributes();
                float transparency = transparencyAttributes != null ? transparencyAttributes.getTransparency() : 0;
                if (transparency >= 1 || (ignoreTranslucentShapes && transparency > 0))
                    return;
                PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
                if (polygonAttributes != null) {
                    if (polygonAttributes.getPolygonMode() != PolygonAttributes.POLYGON_FILL)
                        return;
                    cullFace = polygonAttributes.getCullFace();
                }
            }
            for (int i = 0; i < shape.numGeometries(); i++)
                addGeometry(shape.getGeometry(i), parentTransform, owner, cullFace);
        }
    }

    private void addGeometry(Geometry geometry, Transform3D transform, int owner, int cullFace) {
        if (!(geometry instanceof GeometryArray))
            return;

        GeometryArray geometryArray = (GeometryArray)geometry;
        double[] vertices = getVertices(geometryArray, transform);
        for (int[] triangle : getTriangles(geometryArray)) {
            int first = triangle[0], second = triangle[1], third = triangle[2];
            if (cullFace == PolygonAttributes.CULL_FRONT) {
                second = triangle[2];
                third = triangle[1];
            }
            obstacles.add(
                Arrays.copyOfRange(vertices, first * 3, first * 3 + 3),
                Arrays.copyOfRange(vertices, second * 3, second * 3 + 3),
                Arrays.copyOfRange(vertices, third * 3, third * 3 + 3),
                cullFace == PolygonAttributes.CULL_NONE, owner);
        }
    }

    private static double[] getVertices(GeometryArray geometryArray, Transform3D transform) {
        int vertexCount = geometryArray.getVertexCount();
        int format = geometryArray.getVertexFormat();
        double[] vertices = new double[vertexCount * 3];
        Point3d vertex = new Point3d();
        float[] interleavedVertices = null;
        float[] coordinates = null;
        int vertexSize = 3;

        if ((format & GeometryArray.BY_REFERENCE) != 0) {
            if ((format & GeometryArray.INTERLEAVED) != 0) {
                interleavedVertices = geometryArray.getInterleavedVertices();
                vertexSize = interleavedVertices.length / vertexCount;
            } else
                coordinates = geometryArray.getCoordRefFloat();
        }

        for (int i = 0; i < vertexCount; i++) {
            if (interleavedVertices != null) {
                int offset = i * vertexSize + vertexSize - 3;
                vertex.set(interleavedVertices[offset], interleavedVertices[offset + 1], interleavedVertices[offset + 2]);
            } else if (coordinates != null)
                vertex.set(coordinates[i * 3], coordinates[i * 3 + 1], coordinates[i * 3 + 2]);
            else
                geometryArray.getCoordinate(i, vertex);
            transform.transform(vertex);
            vertices[i * 3] = vertex.x;
            vertices[i * 3 + 1] = vertex.y;
            vertices[i * 3 + 2] = vertex.z;
        }
        return vertices;
    }

    /* The vertices indices of each triangle, lines and points are ignored */
    private static List<int[]> getTriangles(GeometryArray geometryArray) {
        List<int[]> triangles = new ArrayList<>();

        if (geometryArray instanceof IndexedGeometryArray) {
            IndexedGeometryArray indexed = (IndexedGeometryArray)geometryArray;
            int[] indices = new int[indexed.getIndexCount()];
            for (int i = 0; i < indices.length; i++)
                indices[i] = indexed.getCoordinateIndex(i);
            if (geometryArray instanceof IndexedTriangleArray)
                addTriangles(triangles, indices, 3);
            else if (geometryArray instanceof IndexedQuadArray)
                addTriangles(triangles, indices, 4);
            else if (geometryArray instanceof IndexedTriangleStripArray || geometryArray instanceof IndexedTriangleFanArray) {
                int[] stripCounts = new int[((IndexedGeometryStripArray)geometryArray).getNumStrips()];
                ((IndexedGeometryStripArray)geometryArray).getStripIndexCounts(stripCounts);
                addStripTriangles(triangles, indices, stripCounts, geometryArray instanceof IndexedTriangleFanArray);
            }
        } else {
            int[] indices = new int[geometryArray.getVertexCount()];
            for (int i = 0; i < indices.length; i++)
                indices[i] = i;
            if (geometryArray instanceof TriangleArray)
                addTriangles(triangles, indices, 3);
            else if (geometryArray instanceof QuadArray)
                addTriangles(triangles, indices, 4);
            else if (geometryArray instanceof TriangleStripArray || geometryArray instanceof TriangleFanArray) {
                int[] stripCounts = new int[((GeometryStripArray)geometryArray).getNumStrips()];
                ((GeometryStripArray)geometryArray).getStripVertexCounts(stripCounts);
                addStripTriangles(triangles, indices, stripCounts, geometryArray instanceof TriangleFanArray);
            }
        }
        return triangles;
    }

    /* Triangles and quads, which are split in two triangles */
    private static void addTriangles(List<int[]> triangles, int[] indices, int verticesPerPolygon) {
        for (int i = 0; i + verticesPerPolygon <= indices.length; i += verticesPerPolygon) {
            triangles.add(new int[] {indices[i], indices[i + 1], indices[i + 2]});
            if (verticesPerPolygon == 4)
                triangles.add(new int[] {indices[i], indices[i + 2], indices[i + 3]});
        }
    }

    /* Every other triangle of a strip has its vertices order reversed, to keep the same orientation */
    private static void addStripTriangles(List<int[]> triangles, int[] indices, int[] stripCounts, boolean isFan) {
        int start = 0;
        for (int stripCount : stripCounts) {
            for (int i = start; i + 2 < start + stripCount; i++) {
                if (isFan)
                    triangles.add(new int[] {indices[start], indices[i + 1], indices[i + 2]});
                else if ((i - start) % 2 == 0)
                    triangles.add(new int[] {indices[i], indices[i + 1], indices[i + 2]});
                else
                    triangles.add(new int[] {indices[i], indices[i + 2], indices[i + 1]});
            }
            start += stripCount;
        }
    }

    private static Point3d toPoint(double[] location) {
        return new Point3d(location[0], location[1], location[2]);
    }

    private static Point2d average(List<Point2d> points) {
        Point2d average = new Point2d();
        for (Point2d point : points)
            average.add(point);
        average.scale(1.0 / points.size());
        return average;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] scale(double[] a, double factor) {
        return new double[] {a[0] * factor, a[1] * factor, a[2] * factor};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    /* Triangles in a bounding volume hierarchy, for finding the ones crossed by a ray */
    private static class Obstacles {
        private static final int MAX_TRIANGLES_PER_LEAF = 4;
        private static final double EPSILON = 1E-9;

        static class Hit {
            final double distance;
            final double[] normal;

            Hit(double distance, double[] normal) {
                this.distance = distance;
                this.normal = normal;
            }
        }

        private static class BoundingVolume {
            final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            BoundingVolume first;
            BoundingVolume second;
            int start;
            int end;
        }

        /* Per triangle, its first vertex and its two edges from it */
        private List<double[]> triangles = new ArrayList<>();
        private List<Boolean> doubleSided = new ArrayList<>();
        private List<Integer> owners = new ArrayList<>();
        private int[] order;
        private BoundingVolume root;

        int size() {
            return triangles.size();
        }

        void add(double[] first, double[] second, double[] third, boolean isDoubleSided, int owner) {
            double[] firstEdge = subtract(second, first);
            double[] secondEdge = subtract(third, first);
            if (length(cross(firstEdge, secondEdge)) < EPSILON)
                return;
            triangles.add(new double[] {
                first[0], first[1], first[2],
                firstEdge[0], firstEdge[1], firstEdge[2],
                secondEdge[0], secondEdge[1], secondEdge[2]});
            doubleSided.add(isDoubleSided);
            owners.add(owner);
        }

        void build() {
            order = new int[triangles.size()];
            for (int i = 0; i < order.length; i++)
                order[i] = i;
            double[][] centers = new double[order.length][];
            for (int i = 0; i < order.length; i++) {
                double[] t = triangles.get(i);
                centers[i] = new double[] {
                    t[0] + (t[3] + t[6]) / 3, t[1] + (t[4] + t[7]) / 3, t[2] + (t[5] + t[8]) / 3};
            }
            root = order.length > 0 ? build(0, order.length, centers) : null;
        }

        private BoundingVolume build(int start, int end, double[][] centers) {
            BoundingVolume volume = new BoundingVolume();
            volume.start = start;
            volume.end = end;
            for (int i = start; i < end; i++) {
                double[] t = triangles.get(order[i]);
                for (int axis = 0; axis < 3; axis++) {
                    double first = t[axis], second = t[axis] + t[axis + 3], third = t[axis] + t[axis + 6];
                    volume.min[axis] = Math.min(volume.min[axis], Math.min(first, Math.min(second, third)));
                    volume.max[axis] = Math.max(volume.max[axis], Math.max(first, Math.max(second, third)));
                }
            }
            if (end - start <= MAX_TRIANGLES_PER_LEAF)
                return volume;

            /* Split at the median of the triangles' centers along the longest axis */
            int axis = 0;
            for (int i = 1; i < 3; i++)
                if (volume.max[i] - volume.min[i] > volume.max[axis] - volume.min[axis])
                    axis = i;
            final int splitAxis = axis;
            Integer[] range = new Integer[end - start];
            for (int i = start; i < end; i++)
                range[i - start] = order[i];
            Arrays.sort(range, (a, b) -> Double.compare(centers[a][splitAxis], centers[b][splitAxis]));
            for (int i = start; i < end; i++)
                order[i] = range[i - start];

            int middle = (start + end) / 2;
            volume.first = build(start, middle, centers);
            volume.second = build(middle, end, centers);
            return volume;
        }

        boolean isOccluded(double[] origin, double[] direction, double maxDistance, Set<Integer> ignoredOwners) {
            return root != null && find(root, origin, direction, inverse(direction), maxDistance, ignoredOwners, true) != null;
        }

        Hit getClosestHit(double[] origin, double[] direction, double maxDistance) {
            return root != null ? find(root, origin, direction, inverse(direction), maxDistance, Collections.emptySet(), false) : null;
        }

        private static double[] inverse(double[] direction) {
            return new double[] {1 / direction[0], 1 / direction[1], 1 / direction[2]};
        }

        private Hit find(BoundingVolume volume, double[] origin, double[] direction, double[] inverseDirection, double maxDistance, Set<Integer> ignoredOwners, boolean anyHit) {
            if (!intersects(volume, origin, inverseDirection, maxDistance))
                return null;

            if (volume.first == null) {
                Hit closest = null;
                for (int i = volume.start; i < volume.end; i++) {
                    int triangle = order[i];
                    if (ignoredOwners.contains(owners.get(triangle)))
                        continue;
                    double distance = intersect(triangle, origin, direction, closest != null ? closest.distance : maxDistance);
                    if (distance < 0)
                        continue;
                    double[] t = triangles.get(triangle);
                    closest = new Hit(distance, cross(new double[] {t[3], t[4], t[5]}, new double[] {t[6], t[7], t[8]}));
                    if (anyHit)
                        return closest;
                }
                if (closest != null) {
                    double normalLength = length(closest.normal);
                    closest = new Hit(closest.distance, scale(closest.normal, 1 / normalLength));
                }
                return closest;
            }

            Hit first = find(volume.first, origin, direction, inverseDirection, maxDistance, ignoredOwners, anyHit);
            if (first != null && anyHit)
                return first;
            Hit second = find(volume.second, origin, direction, inverseDirection, first != null ? first.distance : maxDistance, ignoredOwners, anyHit);
            return second != null ? second : first;
        }

        private static boolean intersects(BoundingVolume volume, double[] origin, double[] inverseDirection, double maxDistance) {
            double near = 0;
            double far = maxDistance;
            for (int axis = 0; axis < 3; axis++) {
                double first = (volume.min[axis] - origin[axis]) * inverseDirection[axis];
                double second = (volume.max[axis] - origin[axis]) * inverseDirection[axis];
                if (Double.isNaN(first) || Double.isNaN(second)) {
                    /* The ray is parallel to the axis and on one of the volume's sides */
                    continue;
                }
                near = Math.max(near, Math.min(first, second));
                far = Math.min(far, Math.max(first, second));
                if (near > far)
                    return false;
            }
            return true;
        }

        /* Möller–Trumbore ray-triangle intersection, returns the distance to the triangle or -1. Triangles are only hit
         * from their front side (counter-clockwise vertices), unless they're double sided */
        private double intersect(int triangle, double[] origin, double[] direction, double maxDistance) {
            double[] t = triangles.get(triangle);
            double px = direction[1] * t[8] - direction[2] * t[7];
            double py = direction[2] * t[6] - direction[0] * t[8];
            double pz = direction[0] * t[7] - direction[1] * t[6];
            double determinant = t[3] * px + t[4] * py + t[5] * pz;
            if (doubleSided.get(triangle) ? Math.abs(determinant) < EPSILON : determinant < EPSILON)
                return -1;

            double inverseDeterminant = 1 / determinant;
            double sx = origin[0] - t[0], sy = origin[1] - t[1], sz = origin[2] - t[2];
            double u = (sx * px + sy * py + sz * pz) * inverseDeterminant;
            if (u < 0 || u > 1)
                return -1;

            double qx = sy * t[5] - sz * t[4];
            double qy = sz * t[3] - sx * t[5];
            double qz = sx * t[4] - sy * t[3];
            double v = (direction[0] * qx + direction[1] * qy + direction[2] * qz) * inverseDeterminant;
            if (v < 0 || u + v > 1)
                return -1;

            double distance = (t[6] * qx + t[7] * qy + t[8] * qz) * inverseDeterminant;
            return distance > 1E-6 && distance < maxDistance ? distance : -1;
        }
    }
};
