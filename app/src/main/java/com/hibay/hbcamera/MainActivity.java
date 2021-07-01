package com.hibay.hbcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
//————————————————
//        版权声明：本文为CSDN博主「你这个橘子不要皮」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//        原文链接：https://blog.csdn.net/qq_40740256/article/details/84101015
//
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

    private void addCallBack() {
        if(mCamera!=null){
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    try{
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if(image!=null){
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 50, stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            //旋转
                            Matrix matrix = new Matrix();
                            // 缩放原图
                            matrix.postScale(1f, 1f);
                            // 向左旋转45度，参数为正则向右旋转
                            matrix.postRotate(-90);
                            //bmp.getWidth(), 500分别表示重绘后的位图宽高
                            Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                                    matrix, true);

                            Vector<Box> boxes=mtcnn.detectFaces(dstbmp,80);
                            for (int i=0;i<boxes.size();i++){
                                Utils.drawRect(dstbmp,boxes.get(i).transform2Rect());
                                Utils.drawPoints(dstbmp,boxes.get(i).landmark);
                            }

                            ivPic.setImageBitmap(dstbmp);
                            stream.close();
                        }
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
//————————————————
//        版权声明：本文为CSDN博主「再学HelloWorld」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//        原文链接：https://blog.csdn.net/qq_17441227/article/details/82877161