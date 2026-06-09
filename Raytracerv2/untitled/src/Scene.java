//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class Scene {
    public Object3D[] objects;

    public Scene(Object3D[] objects) {
        this.objects = objects;
    }

    public Intersection intersect(Ray ray) {
        Intersection nearest = null;

        for(Object3D obj : this.objects) {
            Intersection hit = obj.intersect(ray);
            if (hit != null && (nearest == null || hit.t < nearest.t)) {
                nearest = hit;
            }
        }

        return nearest;
    }
}
