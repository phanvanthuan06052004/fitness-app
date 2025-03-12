package hcmute.edu.vn.phanVanThuan.service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import hcmute.edu.vn.phanVanThuan.MainActivity;
import hcmute.edu.vn.phanVanThuan.R;
import hcmute.edu.vn.phanVanThuan.controller.StepController;

public class StepService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private int stepCount = 0;
    private int initialStepCount = 0;
    private boolean isInitialized = false;
    public static final String ACTION_STEP_COUNTER = "hcmute.edu.vn.phanVanThuan.ACTION_STEP_COUNTER";
    public static final String EXTRA_STEP_COUNT = "hcmute.edu.vn.phanVanThuan.EXTRA_STEP_COUNT";
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String STEP_COUNT_KEY = "stepCount";
    private static final String INITIAL_STEP_COUNT_KEY = "initialStepCount";
    
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "StepCounterChannel";
    
    private StepController stepController;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Khởi tạo controller
        stepController = new StepController(this);
        
        // Lấy dữ liệu đã lưu
        stepCount = stepController.getStepCount();
        initialStepCount = stepController.getInitialStepCount();
        
        // Lưu thời gian bắt đầu nếu chưa có
        if (stepController.getStartTime() == 0) {
            stepController.saveStartTime(System.currentTimeMillis());
        }
        
        // Khởi tạo cảm biến
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Log.e("StepService", "Step Counter Sensor is not available!");
        }

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        
        Log.d("StepService", "Service started! Loaded step count: " + stepCount);
        
        // Tạo notification channel (chỉ cần thiết cho Android 8.0+)
        createNotificationChannel();
        
        // Bắt đầu foreground service với notification
        startForeground(NOTIFICATION_ID, buildNotification(stepCount));
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hiển thị số bước chân hiện tại");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private android.app.Notification buildNotification(int steps) {
        // Tạo intent để mở ứng dụng khi nhấn vào notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        
        // Xây dựng notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fitness App")
                .setContentText("Số bước chân: " + steps)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    private void updateNotification(int steps) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(steps));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Gửi broadcast ngay khi service bắt đầu để cập nhật UI
        if (stepCount > 0) {
            Intent broadcastIntent = new Intent(ACTION_STEP_COUNTER);
            broadcastIntent.putExtra(EXTRA_STEP_COUNT, stepCount);
            sendBroadcast(broadcastIntent);
        }
        
        // Đảm bảo service không bị kill
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isInitialized) {
            // Lưu giá trị ban đầu từ cảm biến để tính toán số bước chân thực tế
            initialStepCount = (int) event.values[0];
            
            // Lưu giá trị ban đầu thông qua controller
            stepController.saveInitialStepCount(initialStepCount);
            
            isInitialized = true;
        }
        
        // Tính toán số bước chân thực tế (số bước chân hiện tại - số bước chân ban đầu + số bước chân đã lưu)
        int currentSteps = (int) event.values[0];
        int actualSteps = currentSteps - initialStepCount + stepCount;
        
        Log.d("StepService", "Steps: " + actualSteps + " (Sensor: " + currentSteps + ", Initial: " + initialStepCount + ")");
        
        // Lưu số bước chân thông qua controller
        stepController.saveStepCount(actualSteps);
        
        // Lưu dữ liệu vào cơ sở dữ liệu mỗi 100 bước và chỉ khi số bước > 0
        if (actualSteps > 0 && actualSteps % 100 == 0) {
            stepController.saveStepDataToDatabase(actualSteps);
        }
        
        // Cập nhật notification
        updateNotification(actualSteps);
        
        // Gửi broadcast với số bước chân
        Intent intent = new Intent(ACTION_STEP_COUNTER);
        intent.putExtra(EXTRA_STEP_COUNT, actualSteps);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        
        Log.d("StepService", "Service destroyed!");
    }
}
