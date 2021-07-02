package com.hibay.hbcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView textureView;
    private ImageView ivPic;
    private Button btnTakePic;
    private Camera mCamera;
    MTCNN mtcnn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
        mtcnn=new MTCNN(getAssets());
    }

    private void initListener() {
        textureView.setSurfaceTextureListener(this);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCallBack();
            }
        });

    }

    private void initData() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if(numberOfCameras<1){
            Toast.makeText(this, "没有相机", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initView() {
        textureView = (TextureView) findViewById(R.id.texture_view);
//        textureView.setRotation(0); // // 设置预览角度，并不改变获取到的原始数据方向(与Camera.setDisplayOrientation(0)相同)
        ivPic = (ImageView) findViewById(R.id.iv_pic);
        btnTakePic = (Button) findViewById(R.id.btn_takePic);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // 打开相机 0后置 1前置
        //添加权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            System.out.println("ok");
        }else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }

        mCamera = Camera.open(1);
        if (mCamera != null) {
            // 设置相机预览宽高，此处设置为TextureView宽高
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(width, height);
            // 设置自动对焦模式
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
            }
            try {
                mCamera.setDisplayOrientation(90);// 设置预览角度，并不改变获取到的原始数据方向
                // 绑定相机和预览的View
                mCamera.setPreviewTexture(surface);
                // 开始预览
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class NV21ToBitmap {
        private RenderScript rs;
        private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        private Type.Builder yuvType, rgbaType;
        private Allocation in, out;
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        public NV21ToBitmap(Context context) {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        }
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        public Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
            if (yuvType == null){
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(nv21);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            out.copyTo(bmpout);
            return bmpout;
        }
    }

    private void addCallBack() {
        if(mCamera!=null){
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    try{
                        long startTime= System.currentTimeMillis(); //起始时间

                        NV21ToBitmap ff=new NV21ToBitmap(getBaseContext()) ;
                        Bitmap bmp =ff.nv21ToBitmap(data,size.width, size.height);

                        //旋转
                        Matrix matrix = new Matrix();
                        // 缩放原图
                        matrix.postScale(1/8f, 1/8f);
                        // 向左旋转45度，参数为正则向右旋转
                        matrix.postRotate(-90);
                        //bmp.getWidth(), 500分别表示重绘后的位图宽高
                        Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                                matrix, true);

//                        long startTime= System.currentTimeMillis(); //起始时间
                        Vector<Box> boxes=mtcnn.detectFaces(dstbmp,100);
                        for (int i=0;i<boxes.size();i++){
                            Utils.drawRect(dstbmp,boxes.get(i).transform2Rect());
                            Utils.drawPoints(dstbmp,boxes.get(i).landmark);
                        }

                        long endTime = System.currentTimeMillis(); //结束时间

                        long runtime = endTime - startTime;
                        Log.i("hggg", String.format("方法使用时间 %d ms",runtime ));  //50 ms/f detect face:20ms/f

                        ivPic.setImageBitmap(dstbmp);


                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
}
