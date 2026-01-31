package com.bic.byeplug;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bic.byeplug.geo.GeofenceManager;
import com.bic.byeplug.geo.HomePrefs;
import com.bic.byeplug.model.DeviceItem;
import com.bic.byeplug.ui.PowerStripDialogFragment;
import com.bic.byeplug.ui.SmartStripAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ===== 기존 리스트 =====
    private RecyclerView rvDevices;
    private SmartStripAdapter adapter;
    private final List<DeviceItem> items = new ArrayList<>();

    // ===== GPS(집 등록/해제) UI =====
    private TextView tvHomeGeoStatus;
    private Button btnSetHome, btnClearHome;

    // ===== 위치/지오펜스 =====
    private FusedLocationProviderClient fused;
    private GeofenceManager geofenceManager;

    // 집 반경(추천: 80~120m)
    private static final float HOME_RADIUS_M = 100f;

    // 권한 요청 런처: FINE
    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (!fine) {
                    toast("위치 권한이 필요합니다.");
                    refreshHomeUi();
                    return;
                }
                // FINE OK → 집 등록 진행
                registerHomeWithCurrentLocation();
            });

    // 권한 요청 런처: BACKGROUND (Android 10+)
    private final ActivityResultLauncher<String> bgPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    toast("백그라운드 위치 권한이 없으면 자동 차단이 제한될 수 있어요.");
                }
                // 등록은 이미 되어도 되고, 다시 시도해도 됨
                tryRegisterGeofence();
                refreshHomeUi();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableImmersiveMode();

        // ===== 기존 RecyclerView =====
        rvDevices = findViewById(R.id.rvDevices);

        items.add(new DeviceItem("R4_POWER_0001", "책상 스탠드 멀티탭", "ONLINE"));
        items.add(new DeviceItem("R4_POWER_0002", "침대 옆 멀티탭", "OFFLINE"));

        adapter = new SmartStripAdapter(items, deviceId -> {
            PowerStripDialogFragment.newInstance(deviceId)
                    .show(getSupportFragmentManager(), "PowerStripDialog");
        });

        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);

        // ===== GPS(집 등록/해제) UI 연결 =====
        tvHomeGeoStatus = findViewById(R.id.tvHomeGeoStatus);
        btnSetHome = findViewById(R.id.btnSetHome);
        btnClearHome = findViewById(R.id.btnClearHome);

        // (XML에 아직 추가 안 했으면 여기서 NPE 날 수 있음)
        if (tvHomeGeoStatus == null || btnSetHome == null || btnClearHome == null) {
            toast("activity_main.xml에 tvHomeGeoStatus / btnSetHome / btnClearHome를 추가해 주세요.");
            return;
        }

        fused = LocationServices.getFusedLocationProviderClient(this);
        geofenceManager = new GeofenceManager(this);

        btnSetHome.setOnClickListener(v -> onClickSetHome());
        btnClearHome.setOnClickListener(v -> onClickClearHome());

        refreshHomeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableImmersiveMode();
    }

    // =========================
    // 집 등록/해제
    // =========================
    private void onClickSetHome() {
        if (!hasFineLocation()) {
            locationPermLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            return;
        }
        registerHomeWithCurrentLocation();
    }

    private void onClickClearHome() {
        geofenceManager.unregisterHomeGeofence();
        HomePrefs.clear(this);

        toast("집 자동 차단을 해제했습니다.");
        refreshHomeUi();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void registerHomeWithCurrentLocation() {
        if (!hasFineLocation()) {
            toast("위치 권한이 필요합니다.");
            return;
        }

        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        toast("현재 위치를 가져오지 못했어요. 잠시 후 다시 시도해 주세요.");
                        return;
                    }

                    double lat = loc.getLatitude();
                    double lng = loc.getLongitude();

                    // 1) 홈 저장
                    HomePrefs.save(this, lat, lng, HOME_RADIUS_M);

                    // 2) 지오펜스 등록
                    tryRegisterGeofence();

                    // 3) 백그라운드 권한(필요 시) 요청
                    ensureBackgroundLocationIfNeeded();

                    toast("집으로 등록했습니다.");
                    refreshHomeUi();
                })
                .addOnFailureListener(e -> toast("위치 조회 실패: " + e.getMessage()));
    }

    private void tryRegisterGeofence() {
        if (!HomePrefs.hasHome(this)) return;
        if (!hasFineLocation()) return;
        geofenceManager.registerHomeGeofence();
    }

    private void ensureBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return; // Android 9 이하는 불필요

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Android 10+ 백그라운드 위치 권한 요청
        bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    private boolean hasFineLocation() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshHomeUi() {
        if (tvHomeGeoStatus == null) return;

        boolean has = HomePrefs.hasHome(this);
        if (!has) {
            tvHomeGeoStatus.setText("집 자동 차단: 미설정");
            return;
        }

        double lat = HomePrefs.lat(this);
        double lng = HomePrefs.lng(this);
        float radius = HomePrefs.radius(this);

        tvHomeGeoStatus.setText("집 자동 차단: 설정됨 (반경 " + (int) radius + "m)\n"
                + "Home: " + String.format("%.5f", lat) + ", " + String.format("%.5f", lng));
    }

    private void toast(@NonNull String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // (옵션) 권한 거부가 계속되면 설정 화면 유도
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else { // Android 10 이하(Deprecated지만 동작)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

    }
}