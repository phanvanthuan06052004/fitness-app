package hcmute.edu.vn.phanVanThuan.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "step_counter.db";
    private static final int DATABASE_VERSION = 2;

    // Tên bảng và cột
    public static final String TABLE_STEPS = "steps";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STEP_COUNT = "step_count";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_DURATION = "duration";

    // Định dạng ngày tháng
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Câu lệnh tạo bảng
    private static final String CREATE_TABLE_STEPS = "CREATE TABLE " + TABLE_STEPS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_STEP_COUNT + " INTEGER,"
            + COLUMN_DATE + " TEXT,"
            + COLUMN_START_TIME + " TEXT,"
            + COLUMN_DURATION + " INTEGER"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_STEPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Sao lưu dữ liệu cũ
            db.execSQL("CREATE TABLE IF NOT EXISTS temp_steps AS SELECT * FROM " + TABLE_STEPS);
            
            // Xóa bảng cũ
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEPS);
            
            // Tạo bảng mới với cấu trúc mới
            db.execSQL(CREATE_TABLE_STEPS);
            
            // Khôi phục dữ liệu cũ (chỉ các cột tương thích)
            try {
                db.execSQL("INSERT INTO " + TABLE_STEPS + "(" + COLUMN_ID + ", " + COLUMN_STEP_COUNT + ", " + COLUMN_DATE + ") " +
                        "SELECT id, step_count, date FROM temp_steps");
            } catch (Exception e) {
                // Nếu có lỗi khi khôi phục dữ liệu, bỏ qua
            }
            
            // Xóa bảng tạm
            db.execSQL("DROP TABLE IF EXISTS temp_steps");
        }
    }

    // Kiểm tra xem cột có tồn tại trong bảng không
    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        boolean result = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameColumnIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameColumnIndex);
                    if (columnName.equalsIgnoreCase(name)) {
                        result = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking column exists: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    // Thêm dữ liệu bước chân mới
    public long insertStepData(StepData stepData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STEP_COUNT, stepData.getStepCount());
        values.put(COLUMN_DATE, dateFormat.format(stepData.getDate()));
        
        // Kiểm tra cột tồn tại trước khi thêm giá trị
        if (columnExists(db, TABLE_STEPS, COLUMN_START_TIME) && stepData.getStartTime() != null) {
            values.put(COLUMN_START_TIME, dateFormat.format(stepData.getStartTime()));
        }
        
        if (columnExists(db, TABLE_STEPS, COLUMN_DURATION)) {
            values.put(COLUMN_DURATION, stepData.getDurationInSeconds());
        }

        long id = db.insert(TABLE_STEPS, null, values);
        db.close();
        return id;
    }

    // Cập nhật dữ liệu bước chân
    public int updateStepData(StepData stepData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STEP_COUNT, stepData.getStepCount());
        values.put(COLUMN_DATE, dateFormat.format(stepData.getDate()));
        
        // Kiểm tra cột tồn tại trước khi thêm giá trị
        if (columnExists(db, TABLE_STEPS, COLUMN_START_TIME) && stepData.getStartTime() != null) {
            values.put(COLUMN_START_TIME, dateFormat.format(stepData.getStartTime()));
        }
        
        if (columnExists(db, TABLE_STEPS, COLUMN_DURATION)) {
            values.put(COLUMN_DURATION, stepData.getDurationInSeconds());
        }

        int rowsAffected = db.update(TABLE_STEPS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(stepData.getId())});
        db.close();
        return rowsAffected;
    }

    // Lấy tất cả dữ liệu bước chân
    public List<StepData> getAllStepData() {
        List<StepData> stepDataList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_STEPS + " ORDER BY " + COLUMN_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                int stepCount = cursor.getInt(cursor.getColumnIndex(COLUMN_STEP_COUNT));
                Date date = null;
                Date startTime = null;
                long duration = 0;
                
                try {
                    String dateStr = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
                    date = dateFormat.parse(dateStr);
                    
                    if (!cursor.isNull(cursor.getColumnIndex(COLUMN_START_TIME))) {
                        String startTimeStr = cursor.getString(cursor.getColumnIndex(COLUMN_START_TIME));
                        startTime = dateFormat.parse(startTimeStr);
                    }
                    
                    if (!cursor.isNull(cursor.getColumnIndex(COLUMN_DURATION))) {
                        duration = cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    // Xử lý trường hợp cột không tồn tại
                    e.printStackTrace();
                }

                StepData stepData = new StepData(id, stepCount, date, startTime, duration);
                stepDataList.add(stepData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return stepDataList;
    }

    // Lấy dữ liệu bước chân mới nhất
    public StepData getLatestStepData() {
        StepData stepData = null;
        String selectQuery = "SELECT * FROM " + TABLE_STEPS + " ORDER BY " + COLUMN_DATE + " DESC LIMIT 1";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            int stepCount = cursor.getInt(cursor.getColumnIndex(COLUMN_STEP_COUNT));
            Date date = null;
            Date startTime = null;
            long duration = 0;
            
            try {
                String dateStr = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
                date = dateFormat.parse(dateStr);
                
                if (!cursor.isNull(cursor.getColumnIndex(COLUMN_START_TIME))) {
                    String startTimeStr = cursor.getString(cursor.getColumnIndex(COLUMN_START_TIME));
                    startTime = dateFormat.parse(startTimeStr);
                }
                
                if (!cursor.isNull(cursor.getColumnIndex(COLUMN_DURATION))) {
                    duration = cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (Exception e) {
                // Xử lý trường hợp cột không tồn tại
                e.printStackTrace();
            }

            stepData = new StepData(id, stepCount, date, startTime, duration);
        }

        cursor.close();
        db.close();
        return stepData;
    }

    // Xóa tất cả dữ liệu bước chân
    public void deleteAllStepData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STEPS, null, null);
        db.close();
    }

    // Xóa tất cả các bản ghi có số bước = 0
    public void deleteZeroStepData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STEPS, COLUMN_STEP_COUNT + " = 0", null);
        db.close();
    }

    // Xóa các bản ghi trùng lặp
    public void deleteDuplicateData() {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Tạo bảng tạm để lưu các bản ghi duy nhất
        db.execSQL("CREATE TABLE IF NOT EXISTS temp_steps AS " +
                "SELECT MIN(id) as id, step_count, date, start_time, duration " +
                "FROM " + TABLE_STEPS + " " +
                "GROUP BY step_count, duration");
        
        // Xóa bảng cũ
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEPS);
        
        // Đổi tên bảng tạm thành bảng chính
        db.execSQL("ALTER TABLE temp_steps RENAME TO " + TABLE_STEPS);
        
        db.close();
    }
} 