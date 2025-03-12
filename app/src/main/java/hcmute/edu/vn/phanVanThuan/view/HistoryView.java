package hcmute.edu.vn.phanVanThuan.view;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.phanVanThuan.adapter.StepHistoryAdapter;
import hcmute.edu.vn.phanVanThuan.model.StepData;

/**
 * Lớp quản lý giao diện hiển thị lịch sử bước chân
 */
public class HistoryView {
    private Context context;
    private ListView lvHistory;

    public HistoryView(Context context, ListView lvHistory) {
        this.context = context;
        this.lvHistory = lvHistory;
    }

    /**
     * Hiển thị danh sách lịch sử bước chân
     */
    public void displayStepHistory(List<StepData> stepDataList) {
        if (stepDataList.isEmpty()) {
            // Nếu không có dữ liệu, hiển thị thông báo
            List<String> emptyMessage = new ArrayList<>();
            emptyMessage.add("Chưa có lịch sử bước chân");
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_list_item_1, emptyMessage);
            lvHistory.setAdapter(emptyAdapter);
        } else {
            // Sử dụng adapter tùy chỉnh để hiển thị dữ liệu
            StepHistoryAdapter adapter = new StepHistoryAdapter(context, stepDataList);
            lvHistory.setAdapter(adapter);
        }
    }

    /**
     * Kiểm tra xem lịch sử có đang hiển thị không
     */
    public boolean isVisible() {
        return lvHistory.getVisibility() == View.VISIBLE;
    }

    /**
     * Hiển thị hoặc ẩn lịch sử
     */
    public void setVisible(boolean visible) {
        lvHistory.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
} 