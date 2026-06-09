/**
 * Base interface for all objects in the scene.
 */
public interface Object3D {

    // Check if a ray hits this object
    Intersection intersect(Ray ray);

    // Return the object's material
    Material getMaterial();

    // Set the object's material
    void setMaterial(Material material);
}