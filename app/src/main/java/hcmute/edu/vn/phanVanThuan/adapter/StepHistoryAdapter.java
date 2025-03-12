package hcmute.edu.vn.phanVanThuan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.phanVanThuan.R;
import hcmute.edu.vn.phanVanThuan.model.StepData;

public class StepHistoryAdapter extends ArrayAdapter<StepData> {
    
    private Context context;
    private List<StepData> stepDataList;
    
    // Định dạng ngày tháng
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    // Mảng tên các ngày trong tuần
    private static final String[] DAYS_OF_WEEK = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    
    public StepHistoryAdapter(Context context, List<StepData> stepDataList) {
        super(context, 0, stepDataList);
        this.context = context;
        this.stepDataList = stepDataList;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Lấy dữ liệu cho vị trí này
        StepData stepData = getItem(position);
        
        // Kiểm tra xem view đã được tái sử dụng chưa, nếu không thì inflate
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_step_history, parent, false);
        }
        
        // Lấy các view từ layout
        TextView tvStepCount = convertView.findViewById(R.id.tvStepCount);
        TextView tvDuration = convertView.findViewById(R.id.tvDuration);
        TextView tvDayOfWeek = convertView.findViewById(R.id.tvDayOfWeek);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        
        // Thiết lập dữ liệu cho các view
        if (stepData != null) {
            // Hiển thị số bước
            tvStepCount.setText(stepData.getStepCount() + " bước");
            
            // Hiển thị thời gian chạy
            String formattedDuration = stepData.getFormattedDuration();
            tvDuration.setText(formattedDuration);
            
            // Lấy thứ trong tuần
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(stepData.getDate());
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            tvDayOfWeek.setText(DAYS_OF_WEEK[dayOfWeek - 1]);
            
            // Hiển thị ngày tháng
            tvDate.setText(dateFormat.format(stepData.getDate()));
            
            // Hiển thị giờ
            if (stepData.getStartTime() != null) {
                tvTime.setText(timeFormat.format(stepData.getStartTime()));
            } else {
                tvTime.setText(timeFormat.format(stepData.getDate()));
            }
        }
        
        return convertView;
    }
} 