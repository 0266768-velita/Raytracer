public class Scene {

    public Object3D[] objects;

    public Scene(Object3D[] objects) {
        this.objects = objects;
    }
//intersection if hit in the scene draw color or null color
    public Intersection intersect(Ray ray) {

        Intersection nearest = null;

        for (Object3D obj : objects) {

            Intersection hit = obj.intersect(ray);

            if (hit != null) {
                if (nearest == null || hit.t < nearest.t) {
                    nearest = hit;
                }
            }
        }

        return nearest;
    }
}