import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class BVHNode implements Object3D {

    // AABB limits
    double[] bmin = new double[3];
    double[] bmax = new double[3];

    private BVHNode left;
    private BVHNode right;
    private Object3D[] leaves;

    private Material material;

    private static final int MAX_LEAF = 8;      // Maximum objects per leaf
    private static final int SAH_BINS = 16;     // Number of bins used by SAH
    private static final int PAR_THRESHOLD = 50000; // Minimum amount of objects for parallel build

    // Public constructor
    public BVHNode(List<Object3D> objects, int start, int end) {
        build(objects, start, end);
    }

    // Build BVH using multiple CPU cores
    public static BVHNode buildParallel(List<Object3D> objects) {
        int cores = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(cores);

        try {
            return pool.invoke(new BVHTask(objects, 0, objects.size()));
        } finally {
            pool.shutdown();
        }
    }

    private static class BVHTask extends RecursiveTask<BVHNode> {

        private final List<Object3D> objects;
        private final int start;
        private final int end;

        BVHTask(List<Object3D> objects, int start, int end) {
            this.objects = objects;
            this.start = start;
            this.end = end;
        }

        @Override
        protected BVHNode compute() {

            int count = end - start;

            // Small subtree, build normally
            if (count <= PAR_THRESHOLD) {
                return new BVHNode(objects, start, end);
            }

            // Split work into two parallel tasks
            int mid = start + count / 2;

            BVHTask leftTask = new BVHTask(objects, start, mid);
            BVHTask rightTask = new BVHTask(objects, mid, end);

            leftTask.fork();

            BVHNode rightNode = rightTask.compute();
            BVHNode leftNode = leftTask.join();

            BVHNode node = new BVHNode();
            node.left = leftNode;
            node.right = rightNode;

            node.computeBoundsFromChildren();

            return node;
        }
    }

    // Private constructor for empty nodes
    private BVHNode() {
        bmin[0] = bmin[1] = bmin[2] = Double.MAX_VALUE;
        bmax[0] = bmax[1] = bmax[2] = -Double.MAX_VALUE;
    }

    // Build BVH using SAH
    private void build(List<Object3D> objects, int start, int end) {

        computeBounds(objects, start, end);

        int count = end - start;

        // Create a leaf node
        if (count <= MAX_LEAF) {
            leaves = new Object3D[count];

            for (int i = 0; i < count; i++) {
                leaves[i] = objects.get(start + i);
            }

            return;
        }

        // Find the best split using SAH
        int bestAxis = longestAxis();
        int bestSplit = start + count / 2;
        double bestCost = Double.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {

            // Sort objects using their centroid on this axis
            final int currentAxis = axis;

            objects.subList(start, end).sort((a, b) ->
                    Double.compare(centroid(a, currentAxis), centroid(b, currentAxis)));

            // Test possible split positions
            for (int bin = 1; bin < SAH_BINS; bin++) {

                int split = start + (count * bin) / SAH_BINS;

                if (split <= start || split >= end) {
                    continue;
                }

                double leftCost =
                        surfaceArea(objects, start, split) * (split - start);

                double rightCost =
                        surfaceArea(objects, split, end) * (end - split);

                double totalCost = leftCost + rightCost;

                if (totalCost < bestCost) {
                    bestCost = totalCost;
                    bestAxis = axis;
                    bestSplit = split;
                }
            }
        }

        // Sort using the best axis found
        final int axis = bestAxis;

        objects.subList(start, end).sort((a, b) ->
                Double.compare(centroid(a, axis), centroid(b, axis)));

        left = new BVHNode(objects, start, bestSplit);
        right = new BVHNode(objects, bestSplit, end);
    }

    // Calculate the surface area of a group of objects
    private double surfaceArea(List<Object3D> objects, int start, int end) {

        double[] min = {
                Double.MAX_VALUE,
                Double.MAX_VALUE,
                Double.MAX_VALUE
        };

        double[] max = {
                -Double.MAX_VALUE,
                -Double.MAX_VALUE,
                -Double.MAX_VALUE
        };

        for (int i = start; i < end; i++) {

            double[][] bounds = getBounds(objects.get(i));

            for (int k = 0; k < 3; k++) {
                min[k] = Math.min(min[k], bounds[0][k]);
                max[k] = Math.max(max[k], bounds[1][k]);
            }
        }

        double dx = max[0] - min[0];
        double dy = max[1] - min[1];
        double dz = max[2] - min[2];

        return 2.0 * (dx * dy + dy * dz + dz * dx);
    }

    // Check if the ray hits this AABB
    private boolean intersectsAABB(Ray ray) {

        double ox = ray.getOrigin().x;
        double oy = ray.getOrigin().y;
        double oz = ray.getOrigin().z;

        double dx = ray.getDirection().x;
        double dy = ray.getDirection().y;
        double dz = ray.getDirection().z;

        double tmin = 1e-6;
        double tmax = Double.MAX_VALUE;

        // X axis test
        double invDx = 1.0 / dx;

        double t0x = (bmin[0] - ox) * invDx;
        double t1x = (bmax[0] - ox) * invDx;

        if (invDx < 0) {
            double temp = t0x;
            t0x = t1x;
            t1x = temp;
        }

        tmin = Math.max(tmin, t0x);
        tmax = Math.min(tmax, t1x);

        if (tmax <= tmin) {
            return false;
        }

        // Y axis test
        double invDy = 1.0 / dy;

        double t0y = (bmin[1] - oy) * invDy;
        double t1y = (bmax[1] - oy) * invDy;

        if (invDy < 0) {
            double temp = t0y;
            t0y = t1y;
            t1y = temp;
        }

        tmin = Math.max(tmin, t0y);
        tmax = Math.min(tmax, t1y);

        if (tmax <= tmin) {
            return false;
        }

        // Z axis test
        double invDz = 1.0 / dz;

        double t0z = (bmin[2] - oz) * invDz;
        double t1z = (bmax[2] - oz) * invDz;

        if (invDz < 0) {
            double temp = t0z;
            t0z = t1z;
            t1z = temp;
        }

        tmin = Math.max(tmin, t0z);
        tmax = Math.min(tmax, t1z);

        return tmax > tmin;
    }

    @Override
    public Intersection intersect(Ray ray) {

        if (!intersectsAABB(ray)) {
            return null;
        }

        // Check all objects inside the leaf
        if (leaves != null) {

            Intersection nearest = null;

            for (Object3D object : leaves) {

                Intersection hit = object.intersect(ray);

                if (hit != null &&
                        (nearest == null || hit.getT() < nearest.getT())) {

                    nearest = hit;
                }
            }

            return nearest;
        }

        // Visit the closest child first
        Intersection hitLeft =
                left != null ? left.intersect(ray) : null;

        Intersection hitRight =
                right != null ? right.intersect(ray) : null;

        if (hitLeft == null) {
            return hitRight;
        }

        if (hitRight == null) {
            return hitLeft;
        }

        return hitLeft.getT() < hitRight.getT()
                ? hitLeft
                : hitRight;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public void setMaterial(Material material) {
        this.material = material;
    }

    // Utility methods
    private void computeBounds(List<Object3D> objects, int start, int end) {

        bmin[0] = bmin[1] = bmin[2] = Double.MAX_VALUE;
        bmax[0] = bmax[1] = bmax[2] = -Double.MAX_VALUE;

        for (int i = start; i < end; i++) {

            double[][] bounds = getBounds(objects.get(i));

            for (int k = 0; k < 3; k++) {
                bmin[k] = Math.min(bmin[k], bounds[0][k]);
                bmax[k] = Math.max(bmax[k], bounds[1][k]);
            }
        }
    }

    // Compute bounds using both children
    private void computeBoundsFromChildren() {

        bmin[0] = bmin[1] = bmin[2] = Double.MAX_VALUE;
        bmax[0] = bmax[1] = bmax[2] = -Double.MAX_VALUE;

        if (left != null) {
            for (int k = 0; k < 3; k++) {
                bmin[k] = Math.min(bmin[k], left.bmin[k]);
                bmax[k] = Math.max(bmax[k], left.bmax[k]);
            }
        }

        if (right != null) {
            for (int k = 0; k < 3; k++) {
                bmin[k] = Math.min(bmin[k], right.bmin[k]);
                bmax[k] = Math.max(bmax[k], right.bmax[k]);
            }
        }
    }

    // Return the longest axis of the box
    private int longestAxis() {

        double dx = bmax[0] - bmin[0];
        double dy = bmax[1] - bmin[1];
        double dz = bmax[2] - bmin[2];

        if (dx >= dy && dx >= dz) {
            return 0;
        }

        if (dy >= dz) {
            return 1;
        }

        return 2;
    }

    // Get the center of an object on a given axis
    private double centroid(Object3D object, int axis) {

        double[][] bounds = getBounds(object);

        return (bounds[0][axis] + bounds[1][axis]) * 0.5;
    }

    // Return the bounds of an object
    static double[][] getBounds(Object3D object) {

        double[] min = {
                Double.MAX_VALUE,
                Double.MAX_VALUE,
                Double.MAX_VALUE
        };

        double[] max = {
                -Double.MAX_VALUE,
                -Double.MAX_VALUE,
                -Double.MAX_VALUE
        };

        // Triangle bounds
        if (object instanceof Triangle) {

            for (Vector3D vertex : ((Triangle) object).getVertices()) {

                min[0] = Math.min(min[0], vertex.x);
                max[0] = Math.max(max[0], vertex.x);

                min[1] = Math.min(min[1], vertex.y);
                max[1] = Math.max(max[1], vertex.y);

                min[2] = Math.min(min[2], vertex.z);
                max[2] = Math.max(max[2], vertex.z);
            }
        }
        // Sphere bounds
        else if (object instanceof Sphere) {

            Sphere sphere = (Sphere) object;

            Vector3D center = sphere.getCenter();
            double radius = sphere.getRadius();

            min[0] = center.x - radius;
            max[0] = center.x + radius;

            min[1] = center.y - radius;
            max[1] = center.y + radius;

            min[2] = center.z - radius;
            max[2] = center.z + radius;
        }
        // BVH node bounds
        else if (object instanceof BVHNode) {

            BVHNode node = (BVHNode) object;

            min[0] = node.bmin[0];
            min[1] = node.bmin[1];
            min[2] = node.bmin[2];

            max[0] = node.bmax[0];
            max[1] = node.bmax[1];
            max[2] = node.bmax[2];
        }
        // Fallback bounds for unknown objects
        else {

            min[0] = min[1] = min[2] = -0.001;
            max[0] = max[1] = max[2] = 0.001;
        }

        // Avoid zero-size bounding boxes
        for (int k = 0; k < 3; k++) {

            if (max[k] - min[k] < 1e-5) {
                min[k] -= 1e-5;
                max[k] += 1e-5;
            }
        }

        return new double[][]{min, max};
    }
}