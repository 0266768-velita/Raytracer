import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//class for read a Obj manual
//Parse obj files and extract vertices and faces for use in the ray tracer pipeline
public class ObjLoader {
//List of vertices
    private List<float[]> vertices = new ArrayList<>();
    //list of faces
    private List<int[]> faces = new ArrayList<>();

    public List<float[]> getVertices() {
        return vertices;
    }

    public List<int[]> getFaces() {
        return faces;
    }

    public void load(String filePath) throws IOException {

        BufferedReader reader =
                new BufferedReader(
                        new FileReader(filePath)
                );

        String line;

        while ((line = reader.readLine()) != null) {

            line = line.trim();
//Ignore comments and empty lines
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            String[] p = line.split("\\s+");


            // VERTICES

            if (p[0].equals("v")) {

                vertices.add(
                        new float[]{
                                Float.parseFloat(p[1]),
                                Float.parseFloat(p[2]),
                                Float.parseFloat(p[3])
                        }
                );
            }

            // FACES

            else if (p[0].equals("f")) {

                int[] face =
                        new int[p.length - 1];

                for (int i = 1; i < p.length; i++) {

                    String[] data =
                            p[i].split("/");

                    face[i - 1] =
                            Integer.parseInt(data[0]) - 1;
                }

                faces.add(face);
            }
        }

        reader.close();

        System.out.println(
                "OBJ ready"
        );
    }
}