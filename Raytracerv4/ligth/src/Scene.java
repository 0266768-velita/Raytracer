import java.awt.Color;

// Scene container: groups all objects, lights, and background color
public class Scene {

    public Object3D[] objects;
    public Ligth[]    lights;
    public Color      background;

    public Scene(Object3D[] objects, Ligth[] lights, Color background) {
        this.objects    = objects;
        this.lights     = lights;
        this.background = background;
    }

    // Returns the intersection closest to the ray
    public Intersection intersect(Ray ray) {
        Intersection nearest = null;
        for (Object3D obj : objects) {
            Intersection hit = obj.intersect(ray);
            if (hit != null && (nearest == null || hit.t < nearest.t)) {
                nearest = hit;
            }
        }
        return nearest;
    }
}