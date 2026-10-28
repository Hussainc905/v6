package com.elyastech.elyasfun;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** True GLES2 mesh-based 3D stage. All geometry is offline; no CDN, network or analytics. */
public final class ThreeDSceneView extends GLSurfaceView {
    public static final long DURATION_MS = 52000L;
    private final Stage renderer;
    private final boolean preview;
    public ThreeDSceneView(Context context, boolean preview) {
        super(context);
        this.preview=preview;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,16,0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        if (!preview) setZOrderOnTop(true);
        setPreserveEGLContextOnPause(true);
        renderer=new Stage(context.getApplicationContext(),preview);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
    public void play() { renderer.restart(); }
    public void stop() { renderer.stop(); }
    private static final class Mesh {
        final FloatBuffer pos,norm,uv;
        final int count;
        Mesh(float[] p,float[] n,float[] u) {
            count=p.length/3;
            pos=buf(p);norm=buf(n);uv=buf(u);
        }
        private static FloatBuffer buf(float[] data) {
            FloatBuffer out=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            out.put(data).position(0);return out;
        }
    }
    private static class Geometry {
        private final java.util.ArrayList<Float> p=new java.util.ArrayList<>();
        private final java.util.ArrayList<Float> n=new java.util.ArrayList<>();
        private final java.util.ArrayList<Float> u=new java.util.ArrayList<>();
        void v(float x,float y,float z,float nx,float ny,float nz,float tx,float ty) {
            p.add(x);p.add(y);p.add(z);n.add(nx);n.add(ny);n.add(nz);u.add(tx);u.add(ty);
        }
        private static float[] array(java.util.ArrayList<Float> vals){
            float[] a=new float[vals.size()];for(int i=0;i<a.length;i++)a[i]=vals.get(i);return a;
        }
        Mesh result(){return new Mesh(array(p),array(n),array(u));}
        static Mesh cube() {
            Geometry g=new Geometry();
            float[][] faces={
                {0,0,1,  -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f},
                {0,0,-1,  0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f},
                {1,0,0,  0.5f,-0.5f,0.5f,0.5f,-0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f,0.5f,0.5f},
                {-1,0,0, -0.5f,-0.5f,-0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f,-0.5f},
                {0,1,0, -0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,-0.5f,-0.5f,0.5f,-0.5f},
                {0,-1,0, -0.5f,-0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f,-0.5f,0.5f,-0.5f,-0.5f,0.5f}
            };
            int[] idx={0,1,2,0,2,3};float[][] uv={{0,1},{1,1},{1,0},{0,0}};
            for(float[] f:faces){
                for(int k:idx){g.v(f[3+k*3],f[4+k*3],f[5+k*3],f[0],f[1],f[2],uv[k][0],uv[k][1]);}
            }
            return g.result();
        }
        static Mesh quad() {
            Geometry g=new Geometry();int[] ix={0,1,2,0,2,3};
            float[][] v={{-.5f,-.5f,0},{.5f,-.5f,0},{.5f,.5f,0},{-.5f,.5f,0}};
            float[][] tex={{0,1},{1,1},{1,0},{0,0}};
            for(int i:ix)g.v(v[i][0],v[i][1],v[i][2],0,0,1,tex[i][0],tex[i][1]);
            return g.result();
        }
        static Mesh sphere(){
            Geometry g=new Geometry();int rings=16,slices=20;
            for(int row=0;row<rings;row++)for(int col=0;col<slices;col++){
                sphereV(g,row,col,rings,slices);
                sphereV(g,row+1,col,rings,slices);
                sphereV(g,row+1,col+1,rings,slices);
                sphereV(g,row,col,rings,slices);
                sphereV(g,row+1,col+1,rings,slices);
                sphereV(g,row,col+1,rings,slices);
            }
            return g.result();
        }
        private static void sphereV(Geometry g,int r,int c,int rings,int slices){
            float theta=(float)Math.PI*r/rings;
            float phi=(float)(Math.PI*2*c/slices);
            float x=(float)(Math.sin(theta)*Math.cos(phi));
            float y=(float)Math.cos(theta);
            float z=(float)(Math.sin(theta)*Math.sin(phi));
            g.v(x*.5f,y*.5f,z*.5f,x,y,z,(float)c/slices,(float)r/rings);
        }

    }
    private static final class Stage implements Renderer {
        private static final String VERT="attribute vec3 aPos; attribute vec3 aNorm; attribute vec2 aUv; uniform mat4 uM; uniform mat4 uVP; varying vec3 vNorm; varying vec2 vUv; void main(){gl_Position=uVP*uM*vec4(aPos,1.0);vNorm=normalize(mat3(uM)*aNorm);vUv=aUv;}";
        private static final String FRAG="precision mediump float; varying vec3 vNorm; varying vec2 vUv; uniform vec4 uColor; uniform sampler2D uTexture; uniform int uTextured; void main(){vec4 base=(uTextured==1)?texture2D(uTexture,vUv)*uColor:uColor;float light=max(dot(normalize(vNorm),normalize(vec3(-0.35,0.75,0.78))),0.0);float intensity=0.48+0.52*light;gl_FragColor=vec4(base.rgb*intensity,base.a);}";
        private final Context ctx;
        private final boolean preview;
        private Mesh cube,sphere,quad;
        private int program,pa,na,ta,mLoc,vpLoc,colorLoc,texLoc,flagLoc,textureId;
        private final float[] m=new float[16],vp=new float[16],proj=new float[16],view=new float[16];
        private final ArrayDeque<float[]> stack=new ArrayDeque<>();
        private int viewportWidth,viewportHeight;
        private volatile long started=SystemClock.uptimeMillis();
        private volatile boolean animating=true;
        private float t,ground=-4.35f;
        Stage(Context context,boolean preview){ctx=context;this.preview=preview;}
        void restart(){started=SystemClock.uptimeMillis();animating=true;}
        void stop(){animating=false;}
        private static int shader(int type,String src){
            int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);
            int[] status=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,status,0);
            if(status[0]==0)throw new IllegalStateException("GL shader: "+GLES20.glGetShaderInfoLog(s));
            return s;
        }
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig conf){
            program=GLES20.glCreateProgram();
            GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,VERT));
            GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,FRAG));
            GLES20.glLinkProgram(program);
            int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
            if(ok[0]==0)throw new IllegalStateException("GL program: "+GLES20.glGetProgramInfoLog(program));
            pa=GLES20.glGetAttribLocation(program,"aPos");na=GLES20.glGetAttribLocation(program,"aNorm");
            ta=GLES20.glGetAttribLocation(program,"aUv");mLoc=GLES20.glGetUniformLocation(program,"uM");
            vpLoc=GLES20.glGetUniformLocation(program,"uVP");colorLoc=GLES20.glGetUniformLocation(program,"uColor");
            texLoc=GLES20.glGetUniformLocation(program,"uTexture");flagLoc=GLES20.glGetUniformLocation(program,"uTextured");
            cube=Geometry.cube();sphere=Geometry.sphere();quad=Geometry.quad();
            textureId=loadTexture();
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }
        private int loadTexture(){
            try(InputStream in=ctx.getAssets().open("child_face_texture.png")){
                Bitmap bmp=BitmapFactory.decodeStream(in);
                if(bmp==null)return 0;
                int[] id=new int[1];GLES20.glGenTextures(1,id,0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,id[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);bmp.recycle();return id[0];
            } catch(IOException exc){return 0;}
        }
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){
            viewportWidth=w;viewportHeight=h;
            GLES20.glViewport(0,0,w,h);
            float aspect=(float)w/Math.max(1,h);
            Matrix.orthoM(proj,0,-6.05f*aspect,6.05f*aspect,-6.05f,6.05f,1,45);
            Matrix.setLookAtM(view,0,0,0,20,0,0,0,0,1,0);
            Matrix.multiplyMM(vp,0,proj,0,view,0);
        }
        private void reset(){Matrix.setIdentityM(m,0);stack.clear();}
        private void push(){stack.push(m.clone());}
        private void pop(){float[] old=stack.pop();System.arraycopy(old,0,m,0,16);}
        private void xyz(float x,float y,float z){Matrix.translateM(m,0,x,y,z);}
        private void size(float x,float y,float z){Matrix.scaleM(m,0,x,y,z);}
        private void rot(float deg,float x,float y,float z){Matrix.rotateM(m,0,deg,x,y,z);}
        private void draw(Mesh mesh,int color,boolean textured){
            if(textured && textureId==0)return;
            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(mLoc,1,false,m,0);
            GLES20.glUniformMatrix4fv(vpLoc,1,false,vp,0);
            GLES20.glUniform4f(colorLoc,((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,((color>>>24)&255)/255f);
            GLES20.glUniform1i(flagLoc,textured?1:0);
            if(textured){GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);GLES20.glUniform1i(texLoc,0);}
            mesh.pos.position(0);mesh.norm.position(0);mesh.uv.position(0);
            GLES20.glEnableVertexAttribArray(pa);GLES20.glEnableVertexAttribArray(na);GLES20.glEnableVertexAttribArray(ta);
            GLES20.glVertexAttribPointer(pa,3,GLES20.GL_FLOAT,false,0,mesh.pos);
            GLES20.glVertexAttribPointer(na,3,GLES20.GL_FLOAT,false,0,mesh.norm);
            GLES20.glVertexAttribPointer(ta,2,GLES20.GL_FLOAT,false,0,mesh.uv);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,mesh.count);
        }
        private void box(float x,float y,float z,float sx,float sy,float sz,int col){push();xyz(x,y,z);size(sx,sy,sz);draw(cube,col,false);pop();}
        private void orb(float x,float y,float z,float sx,float sy,float sz,int col){push();xyz(x,y,z);size(sx*2,sy*2,sz*2);draw(sphere,col,false);pop();}
        private void bar(float x1,float y1,float z1,float x2,float y2,float z2,float width,int col){
            float dx=x2-x1,dy=y2-y1,dz=z2-z1;
            float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(length<.001f)return;
            push();xyz((x1+x2)/2f,(y1+y2)/2f,(z1+z2)/2f);
            // Rotate a Y-axis beam onto the segment in the XY plane.
            rot((float)-Math.toDegrees(Math.atan2(dx,dy)),0,0,1);
            size(width,length,width);draw(cube,col,false);pop();
        }
        private static float clamp(float x){return Math.max(0,Math.min(1,x));}
        private static float s(float x){x=clamp(x);return x*x*(3-2*x);}
        private static float blend(float a,float b,float x){return a+(b-a)*s(x);}
        @Override public void onDrawFrame(GL10 gl){
            if(preview)GLES20.glClearColor(.046f,.075f,.15f,1f);
            else GLES20.glClearColor(0,0,0,0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            if(!animating)return;
            float elapsed=(SystemClock.uptimeMillis()-started)/1000f;
            if(preview && elapsed>55){started=SystemClock.uptimeMillis();elapsed=0;}
            if(!preview && elapsed>52){animating=false;return;}
            t=elapsed;reset();
            drawStage();
        }
        private void drawStage(){
            if(preview){
                // 3D studio background only; overlay leaves the real phone desktop visible.
                box(0,0,-6,9,17,.05f,0xFF192D4D);
                orb(1.5f,2,-5.7f,1.7f,1.7f,.05f,0xFF294E73);
                box(0,ground-.35f,-2,9,.07f,5.0f,0xFF2E3444);
            }
            // Real 3D wooden door at lower left, hinged around vertical edge.
            drawDoor();
            drawSmudge(.05f,.50f,s((t-13.3f)/5.4f));
            drawSmudge(1.44f,.95f,s((t-34.3f)/5.0f));
            float firstX=-.20f,secondX=1.12f;
            float ladderX=firstX;
            float open=0;
            if(t<7.2f)ladderX=blend(-1.95f,firstX,(t-2.8f)/4.4f);
            else if(t<23.1f){open=s((t-7.2f)/2f)*(1-s((t-20.3f)/2.8f));}
            else if(t<27.6f){ladderX=blend(firstX,secondX,(t-23.1f)/4.5f);}
            else if(t<43){ladderX=secondX;open=s((t-27.6f)/1.9f)*(1-s((t-40.7f)/2.3f));}
            else ladderX=blend(secondX,-1.95f,(t-43)/7);
            if(t>=2.8f && t<50.2f)drawLadder(ladderX,open);
            if(t>=1.45f && t<50.5f){
                float x=-1.8f,y=ground;
                float wave=0,walking=0,wipe=0,annoyed=t<7.2f?1:0;
                if(t<7.2f){x=blend(-2.0f,-.7f,(t-1.45f)/5.75f);walking=1;}
                else if(t<9.2f){x=-.7f;}
                else if(t<13.3f){x=blend(-.7f,-.14f,(t-9.2f)/4.1f);y+=1.17f*s((t-9.2f)/4.1f);walking=1;}
                else if(t<18.7f){x=-.14f;y+=1.17f;wipe=1;}
                else if(t<21){x=blend(-.14f,-.7f,(t-18.7f)/2.3f);y+=1.17f*(1-s((t-18.7f)/2.3f));walking=1;}
                else if(t<23.1f){x=-.7f;}
                else if(t<27.6f){x=blend(-.7f,.48f,(t-23.1f)/4.5f);walking=1;}
                else if(t<29.5f){x=.48f;}
                else if(t<34.3f){x=blend(.48f,1.1f,(t-29.5f)/4.8f);y+=1.17f*s((t-29.5f)/4.8f);walking=1;}
                else if(t<39.3f){x=1.1f;y+=1.17f;wipe=1;}
                else if(t<41.5f){x=blend(1.1f,.5f,(t-39.3f)/2.2f);y+=1.17f*(1-s((t-39.3f)/2.2f));walking=1;}
                else if(t<44.2f){x=.45f;wave=s((t-41.5f)/.6f);}
                else{x=blend(.45f,-1.9f,(t-44.2f)/6.3f);walking=1;}
                drawChild(x,y,walking,wipe,wave,annoyed);
                if(wipe>0){
                    float cleanX=t<22?.13f:1.43f;
                    float yy=t<22?.63f:1.06f;
                    orb(cleanX+.21f*(float)Math.sin(t*7),yy+.18f*(float)Math.cos(t*7),.94f,.19f,.12f,.07f,0xFF2E9CD8);
                }
            }
        }
        private void drawSmudge(float x,float y,float progress){
            if(progress>=1)return;
            int alpha=(int)(150*(1-progress));
            // Non-photoreal fingerprints that fade as cloth wipes the two spots.
            for(int i=0;i<4;i++){
                float px=x+.23f*(float)Math.cos(i*2.1),py=y+.36f*(float)Math.sin(i*1.4);
                orb(px,py,-.8f,.16f,.25f,.025f,(alpha<<24)|0xA5C4D0);
            }
        }
        private void drawDoor(){
            float op=t<1.8f?s(t/1.8f):(t>49.4f?1-s((t-49.4f)/2f):1);
            float px=-2.14f;float py=ground+.89f;
            box(px,py,-1.05f,1.16f,2.0f,.2f,0xFF79523B);
            box(px,py,-.9f,.91f,1.78f,.08f,0xFFFFC985);
            push();xyz(px-.48f,0,-.72f);rot(-78*op,0,1,0);
            box(.48f,py,0,.96f,1.75f,.12f,0xFF995329);
            for(int i=0;i<3;i++)box(.19f+.25f*i,py,0.065f,.015f,1.64f,.02f,0xFFB97542);
            orb(.84f,py, .15f,.055f,.055f,.08f,0xFFE5BE65);
            pop();
        }
        private void drawLadder(float x,float amount){
            float spread=.38f*amount;
            float high=1.56f;
            for(int side=-1;side<=1;side+=2){
                float xx=x+side*.30f;
                bar(xx-spread*.45f,ground,-.32f,xx,ground+high,-.34f,.12f,0xFF9E643E);
                bar(xx+spread*.50f,ground,-.56f,xx,ground+high,-.39f,.12f,0xFF7D543F);
            }
            for(int i=1;i<=4;i++){
                float yy=ground+high*i/4f;
                box(x,yy,-.23f,.67f,.105f,.28f,0xFFC68F58);
            }
            box(x,ground+high+.01f,-.25f,.78f,.15f,.40f,0xFFDEAD6C);
            orb(x,ground+high,-.04f,.11f,.10f,.10f,0xFF353944);
        }
        private void drawChild(float x,float floor,float walking,float wiping,float waving,float grumpy){
            push();xyz(x,floor+.05f+(walking>0?.025f*(float)Math.sin(t*12):0),.48f);
            if(t>44.2f)rot(12,0,1,0);
            float swing=walking*(float)Math.sin(t*8.7f)*27f;
            int skin=0xFFF8C9A0, shirt=0xFFCBD0DE, pants=0xFFC0CBD8;
            // Legs pivot at hips, real 3D articulated geometry.
            for(int side=-1;side<=1;side+=2){
                push();xyz(side*.26f,.88f,0);rot(swing*side,1,0,0);
                box(0,-.29f,0,.31f,.65f,.35f,pants);
                orb(0,-.57f,.15f,.22f,.14f,.30f,0xFFEFE7DA);
                pop();
            }
            orb(0,1.21f,0,.47f,.60f,.33f,shirt);
            orb(0,1.07f,.05f,.46f,.35f,.34f,pants);
            // Short-sleeve arms with separate shoulder rotations and waving action.
            for(int side=-1;side<=1;side+=2){
                push();xyz(side*.40f,1.57f,.02f);
                if(waving>0 && side==1)rot(145+23*(float)Math.sin(t*11),0,0,1);
                else if(wiping>0 && side==1)rot(110+18*(float)Math.sin(t*7),0,0,1);
                else rot(side*(13+swing*.4f),0,0,1);
                orb(side*.13f,-.21f,0,.19f,.30f,.19f,shirt);
                orb(side*.16f,-.50f,.02f,.13f,.18f,.13f,skin);
                pop();
            }
            orb(0,2.00f,-.01f,.62f,.69f,.54f,skin);
            // Rear volume and side hair make head visibly 3D, facial portrait texture adds likeness.
            orb(-.05f,2.51f,-.1f,.59f,.23f,.52f,0xFF69462E);
            if(textureId!=0){
                push();xyz(0,2.02f,.56f);size(1.30f,1.48f,1);draw(quad,0xFFFFFFFF,true);pop();
            } else {
                orb(-.21f,2.09f,.53f,.09f,.11f,.05f,0xFF38291F);
                orb(.21f,2.09f,.53f,.09f,.11f,.05f,0xFF38291F);
            }
            if(grumpy>0){
                bar(-.34f,2.32f,.68f,-.10f,2.24f,.68f,.045f,0xFF4D3528);
                bar(.09f,2.24f,.68f,.32f,2.33f,.68f,.045f,0xFF4D3528);
            }
            pop();
        }
    }
}
