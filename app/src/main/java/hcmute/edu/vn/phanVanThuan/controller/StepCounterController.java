package hcmute.edu.vn.phanVanThuan.controller;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hcmute.edu.vn.phanVanThuan.model.StepData;
import hcmute.edu.vn.phanVanThuan.service.StepService;

/**
 * Lớp quản lý logic theo dõi bước chân
 */
public class StepCounterController {
    private Context context;
    private StepController stepController;
    private boolean isTracking = false;

    public StepCounterController(Context context) {
        this.context = context;
        this.stepController = new StepController(context);
    }

    /**
     * Bắt đầu hoặc dừng theo dõi bước chân
     */
    public boolean toggleTracking() {
        Intent serviceIntent = new Intent(context, StepService.class);
        if (isTracking) {
            // Lưu dữ liệu trước khi dừng service
            int currentSteps = stepController.getStepCount();
            if (currentSteps > 0) {
                // Lưu dữ liệu vào cơ sở dữ liệu
                stepController.saveStepDataToDatabase(currentSteps);
                
                // Xóa các bản ghi trùng lặp sau khi lưu
                stepController.deleteDuplicateData();
            }
            
            context.stopService(serviceIntent);
            Log.d("StepCounterController", "Dừng StepService");
            
            // Reset số bước chân về 0 và đặt lại thời gian bắt đầu
            stepController.resetStepCount();
        } else {
            // Đặt lại thời gian bắt đầu khi bắt đầu tracking
            stepController.saveStartTime(System.currentTimeMillis());
            
            context.startService(serviceIntent);
            Log.d("StepCounterController", "Bắt đầu StepService");
        }
        
        isTracking = !isTracking;
        return isTracking;
    }

    /**
     * Lấy số bước chân hiện tại
     */
    public int getCurrentStepCount() {
        return stepController.getStepCount();
    }

    /**
     * Xóa các bản ghi có số bước = 0 và các bản ghi trùng lặp
     */
    public void cleanupData() {
        // Xóa các bản ghi có số bước = 0
        stepController.deleteZeroStepData();
        
        // Xóa các bản ghi trùng lặp
        stepController.deleteDuplicateData();
    }

    /**
     * Lấy danh sách lịch sử bước chân (đã lọc)
     */
    public List<StepData> getFilteredStepHistory() {
        List<StepData> allStepDataList = stepController.getAllStepData();
        List<StepData> stepDataList = new ArrayList<>();
        
        // Lọc bỏ các bản ghi có số bước = 0 và các bản ghi trùng lặp
        Set<String> uniqueEntries = new HashSet<>();
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
        
        return stepDataList;
    }

    /**
     * Reset cơ sở dữ liệu
     */
    public void resetDatabase() {
        // Dừng service nếu đang chạy
        if (isTracking) {
            Intent serviceIntent = new Intent(context, StepService.class);
            context.stopService(serviceIntent);
            isTracking = false;
        }
        
        // Xóa cơ sở dữ liệu
        context.deleteDatabase("step_counter.db");
        
        // Reset SharedPreferences
        stepController = new StepController(context);
        stepController.resetStepCount();
    }

    /**
     * Kiểm tra xem có đang theo dõi bước chân không
     */
    public boolean isTracking() {
        return isTracking;
    }

    /**
     * Thiết lập trạng thái theo dõi
     */
    public void setTracking(boolean tracking) {
        isTracking = tracking;
    }
} 