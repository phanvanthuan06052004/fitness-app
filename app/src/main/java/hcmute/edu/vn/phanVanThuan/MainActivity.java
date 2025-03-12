package hcmute.edu.vn.phanVanThuan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.phanVanThuan.service.StepService;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.content.DialogInterface;

import hcmute.edu.vn.phanVanThuan.controller.StepController;
import hcmute.edu.vn.phanVanThuan.model.StepData;
import hcmute.edu.vn.phanVanThuan.model.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private TextView tvStepCount;
    private Button btnStartTracking;
    private Button btnViewHistory;
    private Button btnResetDatabase;
    private ListView lvHistory;
    private boolean isTracking = false;
    private StepCountReceiver stepCountReceiver;
    private StepController stepController;
    
    private class StepCountReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepService.ACTION_STEP_COUNTER.equals(intent.getAction())) {
                int stepCount = intent.getIntExtra(StepService.EXTRA_STEP_COUNT, 0);
                updateStepCountUI(stepCount);
            }
        }
    }
    
    private void updateStepCountUI(int stepCount) {
        tvStepCount.setText("Bước chân: " + stepCount);
    }
    
    private void checkPermissions() {
        // Danh sách quyền cần kiểm tra
        String[] permissions = {
                Manifest.permission.ACTIVITY_RECOGNITION // Quyền theo dõi bước chân
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e("PERMISSION", "Chưa có quyền: " + permission);
                ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
            } else {
                Log.d("PERMISSION", "Đã có quyền: " + permission);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkPermissions();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo controller
        stepController = new StepController(this);

        tvStepCount = findViewById(R.id.tvStepCount);
        btnStartTracking = findViewById(R.id.btnStartTracking);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnResetDatabase = findViewById(R.id.btnResetDatabase);
        lvHistory = findViewById(R.id.lvHistory);

        btnStartTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTracking();
            }
        });
        
        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHistoryView();
            }
        });
        
        btnResetDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetDatabaseConfirmation();
            }
        });
        
        // Khởi tạo receiver
        stepCountReceiver = new StepCountReceiver();
        
        // Hiển thị số bước chân đã lưu khi mở ứng dụng
        int savedSteps = stepController.getStepCount();
        updateStepCountUI(savedSteps);
    }
    
    private void showResetDatabaseConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xóa dữ liệu");
        builder.setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu và tạo lại cơ sở dữ liệu không?");
        builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetDatabase();
            }
        });
        builder.setNegativeButton("Không", null);
        builder.show();
    }
    
    private void resetDatabase() {
        // Dừng service nếu đang chạy
        if (isTracking) {
            Intent serviceIntent = new Intent(this, StepService.class);
            stopService(serviceIntent);
            isTracking = false;
            btnStartTracking.setText("Bắt đầu theo dõi");
        }
        
        // Xóa cơ sở dữ liệu
        Context context = getApplicationContext();
        context.deleteDatabase("step_counter.db");
        
        // Tạo lại DatabaseHelper
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        databaseHelper.close();
        
        // Reset SharedPreferences
        stepController = new StepController(this);
        stepController.resetStepCount();
        
        // Cập nhật UI
        updateStepCountUI(0);
        
        // Ẩn lịch sử nếu đang hiển thị
        if (lvHistory.getVisibility() == View.VISIBLE) {
            lvHistory.setVisibility(View.GONE);
            btnViewHistory.setText("Xem lịch sử");
        }
        
        Toast.makeText(this, "Đã reset cơ sở dữ liệu thành công", Toast.LENGTH_SHORT).show();
    }
    
    // Thêm phương thức để xóa các bản ghi có số bước = 0 và các bản ghi trùng lặp
    private void cleanupZeroStepData() {
        // Xóa các bản ghi có số bước = 0
        stepController.deleteZeroStepData();
        
        // Xóa các bản ghi trùng lặp
        stepController.deleteDuplicateData();
        
        // Tải lại lịch sử nếu đang hiển thị
        if (lvHistory.getVisibility() == View.VISIBLE) {
            loadStepHistory();
        }
    }
    
    private void toggleHistoryView() {
        if (lvHistory.getVisibility() == View.VISIBLE) {
            lvHistory.setVisibility(View.GONE);
            btnViewHistory.setText("Xem lịch sử");
        } else {
            // Xóa các bản ghi có số bước = 0 và các bản ghi trùng lặp trước khi hiển thị lịch sử
            cleanupZeroStepData();
            
            loadStepHistory();
            lvHistory.setVisibility(View.VISIBLE);
            btnViewHistory.setText("Ẩn lịch sử");
        }
    }
    
    private void loadStepHistory() {
        List<StepData> allStepDataList = stepController.getAllStepData();
        List<StepData> stepDataList = new ArrayList<>();
        
        // Lọc bỏ các bản ghi có số bước = 0 và các bản ghi trùng lặp
        java.util.Set<String> uniqueEntries = new java.util.HashSet<>();
        for (StepData stepData : allStepDataList) {
            if (stepData.getStepCount() > 0) {
                // Tạo một chuỗi duy nhất đại diện cho bản ghi này (số bước + thời gian)
                String uniqueKey = stepData.getStepCount() + "_" + stepData.getFormattedDuration();
                if (!uniqueEntries.contains(uniqueKey)) {
                    uniqueEntries.add(uniqueKey);
                    stepDataList.add(stepData);
                }
            }
        }
        
        if (stepDataList.isEmpty()) {
            // Nếu không có dữ liệu, hiển thị thông báo
            List<String> emptyMessage = new ArrayList<>();
            emptyMessage.add("Chưa có lịch sử bước chân");
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_list_item_1, emptyMessage);
            lvHistory.setAdapter(emptyAdapter);
        } else {
            // Sử dụng adapter tùy chỉnh để hiển thị dữ liệu
            hcmute.edu.vn.phanVanThuan.adapter.StepHistoryAdapter adapter = 
                    new hcmute.edu.vn.phanVanThuan.adapter.StepHistoryAdapter(this, stepDataList);
            lvHistory.setAdapter(adapter);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký receiver khi activity hiển thị
        IntentFilter filter = new IntentFilter(StepService.ACTION_STEP_COUNTER);
        registerReceiver(stepCountReceiver, filter);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký receiver khi activity không hiển thị
        try {
            unregisterReceiver(stepCountReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver có thể chưa được đăng ký
            Log.e("MainActivity", "Receiver not registered: " + e.getMessage());
        }
    }

    private void toggleTracking() {
        Intent serviceIntent = new Intent(this, StepService.class);
        if (isTracking) {
            // Lưu dữ liệu trước khi dừng service
            int currentSteps = stepController.getStepCount();
            if (currentSteps > 0) {
                // Lưu dữ liệu vào cơ sở dữ liệu
                stepController.saveStepDataToDatabase(currentSteps);
                
                // Xóa các bản ghi trùng lặp sau khi lưu
                stepController.deleteDuplicateData();
                
                // Hiển thị thông báo
                Toast.makeText(this, "Đã lưu " + currentSteps + " bước chân", Toast.LENGTH_SHORT).show();
                
                // Tải lại lịch sử nếu đang hiển thị
                if (lvHistory.getVisibility() == View.VISIBLE) {
                    loadStepHistory();
                }
            }
            
            stopService(serviceIntent);
            Log.d("MainActivity", "Dừng StepService");
            btnStartTracking.setText("Bắt đầu theo dõi");
            
            // Reset số bước chân về 0 và đặt lại thời gian bắt đầu
            stepController.resetStepCount();
            updateStepCountUI(0);
        } else {
            // Đặt lại thời gian bắt đầu khi bắt đầu tracking
            stepController.saveStartTime(System.currentTimeMillis());
            
            startService(serviceIntent);
            Log.d("MainActivity", "Bắt đầu StepService");
            btnStartTracking.setText("Dừng theo dõi");
        }
        isTracking = !isTracking;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION", "Người dùng đã cấp quyền: " + permissions[i]);
                } else {
                    Log.e("PERMISSION", "Người dùng từ chối quyền: " + permissions[i]);
                }
            }
        }
    }

}
