import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
//render the scene
public class RayTracer {

    public RayTracer() throws IOException {
//size of image
        int width = 1200;
        int height = 800;
//save size
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        // camera create

        camera camera = new camera(
                new Vector3D(0, 0, 2)
        );

        List<Object3D> objectList = new ArrayList<>();

        // spheres create


        objectList.add(
                new Sphere(
                        new Vector3D(-2, 0, -5),
                        1,
                        Color.RED
                )
        );

        objectList.add(
                new Sphere(
                        new Vector3D(2, 0, -5),
                        1,
                        Color.BLUE
                )
        );

        // TRIANGLE

        objectList.add(
                new Triangle(
                        new Vector3D(0, 2, -12),
                        new Vector3D(-2, -2, -12),
                        new Vector3D(2, -2, -12),
                        Color.GREEN
                )
        );

        // OBJ
        try {

            ObjLoader loader = new ObjLoader();
//path of obj
            loader.load(
                    "C:\\Raytracerrepo\\Raytracerv3\\OBJ\\model\\72-sword_black_death_obj\\swordHIgh.obj"
            );

            List<float[]> vertices = loader.getVertices();
            List<int[]> faces = loader.getFaces();

            int count = 0;

            for (int[] face : faces) {

                if (face.length >= 3) {

                    float[] vv0 = vertices.get(face[0]);
                    float[] vv1 = vertices.get(face[1]);
                    float[] vv2 = vertices.get(face[2]);
                    // scale

                    double scale = 1.2;


                    // position in the scene

                    double offsetX = 0;
                    double offsetY = -3;
                    double offsetZ = -18;

                    // rotation


                    Vector3D v0 = new Vector3D(
                            vv0[0] * scale + offsetX,
                            -(vv0[2] * scale) + offsetY,
                            -(vv0[1] * scale) + offsetZ
                    );

                    Vector3D v1 = new Vector3D(
                            vv1[0] * scale + offsetX,
                            -(vv1[2] * scale) + offsetY,
                            -(vv1[1] * scale) + offsetZ
                    );

                    Vector3D v2 = new Vector3D(
                            vv2[0] * scale + offsetX,
                            -(vv2[2] * scale) + offsetY,
                            -(vv2[1] * scale) + offsetZ
                    );

                    objectList.add(
                            new Triangle(
                                    v0,
                                    v1,
                                    v2,
                                    new Color(255, 200, 200)
                            )
                    );

                    count++;

                    if (count > 70000) {
                        break;
                    }
                }
            }

            System.out.println(
                    "triangles ready " + count
            );

        } catch (Exception e) {

            System.out.println(
                    "wrong--- OBJ"
            );

            e.printStackTrace();
        }


        // scene create

        Scene scene = new Scene(
                objectList.toArray(new Object3D[0])
        );

        // render ready
        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                double nx =
                        2.0 * (x + 0.5) / width - 1.0;

                double ny =
                        1.0 - 2.0 * (y + 0.5) / height;

                Ray ray = camera.generateRay(nx, ny);

                Intersection hit = scene.intersect(ray);

                if (hit != null) {

                    image.setRGB(
                            x,
                            y,
                            hit.color.getRGB()
                    );

                } else {

                    image.setRGB(
                            x,
                            y,
                            Color.BLACK.getRGB()
                    );
                }
            }
        }

        // save render in a image

        ImageIO.write(
                image,
                "png",
                new File("output.png")
        );

        System.out.println(
                "output.png ready"
        );
    }
}