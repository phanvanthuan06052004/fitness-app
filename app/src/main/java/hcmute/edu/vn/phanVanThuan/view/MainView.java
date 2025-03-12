package hcmute.edu.vn.phanVanThuan.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import hcmute.edu.vn.phanVanThuan.R;
import hcmute.edu.vn.phanVanThuan.model.StepData;

/**
 * Lớp quản lý giao diện của MainActivity
 */
public class MainView {
    private Context context;
    private TextView tvStepCount;
    private Button btnStartTracking;
    private Button btnViewHistory;
    private Button btnResetDatabase;
    private HistoryView historyView;

    public MainView(Context context, View rootView) {
        this.context = context;
        
        // Khởi tạo các thành phần giao diện
        tvStepCount = rootView.findViewById(R.id.tvStepCount);
        btnStartTracking = rootView.findViewById(R.id.btnStartTracking);
        btnViewHistory = rootView.findViewById(R.id.btnViewHistory);
        btnResetDatabase = rootView.findViewById(R.id.btnResetDatabase);
        
        // Khởi tạo HistoryView
        ListView lvHistory = rootView.findViewById(R.id.lvHistory);
        historyView = new HistoryView(context, lvHistory);
    }

    /**
     * Cập nhật hiển thị số bước chân
     */
    public void updateStepCountUI(int stepCount) {
        tvStepCount.setText("Bước chân: " + stepCount);
    }

    /**
     * Hiển thị hộp thoại xác nhận xóa dữ liệu
     */
    public void showResetDatabaseConfirmation(DialogInterface.OnClickListener onConfirmListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xóa dữ liệu");
        builder.setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu và tạo lại cơ sở dữ liệu không?");
        builder.setPositiveButton("Có", onConfirmListener);
        builder.setNegativeButton("Không", null);
        builder.show();
    }

    /**
     * Hiển thị thông báo
     */
    public void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Cập nhật trạng thái nút bắt đầu/dừng theo dõi
     */
    public void updateTrackingButtonState(boolean isTracking) {
        btnStartTracking.setText(isTracking ? "Dừng theo dõi" : "Bắt đầu theo dõi");
    }

    /**
     * Cập nhật trạng thái hiển thị lịch sử
     */
    public void toggleHistoryVisibility(boolean show) {
        historyView.setVisible(show);
        btnViewHistory.setText(show ? "Ẩn lịch sử" : "Xem lịch sử");
    }

    /**
     * Hiển thị danh sách lịch sử bước chân
     */
    public void displayStepHistory(List<StepData> stepDataList) {
        historyView.displayStepHistory(stepDataList);
    }

    /**
     * Kiểm tra xem lịch sử có đang hiển thị không
     */
    public boolean isHistoryVisible() {
        return historyView.isVisible();
    }

    /**
     * Thiết lập sự kiện click cho nút bắt đầu/dừng theo dõi
     */
    public void setStartTrackingClickListener(View.OnClickListener listener) {
        btnStartTracking.setOnClickListener(listener);
    }

    /**
     * Thiết lập sự kiện click cho nút xem lịch sử
     */
    public void setViewHistoryClickListener(View.OnClickListener listener) {
        btnViewHistory.setOnClickListener(listener);
    }

    /**
     * Thiết lập sự kiện click cho nút xóa dữ liệu
     */
    public void setResetDatabaseClickListener(View.OnClickListener listener) {
        btnResetDatabase.setOnClickListener(listener);
    }
} 