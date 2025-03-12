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
    private Button btnResetSteps;
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
        tvStepCount.setText("Steps: " + stepCount);
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
        btnResetSteps = findViewById(R.id.btnResetSteps);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnResetDatabase = findViewById(R.id.btnResetDatabase);
        lvHistory = findViewById(R.id.lvHistory);

        btnStartTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTracking();
            }
        });
        
        btnResetSteps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStepCount();
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
        builder.setTitle("Reset Database");
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
            btnStartTracking.setText("Start Tracking");
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
            btnViewHistory.setText("View History");
        }
        
        Toast.makeText(this, "Đã reset cơ sở dữ liệu thành công", Toast.LENGTH_SHORT).show();
    }
    
    // Thêm phương thức để xóa các bản ghi có số bước = 0
    private void cleanupZeroStepData() {
        stepController.deleteZeroStepData();
        
        // Tải lại lịch sử nếu đang hiển thị
        if (lvHistory.getVisibility() == View.VISIBLE) {
            loadStepHistory();
        }
    }
    
    private void toggleHistoryView() {
        if (lvHistory.getVisibility() == View.VISIBLE) {
            lvHistory.setVisibility(View.GONE);
            btnViewHistory.setText("View History");
        } else {
            // Xóa các bản ghi có số bước = 0 trước khi hiển thị lịch sử
            cleanupZeroStepData();
            
            loadStepHistory();
            lvHistory.setVisibility(View.VISIBLE);
            btnViewHistory.setText("Hide History");
        }
    }
    
    private void loadStepHistory() {
        List<StepData> allStepDataList = stepController.getAllStepData();
        List<StepData> stepDataList = new ArrayList<>();
        
        // Lọc bỏ các bản ghi có số bước = 0
        for (StepData stepData : allStepDataList) {
            if (stepData.getStepCount() > 0) {
                stepDataList.add(stepData);
            }
        }
        
        List<String> historyItems = new ArrayList<>();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (int i = 0; i < stepDataList.size(); i++) {
            StepData stepData = stepDataList.get(i);
            String dateStr = dateFormat.format(stepData.getDate());
            String formattedDuration = stepData.getFormattedDuration();
            
            StringBuilder item = new StringBuilder();
            item.append("Lần ").append(i + 1).append(" - ");
            item.append("Steps: ").append(stepData.getStepCount()).append(" bước - ");
            item.append(formattedDuration).append(" (");
            item.append(dateStr);
            
            if (stepData.getStartTime() != null) {
                String startTimeStr = timeFormat.format(stepData.getStartTime());
                item.append(" ").append(startTimeStr);
            }
            
            item.append(")");
            
            historyItems.add(item.toString());
        }
        
        if (historyItems.isEmpty()) {
            historyItems.add("Chưa có lịch sử bước chân");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, historyItems);
        lvHistory.setAdapter(adapter);
    }
    
    private void resetStepCount() {
        // Không cần lưu dữ liệu vì đã được lưu khi dừng service
        
        // Reset số bước chân thông qua controller
        stepController.resetStepCountWithoutSaving();
        
        // Cập nhật UI
        updateStepCountUI(0);
        
        Toast.makeText(this, "Số bước chân đã được reset về 0", Toast.LENGTH_SHORT).show();
        
        // Nếu service đang chạy, khởi động lại để cập nhật giá trị ban đầu
        if (isTracking) {
            Intent serviceIntent = new Intent(this, StepService.class);
            stopService(serviceIntent);
            startService(serviceIntent);
        }
        
        // Tải lại lịch sử nếu đang hiển thị
        if (lvHistory.getVisibility() == View.VISIBLE) {
            loadStepHistory();
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
                
                // Hiển thị thông báo
                Toast.makeText(this, "Đã lưu " + currentSteps + " bước chân", Toast.LENGTH_SHORT).show();
                
                // Tải lại lịch sử nếu đang hiển thị
                if (lvHistory.getVisibility() == View.VISIBLE) {
                    loadStepHistory();
                }
            }
            
            stopService(serviceIntent);
            Log.d("MainActivity", "Dừng StepService");
            btnStartTracking.setText("Start Tracking");
            
            // Reset số bước chân về 0 và đặt lại thời gian bắt đầu
            stepController.resetStepCount();
            updateStepCountUI(0);
        } else {
            // Đặt lại thời gian bắt đầu khi bắt đầu tracking
            stepController.saveStartTime(System.currentTimeMillis());
            
            startService(serviceIntent);
            Log.d("MainActivity", "Bắt đầu StepService");
            btnStartTracking.setText("Stop Tracking");
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
