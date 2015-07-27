package com.keti.wearable.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;

public class MyActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        NodeApi.NodeListener,
        MessageApi.MessageListener,
        DataApi.DataListener {

    private TextView mTextView; // 텍스트를 출력할 뷰
    private View mLayout; // 배경을 출력할 레이아웃

    private GoogleApiClient mGoogleApiClient; // 구글 플레이 서비스 API 객체

    @Override // Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.keti.wearable.activity.R.layout.activity_my);
        final WatchViewStub stub = (WatchViewStub) findViewById(com.keti.wearable.activity.R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(com.keti.wearable.activity.R.id.text);
                mLayout = stub.findViewById(com.keti.wearable.activity.R.id.layout);
            }
        });

        // 구글 플레이 서비스 객체를 시계 설정으로 초기화
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    // 액티비티가 시작할 때 실행
    @Override // Activity
    protected void onStart() {
        super.onStart();

        // 구글 플레이 서비스에 접속돼 있지 않다면 접속한다.
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    // 액티비티가 종료될 때 실행
    @Override // Activity
    protected void onStop() {
        // 구글 플레이 서비스 접속 해제
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    // 구글 플레이 서비스에 접속 됐을 때 실행
    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();

        // 노드, 메시지, 데이터 이벤트를 활용할 수 있도록 이벤트 리스너 지정
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    // 구글 플레이 서비스에 접속이 일시정지 됐을 때 실행
    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended", Toast.LENGTH_SHORT).show();
    }

    // 구글 플레이 서비스에 접속을 실패했을 때 실행
    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();

        // 노드, 메시지, 데이터 이벤트 리스너 해제
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    // 페어링이 되면 실행된다.
    @Override // NodeApi.NodeListener
    public void onPeerConnected(Node node) {
        Toast.makeText(this, "Peer Connected", Toast.LENGTH_SHORT).show();
    }

    // 페어링이 해제되면 실행된다.
    @Override // NodeApi.NodeListener
    public void onPeerDisconnected(Node node) {
        Toast.makeText(this, "Peer Disconnected", Toast.LENGTH_SHORT).show();
    }

    // 메시지가 수신되면 실행되는 메소드
    @Override // MessageApi.MessageListener
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/MESSAGE_PATH")) {
            // 텍스트뷰에 적용 될 문자열을 지정한다.
            final String msg = new String(messageEvent.getData(), 0, messageEvent.getData().length);

            // UI 스레드를 실행하여 텍스트 뷰의 값을 수정한다.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(msg);
                }
            });
        }
    }

    // 구글 플레이 서비스의 데이터가 변경되면 실행된다.
    @Override // DataApi.DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {

        // 데이터 이벤트 횟수별로 동작한다.
        for (DataEvent event : dataEvents) {

            // 데이터 변경 이벤트일 때 실행된다.
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                // 동작을 구분할 패스를 가져온다.
                String path = event.getDataItem().getUri().getPath();

                // 패스가 문자 데이터 일 때
                if (path.equals("/STRING_DATA_PATH")) {
                    // 이벤트 객체로부터 데이터 맵을 가져온다.
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    // 데이터맵으로부터 수신한 문자열을 가져온다.
                    final String receiveString = dataMapItem.getDataMap().getString("sendString");

                    // UI 스레드를 실행하여 텍스트 뷰의 값을 수정한다.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(receiveString);
                        }
                    });
                }
                // 패스가 이미지 데이터 일 때
                else if (path.equals("/IMAGE_DATA_PATH")) {

                    // 이벤트 객체로부터 데이터 맵을 가져온다.
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    // 데이터맵으로부터 수신한 에셋을 가져온다.
                    Asset assetImage = dataMapItem.getDataMap().getAsset("assetImage");

                    // 에셋으로부터 비트맵을 생성한다.
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, assetImage);

                    // UI 스레드를 실행하여 레이아웃의 배경을 변경한다.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
                        }
                    });
                }

                // 데이터 삭제 이벤트일 때 실행된다.
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // 데이터가 삭제됐을 때 수행할 동작
            }
        }
    }

    // 비트맵을 에셋(Asset)으로부터 생성한다.
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        // 에셋을 구글 플레이 서비스로부터 받는다.
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            return null;
        }

        // 에셋으로부터 비트맵을 생성한다.
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
