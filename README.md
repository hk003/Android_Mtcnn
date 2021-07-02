# Android_Mtcnn
MTCNN For Android Java in video detection. This project is the Android implementaion of MTCNN face detection.
MTCNN For Android Java
主要参考MTCNN论文和Facenet中MTCNN的实现，纯Java实现。为了方便java调用，我先把npy转为pb。

编译环境：Android Studio4.5
核心类MTCNN用法 (MTCNN.Java)
类实例化 MTCNN mtcnn=new MTCNN(getAssets())
只有1个API：public Vector detectFaces(Bitmap bitmap,int minFaceSize)
参数bitmap：要处理的图片
参数minFaceSize：最小的脸像素值，一般>=40。越大则检测速度越快，但会忽略掉较小的脸
返回值:所有的脸的Box，包括left/right/top/bottom/landmark(一共5个点，嘴巴鼻子眼)

视频中检测人脸，不依赖任何opencv等第三方库。。

Result:
![Screenshot_2021-07-02-11-30](https://user-images.githubusercontent.com/17959876/124216894-c890b180-db29-11eb-9c0f-4d906e424afd.png)


加载项目，点击按钮，检测人脸并画框和关键点。
必须加依赖：
dependencies {
    compile 'org.tensorflow:tensorflow-android:+'
}


参考：https://github.com/vcvycy/MTCNN4Android#mtcnn4android-1


