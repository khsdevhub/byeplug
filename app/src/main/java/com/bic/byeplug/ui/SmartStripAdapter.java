package com.bic.byeplug.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bic.byeplug.R;
import com.bic.byeplug.model.DeviceItem;

import java.util.List;

public class SmartStripAdapter extends RecyclerView.Adapter<SmartStripAdapter.VH> {

    private final List<DeviceItem> items;
    private final OnDeviceClickListener listener;

    public SmartStripAdapter(List<DeviceItem> items, OnDeviceClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_smart_strip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DeviceItem item = items.get(position);

        holder.tvName.setText(item.name);
        holder.tvStatus.setText("Device: " + item.deviceId + " · 상태: " + item.status);

        // ✅ 카드(멀티탭 바) 전체 클릭 → Activity로 콜백
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(item.deviceId));
        // 만약 cardRoot id를 쓰고 싶으면:
        // holder.cardRoot.setOnClickListener(v -> listener.onDeviceClick(item.deviceId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        CardView cardRoot;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            // item_smart_strip.xml 루트에 android:id="@+id/cardRoot"가 있으면 사용 가능
            cardRoot = itemView.findViewById(R.id.cardRoot);
        }
    }
}
