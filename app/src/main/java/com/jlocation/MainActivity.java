package com.jlocation;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_START_LOCATION = 0x0001;

    private Button mStartLocateBtn;
    private TextView mLocateResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartLocateBtn = findViewById(R.id.start_locate_btn);
        mLocateResultText = findViewById(R.id.location_result_text);

        mStartLocateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JLocationActivity.class);
                startActivityForResult(intent, REQUEST_CODE_START_LOCATION);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE_START_LOCATION) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = null;
                if (intent != null && (bundle = intent.getBundleExtra("location")) != null) {
                    String name = bundle.getString("name", "");
                    String province = bundle.getString("province", "");
                    String city = bundle.getString("city", "");
                    String address = bundle.getString("address", "");
                    double lat = bundle.getDouble("lat", -1);
                    double lng = bundle.getDouble("lng", -1);
                    String locationText = getString(R.string.fix_location_result, name, province, city, address, String.valueOf(lat), String.valueOf(lng));
                    mLocateResultText.setText(locationText);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Location canceled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
