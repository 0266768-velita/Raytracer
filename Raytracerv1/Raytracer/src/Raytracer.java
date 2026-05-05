import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

 class RayTracer {

    public RayTracer() throws IOException {
//window size
        int width = 400;
        int height = 400;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        camera camera = new camera(new Vector3D(0, 0, 0));
//create a objects with cordinates and colors
        Object3D[] objects = {
                new Sphere(new Vector3D(4, 1, -6), 0.5, new Color(11,0,214)),
                new Sphere(new Vector3D(0.2, 1, -5), 1, new Color(214,0,0))
        };


        Scene scene = new Scene(objects);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double nx = (2.0 * (x + 0.5) / width - 1);
                double ny = (1.0 - 2.0 * (y + 0.5) / height);

                Ray ray = camera.generateRay(nx, ny);
                Intersection hit = scene.intersect(ray);

                if (hit != null) {
                    image.setRGB(x, y, hit.color.getRGB());
                } else {
                    image.setRGB(x, y, 0xFFFFFF);
                }
            }
        }
//save image
        ImageIO.write(image, "png", new File("output.png"));
        System.out.println("Generate image");
    }
}