import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class RayTracer {

    private static final String OBJ_FILE  = "C:/Raytracerrepo/final3/scene3/models/Untitled.obj";
    private static final String MTL_FILE  = "C:/Raytracerrepo/final3/scene3/models/Untitled.mtl";
    private static final String HDRI_PATH = "C:/Raytracerrepo/final3/scene3/models/sky.jpg";
    private static final String OUT_DIR   = "C:/Raytracerrepo/final3/scene3/out";
    private static final double SHADOW_BIAS = 1e-4;
    private static final int    MAX_DEPTH   = 4;
    private static final Color  BG_COLOR    = new Color(0, 0, 0);

    private HDRILight hdri = null;
    private final Map<String,Texture> textureMap = new LinkedHashMap<>();
    private final Map<String,Texture> normalMap  = new LinkedHashMap<>();
    private final Map<String,Texture> roughMap   = new LinkedHashMap<>();
    private Map<String,Material> materials;

    public RayTracer() throws Exception {
        materials = new LinkedHashMap<>();
        loadMtl(MTL_FILE, materials);
        System.out.println("Materials: " + materials.size());

        hdri = new HDRILight(HDRI_PATH);
        System.out.println("HDRI loaded for reflections.");

        File objFile = new File(OBJ_FILE);
        List<Object3D> objectList = new ArrayList<>();

        if (objFile.exists()) {
            ObjLoader loader = new ObjLoader();
            loader.load(OBJ_FILE);
            List<Vector3D> verts = loader.getVertices();

            double minX=Double.MAX_VALUE, maxX=-Double.MAX_VALUE;
            double minY=Double.MAX_VALUE, maxY=-Double.MAX_VALUE;
            double minZ=Double.MAX_VALUE, maxZ=-Double.MAX_VALUE;
            for (Vector3D v : verts) {
                minX=Math.min(minX,v.x); maxX=Math.max(maxX,v.x);
                minY=Math.min(minY,v.y); maxY=Math.max(maxY,v.y);
                minZ=Math.min(minZ,v.z); maxZ=Math.max(maxZ,v.z);
            }
            double cx=(minX+maxX)/2, cy=(minY+maxY)/2, cz=(minZ+maxZ)/2;
            double maxSize=Math.max(maxX-minX, Math.max(maxY-minY, maxZ-minZ));
            double scale = 3.0 / maxSize;
            System.out.printf("Bounds: X=%.2f Y=%.2f Z=%.2f%n", maxX-minX, maxY-minY, maxZ-minZ);
            System.out.printf("Scale: %.6f  Final: %.2f x %.2f x %.2f%n",
                    scale, (maxX-minX)*scale, (maxY-minY)*scale, (maxZ-minZ)*scale);

            Material defaultMat = materials.isEmpty()
                    ? Material.diffuse(new Color(200,195,185))
                    : materials.values().iterator().next();

            List<Vector3D> norms = loader.getNormals();
            List<Vector2D> uvs   = loader.getTextureCoordinates();
            Triangle.initPools(verts.size()+10, norms.size()+10, uvs.size()+10);

            for (Vector3D v : verts) {
                Vector3D vt = tr(v, cx, cy, cz, scale);
                Triangle.GVX[Triangle.vTop]=vt.x; Triangle.GVY[Triangle.vTop]=vt.y; Triangle.GVZ[Triangle.vTop]=vt.z; Triangle.vTop++;
            }
            for (Vector3D n : norms) {
                Triangle.GNX[Triangle.nTop]=n.x; Triangle.GNY[Triangle.nTop]=n.y; Triangle.GNZ[Triangle.nTop]=n.z; Triangle.nTop++;
            }
            for (Vector2D uv : uvs) {
                Triangle.GUU[Triangle.tTop]=(float)uv.getU(); Triangle.GUV[Triangle.tTop]=(float)uv.getV(); Triangle.tTop++;
            }
            System.out.println("Pools: " + Triangle.vTop + " verts, " + Triangle.nTop + " normals, " + Triangle.tTop + " uvs");

            int triCount = 0;
            for (Face face : loader.getFaces()) {
                int[] vi=face.getVertexIndices(), ni=face.getNormalIndices(), ti=face.getUvIndices();
                if (vi==null||vi.length<3) continue;
                String matName=face.getMaterialName();
                Material mat=(matName!=null&&materials.containsKey(matName))?materials.get(matName):defaultMat;
                for (int i=1; i<vi.length-1; i++) {
                    int i0=vi[0], i1=vi[i], i2=vi[i+1];
                    int n0=-1, n1=-1, n2=-1;
                    if (ni!=null&&ni.length>i+1&&ni[0]>=0&&ni[i]>=0&&ni[i+1]>=0){n0=ni[0];n1=ni[i];n2=ni[i+1];}
                    int t0=-1, t1=-1, t2=-1;
                    if (ti!=null&&ti.length>i+1&&ti[0]>=0&&ti[i]>=0&&ti[i+1]>=0){t0=ti[0];t1=ti[i];t2=ti[i+1];}
                    Texture tex=(matName!=null)?textureMap.get(matName):null;
                    objectList.add(new Triangle(i0,i1,i2,n0,n1,n2,t0,t1,t2,mat,tex));
                    triCount++;
                }
            }
            System.out.println("Triangles: " + triCount);

            // dark mirror floor
            Material floorMat = new Material(new Color(5,2,8), 0.01, 0.6, 0.4, 64.0, 0.30, 0.0, 1.0);
            objectList.add(new Triangle(new Vector3D(-8,-1.0,-3), new Vector3D(8,-1.0,-3), new Vector3D(8,-1.0,6),  floorMat));
            objectList.add(new Triangle(new Vector3D(-8,-1.0,-3), new Vector3D(8,-1.0,6),  new Vector3D(-8,-1.0,6), floorMat));

        } else {
            System.out.println("ERROR: file not found: " + OBJ_FILE);
        }

        Camera cam = new Camera(new Vector3D(-0.5, 0.1, -2.2), 50.0);
        cam.setLookAt(new Vector3D(-0.5, -1.2, 0.0));

        // ── divine side — hand and man ────────────────────────────────────────
        // god rays from above — steep angle, falls directly on the man
        Directionallight godRay1 = new Directionallight(new Vector3D( 0.05, 1.0, 0.1), new Color(255,200,80), 2.0);
        Directionallight godRay2 = new Directionallight(new Vector3D(-0.05, 1.0, 0.2), new Color(255,170,50), 1.0);
        // key light from above front — illuminates the man's face
        Pointlight keyMan = new Pointlight(new Vector3D(0.2,-5.0,-0.3), new Color(255,180,60), 3.0);
        // warm fill from the side
        Pointlight fillMan = new Pointlight(new Vector3D(0.8,-2.0,-1.0), new Color(255,140,30), 0.8);
        // golden ground bounce
        Pointlight bounceMan = new Pointlight(new Vector3D(0.2,-0.8, 0.5), new Color(255,160,60), 0.6);
        // golden halo behind the man — divine silhouette
        Pointlight haloMan1 = new Pointlight(new Vector3D(0.2,-2.0, 4.0), new Color(255,160,40), 1.2);
        Pointlight haloMan2 = new Pointlight(new Vector3D(0.2,-1.0, 4.5), new Color(255,140,20), 0.8);
        // hand light — divine origin
        Pointlight handKey  = new Pointlight(new Vector3D(0.3,-1.2,-0.3), new Color(180,200,240), 0.6);
        Pointlight handGlow = new Pointlight(new Vector3D(0.2,-1.5, 0.2), new Color(200,210,230), 0.5);
        // divine fire beneath the hand
        Pointlight handFire1 = new Pointlight(new Vector3D(0.3,-0.5, 2.0), new Color(160,180,220), 0.3);
        Pointlight handFire2 = new Pointlight(new Vector3D(0.6,-1.2,-0.5), new Color(180,190,210), 0.2);

        // ── hellish side — woman ──────────────────────────────────────────────
        // red spotlight from above — condemnation
        Pointlight hellKey  = new Pointlight(new Vector3D(-0.8,-5.0, 0.3), new Color(140,0,0), 4.0);
        Pointlight hellKey2 = new Pointlight(new Vector3D(-0.8,-4.0, 0.3), new Color(120,0,0), 3.5);
        // hellfire from below
        Pointlight hellFire1 = new Pointlight(new Vector3D(-0.8,-0.5, 0.5), new Color(160,0,0),  2.5);
        Pointlight hellFire2 = new Pointlight(new Vector3D(-1.1,-0.3, 1.0), new Color(140,0,10), 2.0);
        // dark halo behind the woman — purple/red
        Pointlight haloEvil1 = new Pointlight(new Vector3D(-0.8,-1.5, 4.5), new Color(100,0,20), 2.5);
        Pointlight haloEvil2 = new Pointlight(new Vector3D(-0.8,-0.5, 4.0), new Color(60,0,60),  2.0);
        Pointlight haloEvil3 = new Pointlight(new Vector3D(-0.8,-2.5, 4.0), new Color(80,0,10),  2.0);
        // cold blue — Exorcist-style fog
        Pointlight fog1 = new Pointlight(new Vector3D(-0.5,-2.0, 3.5), new Color(0,15,60), 2.0);
        Pointlight fog2 = new Pointlight(new Vector3D(-1.2,-1.5, 3.0), new Color(0,10,50), 1.5);
        // evil side purple
        Pointlight evilSide = new Pointlight(new Vector3D(-1.8,-1.5, 0.5), new Color(70,0,90), 2.0);
        // floor light pools — hell side
        Pointlight hellFloor = new Pointlight(new Vector3D(-0.8,-0.95,1.5), new Color(150,0,0),   2.0);
        // floor light pools — divine side
        Pointlight divFloor  = new Pointlight(new Vector3D( 0.2,-0.95,1.5), new Color(255,180,60), 1.5);

        Scene scene = new Scene(
                objectList.toArray(new Object3D[0]),
                new Ligth[]{
                        // divine
                        godRay1, godRay2,
                        keyMan, fillMan, bounceMan,
                        haloMan1, haloMan2,
                        handKey, handGlow, handFire1, handFire2,
                        divFloor,
                        // hellish
                        hellKey, hellKey2,
                        hellFire1, hellFire2,
                        haloEvil1, haloEvil2, haloEvil3,
                        fog1, fog2, evilSide,
                        hellFloor
                },
                BG_COLOR
        );
        scene.setBackground(BG_COLOR);

        new File(OUT_DIR).mkdirs();

        System.out.println("\n=== Preview 480x270 ===");
        BufferedImage preview = render(cam, scene, 480, 270);
        ImageIO.write(preview, "png", new File(OUT_DIR + "/preview.png"));
        System.out.println("Preview saved.");

        System.out.println("\n=== Render 4K 3840x2160 ===");
        BufferedImage render4k = render(cam, scene, 3840, 2160);
        ImageIO.write(render4k, "png", new File(OUT_DIR + "/render_4k.png"));
        System.out.println("4K saved: " + OUT_DIR + "/render_4k.png");
    }

    private Vector3D tr(Vector3D v, double cx, double cy, double cz, double scale) {
        return new Vector3D(-(v.x-cx)*scale, (v.y-cy)*scale, -(v.z-cz)*scale);
    }

    private float clampF(float v) { return Math.max(0f, Math.min(1f, v)); }

    private void loadMtl(String path, Map<String,Material> mats) {
        File f = new File(path);
        if (!f.exists()) { System.out.println("MTL not found: " + path); return; }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line, name = null;
            Color diff = new Color(200,195,190);
            double amb=0.25, spec=0.30, shin=32.0;
            String mapKd=null, mapBump=null, mapNs=null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] t = line.split("\\s+");
                switch (t[0]) {
                    case "newmtl":
                        if (name != null) saveMtl(mats, name, diff, amb, spec, shin, mapKd, mapBump, mapNs);
                        name=t[1]; diff=new Color(200,195,190); amb=0.25; spec=0.30; shin=32.0;
                        mapKd=null; mapBump=null; mapNs=null; break;
                    case "Kd":
                        if (t.length>=4) diff=new Color(clampF(Float.parseFloat(t[1])),
                                clampF(Float.parseFloat(t[2])), clampF(Float.parseFloat(t[3]))); break;
                    case "Ka":
                        if (t.length>=4) { amb=(Double.parseDouble(t[1])+Double.parseDouble(t[2])+Double.parseDouble(t[3]))/3.0;
                            amb=Math.max(0.05,Math.min(0.15,amb)); } break;
                    case "Ks":
                        if (t.length>=4) spec=(Double.parseDouble(t[1])+Double.parseDouble(t[2])+Double.parseDouble(t[3]))/3.0; break;
                    case "Ns":
                        try { shin=Math.max(1,Double.parseDouble(t[1])); } catch(Exception e2) {} break;
                    case "map_Kd":   mapKd   = parsePath(t); break;
                    case "map_Bump": case "bump": mapBump = parsePath(t); break;
                    case "map_Ns":   case "map_Pr": mapNs = parsePath(t); break;
                }
            }
            if (name != null) saveMtl(mats, name, diff, amb, spec, shin, mapKd, mapBump, mapNs);
        } catch (Exception e) { System.out.println("MTL error: " + e.getMessage()); }
    }

    private String parsePath(String[] t) {
        if (t.length < 2) return null;
        int si = 1;
        while (si < t.length-1 && t[si].startsWith("-")) si += 2;
        if (si >= t.length) return null;
        StringBuilder sb = new StringBuilder(t[si]);
        for (int i = si+1; i < t.length; i++) sb.append(" ").append(t[i]);
        return sb.toString();
    }

    private void saveMtl(Map<String,Material> mats, String name, Color diff,
                         double amb, double spec, double shin,
                         String mapKd, String mapBump, String mapNs) {
        double refl=0, refr=0;
        String nl = name.toLowerCase();
        if (nl.contains("marble") || nl.contains("stone") || nl.contains("rock")) { refl=0.55; spec=0.9; shin=180; }
        if (nl.equals("material.001")) { refl=0.20; spec=0.6; shin=80; amb=0.02; }
        mats.put(name, new Material(diff, amb, 0.7, spec, shin, refl, refr, 1.0));
        if (mapKd   != null) loadTexture(name, mapKd,   textureMap);
        if (mapBump != null) loadTexture(name, mapBump,  normalMap);
        if (mapNs   != null) loadTexture(name, mapNs,    roughMap);
    }

    private void loadTexture(String matName, String texPath, Map<String,Texture> map) {
        String ap = texPath.trim();
        while (ap.startsWith("-")) { int si=ap.indexOf(' '); if(si<0)break; ap=ap.substring(si+1).trim(); }
        String fn = new File(ap).getName();
        String[] cands = {
                ap,
                "C:/Raytracerrepo/final3/scene3/models/hand/"  + fn,
                "C:/Raytracerrepo/final3/scene3/models/men/"   + fn,
                "C:/Raytracerrepo/final3/scene3/models/women/" + fn,
                fn
        };
        for (String c : cands) {
            try {
                File tf = new File(c);
                if (tf.exists()) {
                    Texture tex = new Texture(c);
                    map.put(matName, tex);
                    String type = map==textureMap ? "Color" : map==normalMap ? "Normal" : "Rough";
                    System.out.println("  Texture OK [" + type + "]: " + matName + " -> " + fn);
                    return;
                }
            } catch (Exception e) {}
        }
        System.out.println("  Texture MISS: " + matName + " -> " + fn);
    }

    private BufferedImage render(Camera cam, Scene scene, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Cores: " + cores + " — " + w + "x" + h);

        ExecutorService pool = Executors.newFixedThreadPool(cores);
        AtomicInteger done = new AtomicInteger(0), hits = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(h);
        AtomicBoolean rendering = new AtomicBoolean(true);
        long renderStart = System.currentTimeMillis();

        // progress bar thread
        Thread pt = new Thread(() -> {
            while (rendering.get()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                int d=done.get(), pct=(d*100)/h;
                long el=(System.currentTimeMillis()-renderStart)/1000, eta=(d>0)?(el*(h-d))/d:0;
                int f2=pct/5;
                StringBuilder bar=new StringBuilder("[");
                for (int i=0;i<20;i++) bar.append(i<f2?"█":"░");
                bar.append("]");
                System.out.printf("\r%s %3d%%  %ds  ETA:%ds   ", bar, pct, el, eta);
            }
        });
        pt.setDaemon(true); pt.start();

        // 4x MSAA
        final double[][] AA  = {{0.25,0.25},{0.75,0.25},{0.25,0.75},{0.75,0.75}};
        final int        AAS = 4;

        for (int py=0; py<h; py++) {
            final int row = py;
            pool.submit(() -> {
                int rh = 0;
                for (int px=0; px<w; px++) {
                    double rr=0, rg=0, rb=0;
                    for (double[] off : AA) {
                        Ray ray = cam.generateRay(px, row, w, h, off[0], off[1]);
                        Intersection hit = scene.intersect(ray);
                        Color color = BG_COLOR;
                        if (hit != null) { rh++; color = shade(hit, scene.getLights(), scene, ray, 0); }
                        rr+=color.getRed(); rg+=color.getGreen(); rb+=color.getBlue();
                    }
                    img.setRGB(px, row, new Color(
                            (int)Math.min(255,rr/AAS),
                            (int)Math.min(255,rg/AAS),
                            (int)Math.min(255,rb/AAS)).getRGB());
                }
                hits.addAndGet(rh); done.incrementAndGet(); latch.countDown();
            });
        }

        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        rendering.set(false); pt.interrupt(); pool.shutdown();

        long ts = (System.currentTimeMillis()-renderStart)/1000;
        System.out.printf("%n[████████████████████] 100%%  Done in %ds%n", ts);
        System.out.printf("Intersections: %d / %d%n", hits.get(), w*h);
        System.out.println("Applying cinematic bloom...");
        return applyBloom(img, w, h, 0.55f, 8);
    }

    private BufferedImage applyBloom(BufferedImage src, int w, int h, float threshold, int radius) {
        // step 1 — isolate bright pixels
        int[][] bright = new int[w][h];
        for (int x=0;x<w;x++) for (int y=0;y<h;y++) {
            java.awt.Color c = new java.awt.Color(src.getRGB(x,y));
            float br = (c.getRed()+c.getGreen()+c.getBlue())/765.0f;
            bright[x][y] = br>threshold ? src.getRGB(x,y) : 0;
        }

        // step 2 — horizontal blur pass
        float[][] bR=new float[w][h], bG=new float[w][h], bB=new float[w][h];
        for (int x=0;x<w;x++) for (int y=0;y<h;y++) {
            float r=0,g=0,b=0,cnt=0;
            for (int dx=-radius;dx<=radius;dx++) {
                int nx=x+dx; if(nx<0||nx>=w)continue;
                r+=(bright[nx][y]>>16)&0xFF; g+=(bright[nx][y]>>8)&0xFF; b+=bright[nx][y]&0xFF; cnt++;
            }
            bR[x][y]=r/cnt; bG[x][y]=g/cnt; bB[x][y]=b/cnt;
        }

        // step 3 — vertical blur pass + composite back at 60% strength
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x=0;x<w;x++) for (int y=0;y<h;y++) {
            float r=0,g=0,b=0,cnt=0;
            for (int dy=-radius;dy<=radius;dy++) {
                int ny=y+dy; if(ny<0||ny>=h)continue;
                r+=bR[x][ny]; g+=bG[x][ny]; b+=bB[x][ny]; cnt++;
            }
            java.awt.Color orig = new java.awt.Color(src.getRGB(x,y));
            int fr=Math.min(255,orig.getRed()  +(int)(r/cnt*0.6f));
            int fg=Math.min(255,orig.getGreen()+(int)(g/cnt*0.6f));
            int fb=Math.min(255,orig.getBlue() +(int)(b/cnt*0.6f));
            out.setRGB(x, y, new java.awt.Color(fr,fg,fb).getRGB());
        }
        System.out.println("Bloom applied.");
        return out;
    }

    private Color shade(Intersection it, List<Ligth> lights, Scene scene, Ray inRay, int depth) {
        Material mat = it.getMaterial();
        if (mat == null) return Color.MAGENTA;

        // resolve material name for texture lookups
        String matName = null;
        for (Map.Entry<String,Material> e : materials.entrySet()) {
            if (e.getValue() == mat) { matName = e.getKey(); break; }
        }

        Color base = mat.getColor();
        Object3D obj = it.getObject();

        // color texture — check Triangle first, then the map
        Texture colorTex = (obj instanceof Triangle) ? ((Triangle)obj).getTexture() : null;
        if (colorTex == null && matName != null) colorTex = textureMap.get(matName);
        if (colorTex != null) base = colorTex.getColor(it.getU(), it.getV());

        Vector3D N = it.getNormal();

        // normal map perturbation
        if (matName != null && normalMap.containsKey(matName)) {
            Texture nTex = normalMap.get(matName);
            Color nc = nTex.getColor(it.getU(), it.getV());
            double nx=(nc.getRed()/255.0)*2.0-1.0, ny=(nc.getGreen()/255.0)*2.0-1.0, nz=(nc.getBlue()/255.0)*2.0-1.0;
            N = new Vector3D(N.x+nx*0.6, N.y+ny*0.6, N.z+nz*0.6).normalize();
        }

        double shin = mat.getShininess();

        // roughness map — reduces shininess
        if (matName != null && roughMap.containsKey(matName)) {
            Texture rTex = roughMap.get(matName);
            Color rc = rTex.getColor(it.getU(), it.getV());
            double rough = (rc.getRed()+rc.getGreen()+rc.getBlue())/765.0;
            shin = Math.max(4.0, shin*(1.0-rough*0.85));
        }

        double ak=mat.getAmbient(), dk=mat.getDiffuse(), sk=mat.getSpecular(), rk=mat.getReflection();
        double br=base.getRed()/255.0, bg=base.getGreen()/255.0, bb=base.getBlue()/255.0;

        double r=br*ak*0.3, g=bg*ak*0.3, b=bb*ak*0.3;

        Vector3D P    = it.getPoint();
        Vector3D Poff = new Vector3D(P.x+N.x*SHADOW_BIAS, P.y+N.y*SHADOW_BIAS, P.z+N.z*SHADOW_BIAS);
        Vector3D V    = new Vector3D(-inRay.getDirection().x, -inRay.getDirection().y, -inRay.getDirection().z).normalize();

        // direct lighting with 3-sample soft shadows
        for (Ligth light : lights) {
            double ndl = light.getNDotL(it);
            if (ndl <= 0) continue;
            Vector3D Ldir = light.getLightDirection(it);
            double lightDist = light.getDistance(it);
            double li=light.getIntensity(), lr=light.getColor().getRed()/255.0,
                    lg2=light.getColor().getGreen()/255.0, lb=light.getColor().getBlue()/255.0;
            Vector3D perp1 = Math.abs(Ldir.x)<0.9
                    ? new Vector3D(0,Ldir.z,-Ldir.y).normalize()
                    : new Vector3D(-Ldir.z,0,Ldir.x).normalize();
            Vector3D perp2 = Ldir.cross(perp1).normalize();
            double sf = 0;
            for (int s=0; s<3; s++) {
                double ang=s*2.094;
                double ox=Math.cos(ang)*0.03, oy=Math.sin(ang)*0.03;
                Vector3D j = new Vector3D(Ldir.x+perp1.x*ox+perp2.x*oy,
                        Ldir.y+perp1.y*ox+perp2.y*oy,
                        Ldir.z+perp1.z*ox+perp2.z*oy).normalize();
                if (!scene.isOccluded(new Ray(Poff,j), lightDist-SHADOW_BIAS)) sf += 0.333;
            }
            sf = 0.2 + sf*0.8;

            // diffuse (Lambertian)
            double diff = ndl*dk*li*sf;
            r+=br*diff*lr; g+=bg*diff*lg2; b+=bb*diff*lb;

            // specular (Blinn-Phong)
            if (sk>0 && sf>0.3) {
                Vector3D H = Ldir.add(V).normalize();
                double ndh = Math.max(N.dot(H), 0.0);
                double spec = Math.pow(ndh,shin)*sk*li*sf;
                r+=spec*lr; g+=spec*lg2; b+=spec*lb;
            }
        }

        // reflection
        if (rk>0 && depth<MAX_DEPTH) {
            Vector3D D  = inRay.getDirection();
            double   dn = D.dot(N);
            Vector3D rd = new Vector3D(D.x-2*dn*N.x, D.y-2*dn*N.y, D.z-2*dn*N.z).normalize();
            Intersection rh = scene.intersect(new Ray(Poff, rd));
            if (rh != null) {
                Color rc = shade(rh, lights, scene, new Ray(Poff,rd), depth+1);
                r+=rk*rc.getRed()/255.0; g+=rk*rc.getGreen()/255.0; b+=rk*rc.getBlue()/255.0;
            } else if (hdri != null) {
                Color ec = hdri.sample(rd.x, rd.y, rd.z);
                r+=rk*ec.getRed()/255.0; g+=rk*ec.getGreen()/255.0; b+=rk*ec.getBlue()/255.0;
            }
        }

        // gamma correction (gamma 2.2)
        r=Math.pow(Math.min(r,1.0),1.0/2.2);
        g=Math.pow(Math.min(g,1.0),1.0/2.2);
        b=Math.pow(Math.min(b,1.0),1.0/2.2);
        return new Color((float)Math.max(0,r), (float)Math.max(0,g), (float)Math.max(0,b));
    }

    public static void main(String[] args) throws Exception { new RayTracer(); }
}