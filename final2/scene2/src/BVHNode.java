import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

// BVH accelerator — parallel build via ForkJoin, SAH splitting, multi-primitive leaves
public class BVHNode implements Object3D {

    // AABB bounds
    double[] bmin = new double[3];
    double[] bmax = new double[3];

    private BVHNode   left;
    private BVHNode   right;
    private Object3D[] leaves;

    private Material material;

    private static final int MAX_LEAF       = 8;       // max primitives per leaf
    private static final int SAH_BINS       = 16;      // bins used for SAH evaluation
    private static final int PAR_THRESHOLD  = 50_000;  // subtrees smaller than this build sequentially

    // main public constructor
    public BVHNode(List<Object3D> objects, int start, int end) {
        build(objects, start, end);
    }

    // kicks off a parallel build using all available cores
    public static BVHNode buildParallel(List<Object3D> objects) {
        int cores = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(cores);
        try {
            return pool.invoke(new BVHTask(objects, 0, objects.size()));
        } finally {
            pool.shutdown();
        }
    }

    // ForkJoin task — splits the work in half once the range is large enough
    private static class BVHTask extends RecursiveTask<BVHNode> {
        private final List<Object3D> objects;
        private final int start, end;

        BVHTask(List<Object3D> objects, int start, int end) {
            this.objects = objects;
            this.start   = start;
            this.end     = end;
        }

        @Override
        protected BVHNode compute() {
            int count = end - start;

            // small enough — just build sequentially
            if (count <= PAR_THRESHOLD) {
                return new BVHNode(objects, start, end);
            }

            // split down the middle and process both halves in parallel
            int mid = start + count / 2;
            BVHTask taskL = new BVHTask(objects, start, mid);
            BVHTask taskR = new BVHTask(objects, mid,   end);
            taskL.fork();
            BVHNode nodeR = taskR.compute();
            BVHNode nodeL = taskL.join();

            BVHNode node = new BVHNode();
            node.left  = nodeL;
            node.right = nodeR;
            node.computeBoundsFromChildren();
            return node;
        }
    }

    // empty node used when merging parallel results
    private BVHNode() {
        bmin[0] = bmin[1] = bmin[2] =  Double.MAX_VALUE;
        bmax[0] = bmax[1] = bmax[2] = -Double.MAX_VALUE;
    }

    // recursive SAH build
    private void build(List<Object3D> objects, int start, int end) {

        computeBounds(objects, start, end);
        int count = end - start;

        // leaf node — store primitives directly
        if (count <= MAX_LEAF) {
            leaves = new Object3D[count];
            for (int i = 0; i < count; i++) leaves[i] = objects.get(start + i);
            return;
        }

        // try SAH_BINS split candidates on each axis, pick the cheapest
        int    bestAxis  = longestAxis();
        int    bestSplit = start + count / 2;
        double bestCost  = Double.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {
            final int ax = axis;
            objects.subList(start, end).sort((a, b) ->
                    Double.compare(centroid(a, ax), centroid(b, ax)));

            for (int bin = 1; bin < SAH_BINS; bin++) {
                int split = start + (count * bin) / SAH_BINS;
                if (split <= start || split >= end) continue;

                double costL = surfaceArea(objects, start, split) * (split - start);
                double costR = surfaceArea(objects, split, end)   * (end   - split);
                double cost  = costL + costR;

                if (cost < bestCost) {
                    bestCost  = cost;
                    bestAxis  = axis;
                    bestSplit = split;
                }
            }
        }

        // sort by the winning axis before splitting
        final int finalAxis = bestAxis;
        objects.subList(start, end).sort((a, b) ->
                Double.compare(centroid(a, finalAxis), centroid(b, finalAxis)));

        left  = new BVHNode(objects, start,     bestSplit);
        right = new BVHNode(objects, bestSplit,  end);
    }

    // surface area of the AABB enclosing objects[start..end)
    private double surfaceArea(List<Object3D> objects, int start, int end) {
        double[] mn = { Double.MAX_VALUE,  Double.MAX_VALUE,  Double.MAX_VALUE };
        double[] mx = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (int i = start; i < end; i++) {
            double[][] b = getBounds(objects.get(i));
            for (int k = 0; k < 3; k++) {
                mn[k] = Math.min(mn[k], b[0][k]);
                mx[k] = Math.max(mx[k], b[1][k]);
            }
        }
        double dx = mx[0] - mn[0], dy = mx[1] - mn[1], dz = mx[2] - mn[2];
        return 2.0 * (dx * dy + dy * dz + dz * dx);
    }

    // slab method AABB test — returns false early on any axis miss
    private boolean intersectsAABB(Ray ray) {
        double ox = ray.getOrigin().x,    oy = ray.getOrigin().y,    oz = ray.getOrigin().z;
        double dx = ray.getDirection().x, dy = ray.getDirection().y, dz = ray.getDirection().z;

        double tmin = 1e-6, tmax = Double.MAX_VALUE;

        // X slab
        double invDx = 1.0 / dx;
        double t0x = (bmin[0] - ox) * invDx, t1x = (bmax[0] - ox) * invDx;
        if (invDx < 0) { double tmp = t0x; t0x = t1x; t1x = tmp; }
        tmin = Math.max(tmin, t0x); tmax = Math.min(tmax, t1x);
        if (tmax <= tmin) return false;

        // Y slab
        double invDy = 1.0 / dy;
        double t0y = (bmin[1] - oy) * invDy, t1y = (bmax[1] - oy) * invDy;
        if (invDy < 0) { double tmp = t0y; t0y = t1y; t1y = tmp; }
        tmin = Math.max(tmin, t0y); tmax = Math.min(tmax, t1y);
        if (tmax <= tmin) return false;

        // Z slab
        double invDz = 1.0 / dz;
        double t0z = (bmin[2] - oz) * invDz, t1z = (bmax[2] - oz) * invDz;
        if (invDz < 0) { double tmp = t0z; t0z = t1z; t1z = tmp; }
        tmin = Math.max(tmin, t0z); tmax = Math.min(tmax, t1z);
        return tmax > tmin;
    }

    @Override
    public Intersection intersect(Ray ray) {
        if (!intersectsAABB(ray)) return null;

        // leaf — test all primitives and return the closest hit
        if (leaves != null) {
            Intersection nearest = null;
            for (Object3D obj : leaves) {
                Intersection hit = obj.intersect(ray);
                if (hit != null && (nearest == null || hit.getT() < nearest.getT()))
                    nearest = hit;
            }
            return nearest;
        }

        // inner node — visit both children, return the closer hit
        Intersection hitL = left  != null ? left.intersect(ray)  : null;
        Intersection hitR = right != null ? right.intersect(ray) : null;
        if (hitL == null) return hitR;
        if (hitR == null) return hitL;
        return hitL.getT() < hitR.getT() ? hitL : hitR;
    }

    @Override public Material getMaterial()       { return material; }
    @Override public void setMaterial(Material m) { this.material = m; }

    // expand AABB to cover all objects in range
    private void computeBounds(List<Object3D> objects, int start, int end) {
        bmin[0] = bmin[1] = bmin[2] =  Double.MAX_VALUE;
        bmax[0] = bmax[1] = bmax[2] = -Double.MAX_VALUE;
        for (int i = start; i < end; i++) {
            double[][] b = getBounds(objects.get(i));
            for (int k = 0; k < 3; k++) {
                bmin[k] = Math.min(bmin[k], b[0][k]);
                bmax[k] = Math.max(bmax[k], b[1][k]);
            }
        }
    }

    // recompute AABB from already-built children (used in parallel merge)
    private void computeBoundsFromChildren() {
        bmin[0] = bmin[1] = bmin[2] =  Double.MAX_VALUE;
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

    // index of the axis with the largest extent
    private int longestAxis() {
        double dx = bmax[0] - bmin[0], dy = bmax[1] - bmin[1], dz = bmax[2] - bmin[2];
        if (dx >= dy && dx >= dz) return 0;
        if (dy >= dz)             return 1;
        return 2;
    }

    private double centroid(Object3D obj, int axis) {
        double[][] b = getBounds(obj);
        return (b[0][axis] + b[1][axis]) * 0.5;
    }

    // returns {min, max} bounds for any supported object type
    static double[][] getBounds(Object3D obj) {
        double[] mn = { Double.MAX_VALUE,  Double.MAX_VALUE,  Double.MAX_VALUE };
        double[] mx = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };

        if (obj instanceof Triangle) {
            for (Vector3D v : ((Triangle) obj).getVertices()) {
                mn[0] = Math.min(mn[0], v.x); mx[0] = Math.max(mx[0], v.x);
                mn[1] = Math.min(mn[1], v.y); mx[1] = Math.max(mx[1], v.y);
                mn[2] = Math.min(mn[2], v.z); mx[2] = Math.max(mx[2], v.z);
            }
        } else if (obj instanceof Sphere) {
            Sphere s = (Sphere) obj;
            Vector3D c = s.getCenter();
            double r = s.getRadius();
            mn[0] = c.x - r; mx[0] = c.x + r;
            mn[1] = c.y - r; mx[1] = c.y + r;
            mn[2] = c.z - r; mx[2] = c.z + r;
        } else if (obj instanceof BVHNode) {
            BVHNode n = (BVHNode) obj;
            mn[0] = n.bmin[0]; mn[1] = n.bmin[1]; mn[2] = n.bmin[2];
            mx[0] = n.bmax[0]; mx[1] = n.bmax[1]; mx[2] = n.bmax[2];
        } else {
            // unknown type — give it a tiny default box
            mn[0] = mn[1] = mn[2] = -0.001;
            mx[0] = mx[1] = mx[2] =  0.001;
        }

        // make sure no axis has a degenerate zero-width slab
        for (int k = 0; k < 3; k++) {
            if (mx[k] - mn[k] < 1e-5) { mn[k] -= 1e-5; mx[k] += 1e-5; }
        }

        return new double[][]{ mn, mx };
    }
}