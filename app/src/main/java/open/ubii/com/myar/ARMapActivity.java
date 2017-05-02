package open.ubii.com.myar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.amap.api.maps.MapView;
import com.vuforia.State;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import open.ubii.com.myar.utils.SampleApplicationControl;
import open.ubii.com.myar.utils.SampleApplicationException;
import open.ubii.com.myar.utils.SampleApplicationSession;

public class ARMapActivity extends AppCompatActivity implements SensorEventListener {
    // ---------------界面相关------------
	/* 摄像层浏览控件 . */
    protected TextureView mCamera;

    /* 摄像层控制 . */
    protected CameraController mCameraController;

    /* 屏幕控制 . */
    private PowerManager.WakeLock mWakeLock;

    /* 传感器管理器 . */
    protected SensorManager mSensorMag;

    /* 加速度传感器. */
    protected Sensor mGravSensor;

    /* 磁场传感器. */
    protected Sensor mMagSensor;

    /* 传感器速率 . */
    protected int mDelayRate = SensorManager.SENSOR_DELAY_NORMAL;

    /* 加速度传感器数据. */
    protected float[] mGrav = new float[3];
    protected float[] mGravSensorVals = new float[3];

    /* 磁场传感器数据. */
    protected float[] mMag = new float[3];
    protected float[] mMagSensorVals = new float[3];

    /* 旋转矩阵 . */
    protected float[] mRotation = new float[9];
    protected float[] mRTmp = new float[9];

    /* 地图层. */
    private View mMapFrame;

    /* 地图层视图 . */
    private MapView mMapView;

    /* 旋转矩阵的方向向量. */
    protected volatile float[] mOrientation = new float[3];
    /* 地图移动锁 . */
    private AtomicBoolean mMapMoveToLock = new AtomicBoolean(true);

    SampleApplicationSession vuforiaAppSession;

    private ArrayList<String> mDatasetStrings = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorMag = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器
        // 初始化视图
        // 地图层
        mMapFrame = findViewById(R.id.frameLayout_map);
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        // 实景层
        mCamera = (TextureView) findViewById(R.id.textureView_camera);
        mCameraController = new CameraController(this, mCamera);
        mCameraController.initPreview(ScreenUtils.getScreenWidth(this), ScreenUtils.getScreenHeight(this),
                getWindowManager().getDefaultDisplay().getRotation());
        mCameraController
                .setCameraTextureListener(mCameraController.new CameraTextureListener());
        // 屏幕控制
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "DimScreen");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 获取加速度传感器
        List<Sensor> mSensors = mSensorMag
                .getSensorList(Sensor.TYPE_ACCELEROMETER);

        if (mSensors.size() > 0) {
            mGravSensor = mSensors.get(0);
        }

        // 获取磁场加速度传感器
        mSensors = mSensorMag.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);

        if (mSensors.size() > 0) {
            mMagSensor = mSensors.get(0);
        }

        // 注册传感器监听
        mSensorMag.registerListener(this, mGravSensor, mDelayRate);

        mSensorMag.registerListener(this, mMagSensor, mDelayRate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorMag.unregisterListener(this, mGravSensor);
        mSensorMag.unregisterListener(this, mMagSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(Sensor.TYPE_ACCELEROMETER == sensorEvent.sensor.getType()){
            mGravSensorVals = LowPassFilter.filter(0.5f, 1.0f,
                    sensorEvent.values.clone(), mGravSensorVals);
            mGrav[0] = mGravSensorVals[0];
            mGrav[1] = mGravSensorVals[1];
            mGrav[2] = mGravSensorVals[2];
        }else if (Sensor.TYPE_MAGNETIC_FIELD == sensorEvent.sensor.getType()){
            mMagSensorVals = LowPassFilter.filter(2.0f, 4.0f,
                    sensorEvent.values.clone(), mMagSensorVals);
            mMag[0] = mMagSensorVals[0];
            mMag[1] = mMagSensorVals[1];
            mMag[2] = mMagSensorVals[2];
        }
        if(null != mGravSensorVals && null != mMagSensorVals){
            SensorManager.getRotationMatrix(mRTmp, null, mGrav, mMag);

            // 重新规定传感器坐标系
            switch (getRotation()) {
                case Surface.ROTATION_0://一般状态
                    SensorManager.remapCoordinateSystem(mRTmp,
                            SensorManager.AXIS_X, SensorManager.AXIS_Y, mRotation);
                    break;
                case Surface.ROTATION_90: // 横屏 顶部向左
                    SensorManager.remapCoordinateSystem(mRTmp,
                            SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                            mRotation);
                    break;
                case Surface.ROTATION_180: // 横屏 顶部向右
                    break;
                case Surface.ROTATION_270: // 竖屏 顶部向下
                    break;
            }
            // 获取方向向量
            SensorManager.getOrientation(mRotation, mOrientation);
            // 回调
            onSensorAccess();
        }


    }

    public int getRotation() {
        return  getWindowManager().getDefaultDisplay().getRotation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (null == sensor){
            throw new NullPointerException();
        }
        if (Sensor.TYPE_MAGNETIC_FIELD == sensor.getType()
                && SensorManager.SENSOR_STATUS_UNRELIABLE == accuracy) {
        }
    }

    // ------------------------ 传感器数据处理------------------------
    /**
     * 传感器数据处理
     */
    protected synchronized void onSensorAccess() {
        float pitch = (float) ((mOrientation[1] * 180) / Math.PI);
        // 当设备水平放置时触发
        if (pitch >= -15 && pitch <= 25) {
            inHorizontal(true);
        } else {
            inHorizontal(false);
        }
    }
    /**
     * 设备水平放置回调
     */
    private void inHorizontal(boolean isHorizontal) {
        if (isHorizontal) {
            // 显示地图
            mMapFrame.setVisibility(View.VISIBLE);
            mMapView.postInvalidate();
        }else{
            // 不显示地图
            mMapFrame.setVisibility(View.INVISIBLE);
            mMapMoveToLock.set(false);
        }
    }










}
