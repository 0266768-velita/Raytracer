import java.awt.Color;

class Triangle implements Object3D {

    // shared vertex, normal and UV pools — filled once by RayTracer before any triangles are created
    public static double[] GVX, GVY, GVZ;
    public static double[] GNX, GNY, GNZ;
    public static float[]  GUU, GUV;

    // per-triangle indices into the pools
    private final int vi0, vi1, vi2;
    private final int ni0, ni1, ni2;  // -1 means no interpolated normal
    private final int ti0, ti1, ti2;  // -1 means no UV

    // precomputed face normal
    private final double fnx, fny, fnz;

    private Material material;
    private Texture  texture;

    // legacy constructor — color only
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        this(v0,v1,v2,null,null,null,null,null,null,Material.diffuse(color),null);
    }

    // legacy constructor — material only
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Material mat) {
        this(v0,v1,v2,null,null,null,null,null,null,mat,null);
    }

    // full legacy constructor — embeds coords directly into the global pools
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2,
                    Vector3D n0, Vector3D n1, Vector3D n2,
                    Vector2D uv0, Vector2D uv1, Vector2D uv2,
                    Material mat, Texture tex) {
        this.vi0=embedV(v0); this.vi1=embedV(v1); this.vi2=embedV(v2);
        this.ni0=(n0!=null)?embedN(n0):-1;
        this.ni1=(n1!=null)?embedN(n1):-1;
        this.ni2=(n2!=null)?embedN(n2):-1;
        this.ti0=(uv0!=null)?embedT(uv0):-1;
        this.ti1=(uv1!=null)?embedT(uv1):-1;
        this.ti2=(uv2!=null)?embedT(uv2):-1;
        this.material=mat; this.texture=tex;
        double[] fn=calcFN(vi0,vi1,vi2);
        fnx=fn[0]; fny=fn[1]; fnz=fn[2];
    }

    // index-based constructor — most efficient, used by RayTracer when loading OBJ
    public Triangle(int vi0, int vi1, int vi2,
                    int ni0, int ni1, int ni2,
                    int ti0, int ti1, int ti2,
                    Material mat, Texture tex) {
        this.vi0=vi0; this.vi1=vi1; this.vi2=vi2;
        this.ni0=ni0; this.ni1=ni1; this.ni2=ni2;
        this.ti0=ti0; this.ti1=ti1; this.ti2=ti2;
        this.material=mat; this.texture=tex;
        double[] fn=calcFN(vi0,vi1,vi2);
        fnx=fn[0]; fny=fn[1]; fnz=fn[2];
    }

    // allocate the global pools — call this before creating any triangles
    public static void initPools(int nVerts, int nNorms, int nUVs) {
        GVX=new double[nVerts]; GVY=new double[nVerts]; GVZ=new double[nVerts];
        GNX=new double[nNorms]; GNY=new double[nNorms]; GNZ=new double[nNorms];
        GUU=new float[nUVs];    GUV=new float[nUVs];
    }

    // running insert positions for each pool
    public static int vTop=0, nTop=0, tTop=0;

    // insert a vertex into the pool and return its index
    private static int embedV(Vector3D v) {
        int i=vTop++;
        GVX[i]=v.x; GVY[i]=v.y; GVZ[i]=v.z; return i;
    }

    // insert a normal into the pool and return its index
    private static int embedN(Vector3D n) {
        int i=nTop++;
        GNX[i]=n.x; GNY[i]=n.y; GNZ[i]=n.z; return i;
    }

    // insert a UV coordinate into the pool and return its index
    private static int embedT(Vector2D uv) {
        int i=tTop++;
        GUU[i]=(float)uv.getU(); GUV[i]=(float)uv.getV(); return i;
    }

    // compute the normalized face normal from three pool indices
    private static double[] calcFN(int i0, int i1, int i2) {
        double e1x=GVX[i1]-GVX[i0], e1y=GVY[i1]-GVY[i0], e1z=GVZ[i1]-GVZ[i0];
        double e2x=GVX[i2]-GVX[i0], e2y=GVY[i2]-GVY[i0], e2z=GVZ[i2]-GVZ[i0];
        double cx=e1y*e2z-e1z*e2y, cy=e1z*e2x-e1x*e2z, cz=e1x*e2y-e1y*e2x;
        double l=Math.sqrt(cx*cx+cy*cy+cz*cz); if(l<1e-12)l=1;
        return new double[]{cx/l, cy/l, cz/l};
    }

    // Möller–Trumbore intersection — all math on primitives to avoid Vector3D allocations
    @Override
    public Intersection intersect(Ray ray) {
        final double EPS=1e-7;
        double ax=GVX[vi0], ay=GVY[vi0], az=GVZ[vi0];
        double e1x=GVX[vi1]-ax, e1y=GVY[vi1]-ay, e1z=GVZ[vi1]-az;
        double e2x=GVX[vi2]-ax, e2y=GVY[vi2]-ay, e2z=GVZ[vi2]-az;
        double rdx=ray.getDirection().x, rdy=ray.getDirection().y, rdz=ray.getDirection().z;
        double hx=rdy*e2z-rdz*e2y, hy=rdz*e2x-rdx*e2z, hz=rdx*e2y-rdy*e2x;
        double a=e1x*hx+e1y*hy+e1z*hz;
        if(a>-EPS&&a<EPS) return null; // ray parallel to triangle
        double f=1.0/a;
        double rox=ray.getOrigin().x, roy=ray.getOrigin().y, roz=ray.getOrigin().z;
        double sx=rox-ax, sy=roy-ay, sz=roz-az;
        double u=f*(sx*hx+sy*hy+sz*hz);
        if(u<0||u>1) return null;
        double qx=sy*e1z-sz*e1y, qy=sz*e1x-sx*e1z, qz=sx*e1y-sy*e1x;
        double v=f*(rdx*qx+rdy*qy+rdz*qz);
        if(v<0||u+v>1) return null;
        double t=f*(e2x*qx+e2y*qy+e2z*qz);
        if(t<=EPS) return null; // intersection behind the ray origin

        Vector3D point=ray.getPoint(t);
        double nx, ny, nz;
        if(ni0>=0) {
            // interpolate smooth normal using barycentric coords
            double w=1-u-v;
            nx=GNX[ni0]*w+GNX[ni1]*u+GNX[ni2]*v;
            ny=GNY[ni0]*w+GNY[ni1]*u+GNY[ni2]*v;
            nz=GNZ[ni0]*w+GNZ[ni1]*u+GNZ[ni2]*v;
            double nl=Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(nl>1e-12){nx/=nl;ny/=nl;nz/=nl;}
        } else {
            // fall back to the flat face normal and flip if needed
            nx=fnx; ny=fny; nz=fnz;
            if(nx*rdx+ny*rdy+nz*rdz>0){nx=-nx;ny=-ny;nz=-nz;}
        }

        double texU=0, texV=0;
        Color    finalColor=material.getColor();
        Material hitMat=material;
        if(texture!=null&&ti0>=0) {
            // interpolate UV and sample the texture
            double w=1-u-v;
            texU=GUU[ti0]*w+GUU[ti1]*u+GUU[ti2]*v;
            texV=GUV[ti0]*w+GUV[ti1]*u+GUV[ti2]*v;
            finalColor=texture.getColor(texU,texV);
            hitMat=new Material(finalColor,material.getAmbient(),material.getDiffuse(),
                    material.getSpecular(),material.getShininess(),
                    material.getReflection(),material.getRefraction(),
                    material.getRefractiveIndex());
        }
        Intersection result=new Intersection(t,point,new Vector3D(nx,ny,nz),hitMat,texU,texV);
        result.setObject(this);
        return result;
    }

    @Override public Material getMaterial()        {return material;}
    @Override public void setMaterial(Material m)  {this.material=m;}
    public Texture  getTexture()                   {return texture;}
    public void     setTexture(Texture t)          {this.texture=t;}
    public Vector3D getFaceNormal()                {return new Vector3D(fnx,fny,fnz);}

    public Vector3D[] getVertices() {
        return new Vector3D[]{
                new Vector3D(GVX[vi0],GVY[vi0],GVZ[vi0]),
                new Vector3D(GVX[vi1],GVY[vi1],GVZ[vi1]),
                new Vector3D(GVX[vi2],GVY[vi2],GVZ[vi2])};
    }
}