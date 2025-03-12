package hcmute.edu.vn.phanVanThuan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.phanVanThuan.service.StepService;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;

import hcmute.edu.vn.phanVanThuan.controller.StepCounterController;
import hcmute.edu.vn.phanVanThuan.view.MainView;

public class MainActivity extends AppCompatActivity {

    private MainView mainView;
    private StepCounterController stepCounterController;
    private StepCountReceiver stepCountReceiver;
    
    private class StepCountReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepService.ACTION_STEP_COUNTER.equals(intent.getAction())) {
                int stepCount = intent.getIntExtra(StepService.EXTRA_STEP_COUNT, 0);
                mainView.updateStepCountUI(stepCount);
            }
        }
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
        stepCounterController = new StepCounterController(this);
        
        // Khởi tạo view
        mainView = new MainView(this, findViewById(android.R.id.content));
        
        // Thiết lập sự kiện click cho các nút
        mainView.setStartTrackingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTracking();
            }
        });
        
        mainView.setViewHistoryClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHistoryView();
            }
        });
        
        mainView.setResetDatabaseClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetDatabaseConfirmation();
            }
        });
        
        // Khởi tạo receiver
        stepCountReceiver = new StepCountReceiver();
        
        // Hiển thị số bước chân đã lưu khi mở ứng dụng
        int savedSteps = stepCounterController.getCurrentStepCount();
        mainView.updateStepCountUI(savedSteps);
    }
    
    private void showResetDatabaseConfirmation() {
        mainView.showResetDatabaseConfirmation(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetDatabase();
            }
        });
    }
    
    private void resetDatabase() {
        // Reset cơ sở dữ liệu
        stepCounterController.resetDatabase();
        
        // Cập nhật UI
        mainView.updateStepCountUI(0);
        
        // Ẩn lịch sử nếu đang hiển thị
        if (mainView.isHistoryVisible()) {
            mainView.toggleHistoryVisibility(false);
        }
        
        // Cập nhật trạng thái nút
        mainView.updateTrackingButtonState(false);
        
        mainView.showToast("Đã reset cơ sở dữ liệu thành công");
    }
    
    private void toggleHistoryView() {
        boolean isHistoryVisible = mainView.isHistoryVisible();
        
        if (isHistoryVisible) {
            mainView.toggleHistoryVisibility(false);
        } else {
            // Xóa các bản ghi có số bước = 0 và các bản ghi trùng lặp trước khi hiển thị lịch sử
            stepCounterController.cleanupData();
            
            // Hiển thị lịch sử
            loadStepHistory();
            mainView.toggleHistoryVisibility(true);
        }
    }
    
    private void loadStepHistory() {
        // Lấy danh sách lịch sử bước chân đã lọc
        mainView.displayStepHistory(stepCounterController.getFilteredStepHistory());
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
        // Bắt đầu hoặc dừng theo dõi bước chân
        boolean isTracking = stepCounterController.toggleTracking();
        
        // Cập nhật UI
        mainView.updateTrackingButtonState(isTracking);
        
        if (!isTracking) {
            // Nếu đã dừng theo dõi, cập nhật số bước chân về 0
            mainView.updateStepCountUI(0);
            
            // Hiển thị thông báo
            int currentSteps = stepCounterController.getCurrentStepCount();
            if (currentSteps > 0) {
                mainView.showToast("Đã lưu " + currentSteps + " bước chân");
                
                // Tải lại lịch sử nếu đang hiển thị
                if (mainView.isHistoryVisible()) {
                    loadStepHistory();
                }
            }
        }
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
