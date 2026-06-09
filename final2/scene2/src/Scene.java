import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Scene {

    private List<Object3D> objects;
    private List<Ligth>    lights;
    private Color          background;
    private BVHNode        bvh;
    private boolean        bvhBuilt = false;

    public Scene(Color background) {
        this.background = background;
        this.objects    = new ArrayList<>();
        this.lights     = new ArrayList<>();
    }

    public Scene(Object3D[] objects, Ligth[] lights, Color background) {
        this(background);
        if (objects != null) for (Object3D o : objects) this.objects.add(o);
        if (lights  != null) for (Ligth   l : lights)  this.lights.add(l);
        buildBVH();
    }

    public void buildBVH() {
        if (objects.isEmpty()) { bvhBuilt = false; return; }

        int total = objects.size();
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Building BVH over " + total + " objects using " + cores + " cores...");

        // spinner thread so the console doesn't look frozen
        Thread progressThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            String[] spinner = {"|", "/", "-", "\\"};
            int tick = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300);
                    long elapsed = (System.currentTimeMillis() - start) / 1000;
                    System.out.printf("\r  %s Building... %ds   ", spinner[tick % 4], elapsed);
                    tick++;
                } catch (InterruptedException e) { break; }
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();

        long t0 = System.currentTimeMillis();
        // parallel build via ForkJoin
        bvh = BVHNode.buildParallel(new ArrayList<>(objects));
        bvhBuilt = true;

        progressThread.interrupt();
        double sec = (System.currentTimeMillis() - t0) / 1000.0;
        System.out.printf("\rBVH built in %.1fs ✓                              %n", sec);
    }

    public void addObject(Object3D o) { objects.add(o); bvhBuilt = false; }
    public void addLight(Ligth l)     { lights.add(l); }

    public List<Object3D> getObjects()    { return objects;    }
    public List<Ligth>    getLights()     { return lights;     }
    public Color          getBackground() { return background; }
    public void           setBackground(Color c) { this.background = c; }

    public Intersection intersect(Ray ray) { return intersect(ray, null); }

    // if BVH is ready and no object is ignored, delegate to it for speed
    public Intersection intersect(Ray ray, Object3D ignored) {
        if (bvhBuilt && ignored == null) return bvh.intersect(ray);
        Intersection nearest = null;
        for (Object3D obj : objects) {
            if (obj == ignored) continue;
            Intersection hit = obj.intersect(ray);
            if (hit != null && (nearest == null || hit.getT() < nearest.getT())) nearest = hit;
        }
        return nearest;
    }

    public boolean isOccluded(Ray ray, double maxDistance) {
        return isOccluded(ray, maxDistance, null);
    }

    // returns true as soon as any object blocks the ray within maxDistance
    public boolean isOccluded(Ray ray, double maxDistance, Object3D ignored) {
        if (bvhBuilt && ignored == null) {
            Intersection hit = bvh.intersect(ray);
            return hit != null && hit.getT() > 1e-5 && hit.getT() < maxDistance;
        }
        for (Object3D obj : objects) {
            if (obj == ignored) continue;
            Intersection hit = obj.intersect(ray);
            if (hit != null && hit.getT() > 1e-5 && hit.getT() < maxDistance) return true;
        }
        return false;
    }
}