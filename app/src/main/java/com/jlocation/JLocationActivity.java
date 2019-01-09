package com.jlocation;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.animation.Animation;
import com.baidu.mapapi.animation.Transformation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.ArrayList;
import java.util.List;

public class JLocationActivity extends AppCompatActivity implements OnGetGeoCoderResultListener {

    public static final String TAG = "J_LOCATION";

    private Context mContext;
    private ImageView mLocationBtn;
    private RecyclerView mNearbyLocationListView;
    private NearbyLocationListAdapter mNearbyLocationAdapter;
    private View mSelectLocationBtn;
    private View mBackBtn;

    private GeoCoder mSearch = null;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private Marker mMarker;
    private LatLng mLocation;
    private LocationClient mLocClient;

    BitmapDescriptor mMarkDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_mark);

    private boolean mAllowReverseGeo = true;
    private PoiInfo mSelectedLocation = null;

    private selectLocationListener mSelectLocationListener = new selectLocationListener() {
        @Override
        public void onSelectLocation(PoiInfo pInfo) {
            mSelectedLocation = pInfo;
            if (pInfo != null) {
                MapStatus mapStatus = new MapStatus.Builder().target(pInfo.location).build();
                MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
                mBaiduMap.animateMapStatus(mMapStatusUpdate);
                mAllowReverseGeo = false;
            }
        }
    };

    private Point mScreenCenterPoint;

    public LocationListener mLocationListener = new LocationListener();
    private int mCurrentDirection = 0;
    private MyLocationData locData;
    boolean isFirstLoc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarFullTransparent();

        setContentView(R.layout.layout_baidu_location);

        mContext = getApplicationContext();
        mLocationBtn = (ImageView) findViewById(R.id.start_locate_btn);
        mLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocation();
            }
        });

        mNearbyLocationListView = (RecyclerView) findViewById(R.id.nearby_location_list);
        mNearbyLocationListView.setLayoutManager(new LinearLayoutManager(mContext));
        NormDividerDecoration dividerDecoration = new NormDividerDecoration.Builder(mContext)
                .setItemDividerHeight(1f)
                .setColor(Color.parseColor("#CBCBCB"))
                .build(false, false, true);
        mNearbyLocationListView.addItemDecoration(dividerDecoration);
        mNearbyLocationAdapter = new NearbyLocationListAdapter(mContext, mSelectLocationListener);
        mNearbyLocationListView.setAdapter(mNearbyLocationAdapter);

        mSelectLocationBtn = findViewById(R.id.select_location_btn);
        mSelectLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedLocation != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("name", mSelectedLocation.name);
                    bundle.putString("province", mSelectedLocation.province);
                    bundle.putString("city", mSelectedLocation.city);
                    bundle.putString("address", mSelectedLocation.address);
                    LatLng location = mSelectedLocation.location;
                    bundle.putDouble("lat", location.latitude);
                    bundle.putDouble("lng", location.longitude);
                    setResult(RESULT_OK, new Intent().putExtra("location", bundle));
                } else {
                    setResult(RESULT_OK, new Intent());
                }
                finish();
            }
        });

        mBackBtn = findViewById(R.id.back_btn);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        //禁用3D楼宇特效
        mBaiduMap.getUiSettings().setOverlookingGesturesEnabled(false);

        //隐藏百度图标
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView)) {
            child.setVisibility(View.GONE);
        }

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);

        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if (null != mBaiduMap.getMapStatus()) {
                    mLocation = mBaiduMap.getMapStatus().target;
                    mScreenCenterPoint = mBaiduMap.getProjection().toScreenLocation(mLocation);
                    MarkerOptions ooF = new MarkerOptions().position(mLocation).icon(mMarkDescriptor).perspective(true)
                            .fixedScreenPosition(mScreenCenterPoint);
                    mMarker = (Marker) (mBaiduMap.addOverlay(ooF));

                    onReverseGeoCode(mLocation);
                }
            }
        });

        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus status) {

            }

            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {

            }

            @Override
            public void onMapStatusChange(MapStatus status) {

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus status) {
                if (null == mMarker) {
                    return;
                }

                mLocation = status.target;
                mMarker.setAnimation(getTransformationPoint());
                mMarker.startAnimation();

                if (mAllowReverseGeo) {
                    onReverseGeoCode(mLocation);
                } else {
                    mAllowReverseGeo = true;
                }
            }
        });

        startLocation();

        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(12.0f);
        mBaiduMap.setMapStatus(msu);
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mContext, R.string.reverse_geo_code_failed, Toast.LENGTH_LONG).show();
            return;
        }

        List<PoiInfo> poiList = result.getPoiList();
        mNearbyLocationAdapter.updateLatestNearbyLocation(poiList);
        // set first poi as default select location
        if (poiList != null && !poiList.isEmpty()) {
            mSelectedLocation = poiList.get(0);
        }

        // print poi info list
        List<PoiInfo> poiInfoList = result.getPoiList();
        if (poiInfoList != null) {
            for (PoiInfo info : poiInfoList) {
                Log.e(TAG, "locName: " + info.name);
            }
            Log.e(TAG, " ");
        }
    }

    /**
     * 定位SDK监听函数
     */
    public class LocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }

            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    protected void setStatusBarFullTransparent() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= 19) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        // 设置状态栏黑色文字、图标
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    /**
     * 创建平移坐标动画
     */
    private Animation getTransformationPoint() {
        if (null != mScreenCenterPoint) {
            Point pointTo = new Point(mScreenCenterPoint.x, mScreenCenterPoint.y - 100);
            Transformation mTransforma = new Transformation(mScreenCenterPoint, pointTo, mScreenCenterPoint);
            mTransforma.setDuration(500);
            mTransforma.setRepeatMode(Animation.RepeatMode.RESTART);//动画重复模式
            mTransforma.setRepeatCount(1);//动画重复次数
            mTransforma.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart() {
                }

                @Override
                public void onAnimationEnd() {
                }

                @Override
                public void onAnimationCancel() {
                }

                @Override
                public void onAnimationRepeat() {

                }
            });
            return mTransforma;
        }

        return null;
    }

    @Override
    protected void onPause() {
        // MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        // MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mMarker != null) {
            mMarker.cancelAnimation();
        }
        // MapView的生命周期与Activity同步，当activity销毁时需调用MapView.destroy()
        mMapView.onDestroy();
        markerRemove();
        super.onDestroy();
        // 回收 bitmap 资源
        mMarkDescriptor.recycle();
    }

    public void markerRemove() {

    }

    private void onReverseGeoCode(LatLng ll) {
        int version = 1;//返回新数据
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ll).newVersion(version).radius(500));
    }

    private void startLocation() {
        isFirstLoc = true;
        mLocClient = new LocationClient(getApplicationContext());
        mLocClient.registerLocationListener(mLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        mLocClient.setLocOption(option);
        mLocClient.start();
    }

    class NearbyLocationListAdapter extends RecyclerView.Adapter<NearbyLocationListAdapter.LocationItemHolder> {

        private Context appContext;
        private LayoutInflater inflater;
        private List<PoiInfo> nearbyLocationList = new ArrayList<>();
        private int curSelectLocationIndex = 1;
        private selectLocationListener selectListener;

        public NearbyLocationListAdapter(Context context, selectLocationListener listener) {
            appContext = context.getApplicationContext();
            inflater = LayoutInflater.from(appContext);
            selectListener = listener;
        }

        @Override
        public LocationItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.layout_location_list_item, null);
            final LocationItemHolder holder = new LocationItemHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    curSelectLocationIndex = position;
                    if (selectListener != null) {
                        if (position == 0) {
                            selectListener.onSelectLocation(null);
                        } else {
                            PoiInfo pInfo = nearbyLocationList.get(position - 1);
                            selectListener.onSelectLocation(pInfo);
                        }
                    }
                    notifyDataSetChanged();
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(LocationItemHolder holder, int position) {
            if (position == 0) {
                holder.setLocationName(getString(R.string.hide_location_title));
                holder.setLocationAddr(getString(R.string.hide_location_summary));
                holder.setSelect(position == curSelectLocationIndex);
            } else {
                PoiInfo pInfo = nearbyLocationList.get(position - 1);
                if (pInfo != null) {
                    String locName = pInfo.name;
                    if (position == 1) {
                        locName = getString(R.string.current_location, locName);
                    }
                    holder.setLocationName(locName);
                    holder.setLocationAddr(pInfo.address);
                    holder.setSelect(position == curSelectLocationIndex);
                }
            }
        }

        @Override
        public int getItemCount() {
            if (nearbyLocationList == null || nearbyLocationList.isEmpty()) {
                return 0;
            }
            return nearbyLocationList.size() + 1;
        }

        public void updateLatestNearbyLocation(List<PoiInfo> list) {
            curSelectLocationIndex = 1;
            nearbyLocationList = list;
            notifyDataSetChanged();
        }

        class LocationItemHolder extends RecyclerView.ViewHolder {

            private TextView locationName;
            private TextView locationAddr;
            private ImageView selectView;

            public LocationItemHolder(View itemView) {
                super(itemView);
                locationName = (TextView) itemView.findViewById(R.id.name);
                locationAddr = (TextView) itemView.findViewById(R.id.address);
                selectView = (ImageView) itemView.findViewById(R.id.select);
            }

            public void setLocationName(String name) {
                locationName.setText(name);
            }

            public void setLocationAddr(String address) {
                locationAddr.setText(address);
            }

            public void setSelect(boolean isSelect) {
                if (isSelect) {
                    selectView.setVisibility(View.VISIBLE);
                } else {
                    selectView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    interface selectLocationListener {
        void onSelectLocation(PoiInfo pInfo);
    }
}
