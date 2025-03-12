package hcmute.edu.vn.phanVanThuan.controller;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.List;

import hcmute.edu.vn.phanVanThuan.model.DatabaseHelper;
import hcmute.edu.vn.phanVanThuan.model.StepData;

public class StepController {
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String STEP_COUNT_KEY = "stepCount";
    private static final String INITIAL_STEP_COUNT_KEY = "initialStepCount";
    private static final String START_TIME_KEY = "startTime";
    
    private Context context;
    private DatabaseHelper databaseHelper;
    
    // Biến để theo dõi thời gian lưu dữ liệu gần nhất
    private long lastSaveTime = 0;
    
    public StepController(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    // Lưu số bước chân vào SharedPreferences
    public void saveStepCount(int stepCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(STEP_COUNT_KEY, stepCount);
        editor.apply();
    }
    
    // Lấy số bước chân từ SharedPreferences
    public int getStepCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(STEP_COUNT_KEY, 0);
    }
    
    // Lưu giá trị ban đầu từ cảm biến
    public void saveInitialStepCount(int initialStepCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(INITIAL_STEP_COUNT_KEY, initialStepCount);
        editor.apply();
    }
    
    // Lấy giá trị ban đầu từ cảm biến
    public int getInitialStepCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(INITIAL_STEP_COUNT_KEY, 0);
    }
    
    // Lưu thời gian bắt đầu
    public void saveStartTime(long startTimeMillis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(START_TIME_KEY, startTimeMillis);
        editor.apply();
    }
    
    // Lấy thời gian bắt đầu
    public long getStartTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(START_TIME_KEY, 0);
    }
    
    // Reset số bước chân
    public void resetStepCount() {
        // Lưu dữ liệu hiện tại vào cơ sở dữ liệu trước khi reset
        int currentStepCount = getStepCount();
        if (currentStepCount > 0) {
            long startTimeMillis = getStartTime();
            Date startTime = startTimeMillis > 0 ? new Date(startTimeMillis) : null;
            StepData currentStepData = new StepData(currentStepCount, startTime);
            databaseHelper.insertStepData(currentStepData);
        }
        
        // Reset trong SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(STEP_COUNT_KEY, 0);
        editor.putLong(START_TIME_KEY, new Date().getTime()); // Đặt lại thời gian bắt đầu
        editor.apply();
    }
    
    // Reset số bước chân mà không lưu dữ liệu
    public void resetStepCountWithoutSaving() {
        // Reset trong SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(STEP_COUNT_KEY, 0);
        editor.putLong(START_TIME_KEY, new Date().getTime()); // Đặt lại thời gian bắt đầu
        editor.apply();
    }
    
    // Lưu dữ liệu bước chân vào cơ sở dữ liệu
    public long saveStepDataToDatabase(int stepCount) {
        // Kiểm tra xem dữ liệu đã được lưu trong 5 giây gần đây hay chưa
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime < 5000) {
            // Đã lưu gần đây, không lưu lại
            return -1;
        }
        
        long startTimeMillis = getStartTime();
        Date startTime = startTimeMillis > 0 ? new Date(startTimeMillis) : null;
        StepData stepData = new StepData(stepCount, startTime);
        long id = databaseHelper.insertStepData(stepData);
        
        // Cập nhật thời gian lưu gần nhất
        lastSaveTime = currentTime;
        
        return id;
    }
    
    // Lấy tất cả dữ liệu bước chân từ cơ sở dữ liệu
    public List<StepData> getAllStepData() {
        return databaseHelper.getAllStepData();
    }
    
    // Lấy dữ liệu bước chân mới nhất từ cơ sở dữ liệu
    public StepData getLatestStepData() {
        return databaseHelper.getLatestStepData();
    }
    
    // Xóa tất cả dữ liệu bước chân
    public void deleteAllStepData() {
        databaseHelper.deleteAllStepData();
    }
    
    // Xóa tất cả các bản ghi có số bước = 0
    public void deleteZeroStepData() {
        databaseHelper.deleteZeroStepData();
    }
    
    // Xóa các bản ghi trùng lặp
    public void deleteDuplicateData() {
        databaseHelper.deleteDuplicateData();
    }
} 