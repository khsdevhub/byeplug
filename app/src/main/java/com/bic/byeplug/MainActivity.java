package com.bic.byeplug;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bic.byeplug.model.DeviceItem;
import com.bic.byeplug.ui.PowerStripDialogFragment;
import com.bic.byeplug.ui.SmartStripAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvDevices;
    private SmartStripAdapter adapter;
    private final List<DeviceItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvDevices = findViewById(R.id.rvDevices);

        // 1) 샘플 데이터 (나중에 '새 기기 추가하기'로 늘리면 됨)
        items.add(new DeviceItem("R4_POWER_0001", "책상 스탠드 멀티탭", "ONLINE"));
        items.add(new DeviceItem("R4_POWER_0002", "침대 옆 멀티탭", "OFFLINE"));

        // 2) Adapter + 클릭 콜백 인터페이스 연결
        adapter = new SmartStripAdapter(items, deviceId -> {
            PowerStripDialogFragment.newInstance(deviceId)
                    .show(getSupportFragmentManager(), "PowerStripDialog");
        });

        // 3) RecyclerView 설정
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);
    }
}
