import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/*
 * Stores all objects, lights and background information.
 * Also handles BVH construction and ray intersections.
 */
public class Scene {

    private List<Object3D> objects;
    private List<Ligth> lights;
    private Color background;

    private BVHNode bvh;
    private boolean bvhBuilt = false;

    // Create an empty scene
    public Scene(Color background) {
        this.background = background;
        this.objects = new ArrayList<>();
        this.lights = new ArrayList<>();
    }

    // Create a scene with objects and lights already loaded
    public Scene(
            Object3D[] objects,
            Ligth[] lights,
            Color background
    ) {

        this(background);

        if (objects != null) {
            for (Object3D o : objects) {
                this.objects.add(o);
            }
        }

        if (lights != null) {
            for (Ligth l : lights) {
                this.lights.add(l);
            }
        }

        buildBVH();
    }

    // Build the BVH acceleration structure
    public void buildBVH() {

        if (objects.isEmpty()) {
            bvhBuilt = false;
            return;
        }

        int total = objects.size();

        int cores =
                Runtime.getRuntime()
                        .availableProcessors();

        System.out.println(
                "Building BVH for "
                        + total
                        + " objects using "
                        + cores
                        + " cores..."
        );

        // Simple loading spinner while building
        Thread progressThread =
                new Thread(() -> {

                    long start =
                            System.currentTimeMillis();

                    String[] spinner = {
                            "|",
                            "/",
                            "-",
                            "\\"
                    };

                    int tick = 0;

                    while (!Thread.currentThread().isInterrupted()) {

                        try {

                            Thread.sleep(300);

                            long elapsed =
                                    (System.currentTimeMillis()
                                            - start) / 1000;

                            System.out.printf(
                                    "\r  %s Building... %ds   ",
                                    spinner[tick % 4],
                                    elapsed
                            );

                            tick++;

                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });

        progressThread.setDaemon(true);
        progressThread.start();

        long t0 =
                System.currentTimeMillis();

        // Build BVH using parallel processing
        bvh =
                BVHNode.buildParallel(
                        new ArrayList<>(objects)
                );

        bvhBuilt = true;

        progressThread.interrupt();

        double sec =
                (System.currentTimeMillis()
                        - t0) / 1000.0;

        System.out.printf(
                "\rBVH ready in %.1fs ✓%n",
                sec
        );
    }

    // Add an object to the scene
    public void addObject(Object3D object) {

        objects.add(object);

        // BVH needs to be rebuilt
        bvhBuilt = false;
    }

    // Add a light to the scene
    public void addLight(Ligth light) {
        lights.add(light);
    }

    public List<Object3D> getObjects() {
        return objects;
    }

    public List<Ligth> getLights() {
        return lights;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    // Find the closest intersection
    public Intersection intersect(Ray ray) {
        return intersect(ray, null);
    }

    // Find the closest intersection while ignoring one object
    public Intersection intersect(
            Ray ray,
            Object3D ignored
    ) {

        // Use BVH when available
        if (bvhBuilt && ignored == null) {
            return bvh.intersect(ray);
        }

        Intersection nearest = null;

        for (Object3D obj : objects) {

            if (obj == ignored) {
                continue;
            }

            Intersection hit =
                    obj.intersect(ray);

            if (hit != null
                    && (nearest == null
                    || hit.getT() < nearest.getT())) {

                nearest = hit;
            }
        }

        return nearest;
    }

    // Check if a ray is blocked before reaching a distance
    public boolean isOccluded(
            Ray ray,
            double maxDistance
    ) {

        return isOccluded(
                ray,
                maxDistance,
                null
        );
    }

    // Check if a ray is blocked while ignoring one object
    public boolean isOccluded(
            Ray ray,
            double maxDistance,
            Object3D ignored
    ) {

        // Fast BVH shadow test
        if (bvhBuilt && ignored == null) {

            Intersection hit =
                    bvh.intersect(ray);

            return hit != null
                    && hit.getT() > 1e-5
                    && hit.getT() < maxDistance;
        }

        // Brute force shadow test
        for (Object3D obj : objects) {

            if (obj == ignored) {
                continue;
            }

            Intersection hit =
                    obj.intersect(ray);

            if (hit != null
                    && hit.getT() > 1e-5
                    && hit.getT() < maxDistance) {

                return true;
            }
        }

        return false;
    }
}