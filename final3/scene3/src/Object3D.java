// base interface for all renderable scene objects
public interface Object3D {

    // returns intersection info or null if the ray misses
    Intersection intersect(Ray ray);

    Material getMaterial();
    void setMaterial(Material material);
}