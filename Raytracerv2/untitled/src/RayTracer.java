import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class RayTracer {

    public RayTracer() throws IOException {
        int width = 400;
        int height = 400;
//save image with measure
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//create camera
        camera camera = new camera(new Vector3D(0.0, 0.0, 0.0));
//create a sphere and add colors
        Object3D[] objects = new Object3D[] {
                new Sphere(
                        new Vector3D(4.0, 1.0, -6.0),
                        0.5,
                        new Color(11, 0, 214)
                ),

                new Sphere(
                        new Vector3D(0.2, 1.0, -5.0),
                        1.0,
                        new Color(214, 0, 0)
                ),
//crate a trinagle with cordinates
                new Triangle(
                        new Vector3D(0.0, 1.0, -5.0),
                        new Vector3D(-1.0, -1.0, -5.0),
                        new Vector3D(1.0, -1.0, -5.0),
                        Color.GREEN
                )
        };
//create de scene and the background  with ligth
        Scene scene = new Scene(objects);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double nx = 2.0 * (x + 0.5) / width - 1.0;
                double ny = 1.0 - 2.0 * (y + 0.5) / height;

                Ray ray = camera.generateRay(nx, ny);
                Intersection hit = scene.intersect(ray);

                if (hit != null) {
                    image.setRGB(x, y, hit.color.getRGB());
                } else {
                    image.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }

        ImageIO.write(image, "png", new File("output.png"));
        System.out.println("Generated image: output.png");
    }
}