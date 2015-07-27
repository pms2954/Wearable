package com.keti.wearable.activity;

import android.widget.Toast;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class MyService extends WearableListenerService {

    // 페어링이 되면 실행된다.
    @Override // NodeApi.NodeListener
    public void onPeerConnected(Node node) {
        Toast.makeText(getApplication(), "Peer Connected", Toast.LENGTH_SHORT).show();
    }

    // 페어링이 해제되면 실행된다.
    @Override // NodeApi.NodeListener
    public void onPeerDisconnected(Node node) {
        Toast.makeText(getApplication(), "Peer Disconnected", Toast.LENGTH_SHORT).show();
    }

    // 메시지가 수신되면 실행되는 메소드
    @Override // MessageApi.MessageListener
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/MESSAGE_PATH")) {
            // 텍스트뷰에 적용 될 문자열을 지정한다.
            final String msg = new String(messageEvent.getData(), 0, messageEvent.getData().length);

            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
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

                    Toast.makeText(getApplication(), receiveString, Toast.LENGTH_SHORT).show();
                }
                // 패스가 이미지 데이터 일 때
                else if (path.equals("/IMAGE_DATA_PATH")) {
                    Toast.makeText(getApplication(), "Received Image", Toast.LENGTH_SHORT).show();
                }

                // 데이터 삭제 이벤트일 때 실행된다.
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // 데이터가 삭제됐을 때 수행할 동작
            }
        }
    }
}
