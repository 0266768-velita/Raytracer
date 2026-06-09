import java.awt.*;
import java.io.IOException;
public class Main {

    public static void main(String[] args) throws IOException {
        new RayTracer();

        camera camera = new camera(new Vector3D(0, 0, 0));
//create object
        Object3D[] objects = {
                new Sphere(new Vector3D(4, 1, -6), 0.5, new Color(11,0,214)),
                new Sphere(new Vector3D(0.2, 1, -5), 1, new Color(214,0,0))
        };

        Scene scene = new Scene(objects);

        // ray to center in position (x,y)
        Ray ray = camera.generateRay(0, 0);

        Intersection hit = scene.intersect(ray);


    }
}