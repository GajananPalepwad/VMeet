package com.gn4k.vmeet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class MainActivity extends AppCompatActivity {


    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS =
            {
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CAMERA
            };
    private int recUid = 1, senderUid = 1;

    private boolean isJoined = false;
    private boolean isCameraOn = true;
    private boolean isMicOn = true;
    Drawable drawable;
    MyAdapter adapter;
    private MaterialButton btnCam, btnMic;
    String serverId="a";
    String lastKey="a", newKey="a";
    private RtcEngine agoraEngin;
    private SurfaceView localSurfaceView;
    private SurfaceView remoteSurfaceView;

    List<String> stringList = new ArrayList<>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Random random = new Random();

        recUid = random.nextInt();
        btnCam = findViewById(R.id.btnCam);
        btnMic = findViewById(R.id.btnMic);
        if(!checkSelfPermission()){
            ActivityCompat.requestPermissions(this,REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }

        findViewById(R.id.btnEnd).setOnClickListener(v -> {
            if(isJoined){
                leaveChannel(v);
                int color = Color.parseColor("#00FF00");
                findViewById(R.id.btnEnd).setBackgroundColor(color);
            } else {
                joinChannel(v);
                int color = Color.parseColor("#DF3131");
                findViewById(R.id.btnEnd).setBackgroundColor(color);
            }
        });

        setupVideoSDKEngine();

    }


    private boolean checkSelfPermission()
    {
        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) !=  PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) !=  PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        return true;
    }

    public void toggleCamera(View view) {

        try {
            if (isCameraOn) {
                // Turn off the camera
                agoraEngin.enableLocalVideo(false);
                localSurfaceView.setVisibility(View.GONE);
                drawable = getResources().getDrawable(R.drawable.mdi_camera_off);
                showMessage("Camera turned off");
            } else {
                // Turn on the camera
                agoraEngin.enableLocalVideo(true);
                localSurfaceView.setVisibility(View.VISIBLE);
                drawable = getResources().getDrawable(R.drawable.mdi_camera);
                showMessage("Camera turned on");
            }
            isCameraOn = !isCameraOn;
            btnCam.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);
        }catch (Exception E){

        }
    }

    private void showChatPopup() {
        // Inflate the chat pop-up layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_chat, null);
        adapter = new MyAdapter(stringList);
        // Create and configure a custom dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.AppBottomSheetDialogTheme);
        alertDialogBuilder.setView(popupView);

        // Initialize UI elements in the pop-up layout
        RecyclerView chatRecyclerView = popupView.findViewById(R.id.chatRecyclerView);
        EditText chatInputEditText = popupView.findViewById(R.id.chatInputEditText);
        Button sendButton = popupView.findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v->{

            if(!isJoined){
                Toast.makeText(this, "Join Call first", Toast.LENGTH_SHORT).show();
            }else {


                String msg = chatInputEditText.getText().toString();

                if(!(msg =="") || !msg.isEmpty()){

                    chatInputEditText.setText(""); // Clear the input field

                    if (recUid > senderUid) {
                        serverId = senderUid + "" + recUid;
                    } else {
                        serverId = recUid + "" + senderUid;
                    }


                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference yourNodeRef = databaseReference.child("your/" + serverId);

                    yourNodeRef
                            .orderByKey()
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Get the last key (ID)
                                        lastKey = dataSnapshot.getChildren().iterator().next().getKey();
                                        DatabaseReference stringRef = databaseReference.child("your/" + serverId + "/" + generateNextString(lastKey));
                                        stringRef.setValue(msg);

                                        stringRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            String receivedData = dataSnapshot.getValue(String.class);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                // Handle errors, if any
                                            }
                                        });
                                    } else {
                                        DatabaseReference stringRef = databaseReference.child("your/" + serverId + "/" + generateNextString(lastKey));
                                        stringRef.setValue(msg);

                                        stringRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            String receivedData = dataSnapshot.getValue(String.class);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                // Handle errors, if any
                                            }
                                        });
                                        Log.d("Firebase", "No data found in the node.");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    // Handle errors, if any
                                    Log.e("Firebase", "Error: " + databaseError.getMessage());
                                }
                            });





                }
            }
            chatRecyclerView.setAdapter(adapter);
        });

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        handler.postDelayed(updateRunnable, 1000);
        try {


            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference stringsRef = databaseReference.child("your/" + serverId);

            stringsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String stringData = snapshot.getValue().toString();
                            if (stringData != null) {
                                stringList.add(stringData);
                            }
                        }
                        chatRecyclerView.setAdapter(adapter);

                        // Notify the adapter of data changes
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("Firebase", "No data found in the node.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors, if any
                    Log.e("Firebase", "Error: " + databaseError.getMessage());
                }
            });
        }catch (Exception e){}


        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private final Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            // Call a method to update the list here
            updateChatList();
            // Schedule the next update after 1 second (1000 milliseconds)
            handler.postDelayed(this, 1000);
        }
    };

    private void updateChatList() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference stringsRef = databaseReference.child("your/" + serverId);

        stringsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    stringList.clear(); // Clear the existing list
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String stringData = snapshot.getValue(String.class);
                        if (stringData != null) {
                            stringList.add(stringData);
                        }
                    }
                    adapter.notifyDataSetChanged(); // Notify the adapter of data changes
                } else {
                    Log.d("Firebase", "No data found in the node.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors, if any
                Log.e("Firebase", "Error: " + databaseError.getMessage());
            }
        });
    }

    public void showChatPopupOnClick(View view) {
        showChatPopup();
    }


    public static String generateNextString(String previous) {
        char[] chars = previous.toCharArray();

        // Find the last character
        int lastIndex = chars.length - 1;

        // Handle the special case when the last character is 'z'
        if (chars[lastIndex] == 'z') {
            // Loop backward to find the first character that is not 'z'
            int i = lastIndex;
            while (i >= 0 && chars[i] == 'z') {
                chars[i] = 'a';
                i--;
            }

            // If all characters were 'z', add an 'a' at the beginning
            if (i < 0) {
                char[] newChars = new char[chars.length + 1];
                newChars[0] = 'a';
                System.arraycopy(chars, 0, newChars, 1, chars.length);
                chars = newChars;
            } else {
                // Increment the character at index i
                chars[i]++;
            }
        } else {
            // Increment the last character if it's not 'z'
            chars[lastIndex]++;
        }

        return new String(chars);
    }

    public void toggleMic(View view) {
        if (isMicOn) {
            // Turn off the microphone
            agoraEngin.muteLocalAudioStream(true);
            drawable = getResources().getDrawable(R.drawable.microphone_mute);
            showMessage("Microphone turned off");
        } else {
            // Turn on the microphone
            agoraEngin.muteLocalAudioStream(false);
            drawable = getResources().getDrawable(R.drawable.ic_mic);
            showMessage("Microphone turned on");
        }
        isMicOn = !isMicOn;
        btnMic.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);

    }



    void showMessage(String message){
        runOnUiThread(()-> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void setupVideoSDKEngine(){
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = getResources().getString(R.string.appId);
            config.mEventHandler = mRtcHandler;
            agoraEngin = RtcEngine.create(config);
            agoraEngin.enableVideo();
        }catch (Exception e){
            showMessage(e.toString());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        agoraEngin.stopPreview();
        agoraEngin.leaveChannel();

        new Thread(()->{
            RtcEngine.destroy();
            agoraEngin = null;
        }).start();
    }


    private final IRtcEngineEventHandler mRtcHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            showMessage("Remote user joined "+uid);
            senderUid = uid;
            if (recUid > senderUid) {
                serverId = senderUid + "" + recUid;
            } else {
                serverId = recUid + "" + senderUid;
            }
            runOnUiThread(()->setupRemoteVideo(uid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            isJoined = true;
            senderUid = uid;
            if (recUid > senderUid) {
                serverId = senderUid + "" + recUid;
            } else {
                serverId = recUid + "" + senderUid;
            }
            showMessage("Joined Channel "+channel);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);

            showMessage("Remote user offline "+uid +" "+reason);
            senderUid = uid;
            if (recUid > senderUid) {
                serverId = senderUid + "" + recUid;
            } else {
                serverId = recUid + "" + senderUid;
            }
            runOnUiThread(()->remoteSurfaceView.setVisibility(View.GONE));
        }
    };


    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remoteVideo);
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngin.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }

    private void setupLocalVideo(){
        FrameLayout container = findViewById(R.id.localVideo);
        localSurfaceView = new SurfaceView(getBaseContext());
        container.addView(localSurfaceView);
        agoraEngin.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }


    public void joinChannel(View view){
        if(checkSelfPermission()){
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            setupLocalVideo();
            localSurfaceView.setVisibility(View.VISIBLE);
            agoraEngin.startPreview();
            agoraEngin.joinChannel(getResources().getString(R.string.token),getResources().getString(R.string.channel),recUid,options);

        }else {
            showMessage("Permission was not granted");
        }
    }

    public void leaveChannel(View view){
        if(!isJoined){
            showMessage("Join a channel first");
        }else{
            agoraEngin.leaveChannel();
            showMessage("You left channel");
            if(remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
            if(localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
            isJoined = false;
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference branchRef = databaseReference.child("your/"+serverId);

            branchRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Branch deleted successfully
                        Log.d("Firebase", "Branch deleted successfully.");
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error if the branch deletion fails
                        Log.e("Firebase", "Error deleting branch: " + e.getMessage());
                    });


        }
    }

}