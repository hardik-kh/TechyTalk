package com.example.techytalk.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.techytalk.Adapters.GroupMessagesAdapter;
import com.example.techytalk.Adapters.MessagesAdapter;
import com.example.techytalk.Models.Message;
import com.example.techytalk.databinding.ActivityGroupChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatBinding binding;
    GroupMessagesAdapter adapter;

    ArrayList<Message> messages;

    FirebaseDatabase database;
    FirebaseStorage storage;
    String senderUid;
    String receiverUid;
    String tag;
    public String filePath;
    Uri selectedImage;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        senderUid = FirebaseAuth.getInstance().getUid();
        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading...");
        dialog.setCancelable(false);
        messages = new ArrayList<>();
        adapter = new GroupMessagesAdapter(this,messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        database.getReference().child("public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = binding.messageBox.getText().toString();
                if(!messageTxt.trim().isEmpty()) {
                    Date date = new Date();
                    Message message = new Message(messageTxt, senderUid, date.getTime());
                    binding.messageBox.setText("");

                    database.getReference().child("public")
                            .push()
                            .setValue(message);

                }
                else {
                    Toast.makeText(GroupChatActivity.this, "Please type a message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,25);
            }
        });


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 25) {
            if(data != null) {
                if(data.getData() != null) {
                    if (data.getData().getPath().contains("video")) {
                        tag = "video";
                        //Log.e("hardik", "onActivityResult: " + data.getType() );
                    }
                    else{
                        tag = "photo";
                        //Log.e("hardik", "onActivityResult: " + data.getType() );
                    }
                    Log.e("hardik", "onActivityResult: " + data.getType() );
                    selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(task -> {
                        dialog.dismiss();
                        if(task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                filePath = uri.toString();

                                String messageTxt = binding.messageBox.getText().toString();
                                Date date = new Date();
                                Message message = new Message(messageTxt,senderUid,date.getTime());
                                message.setMessage(tag);
                                message.setImageUrl(filePath);
                                binding.messageBox.setText("");


                                database.getReference().child("public")
                                        .push()
                                        .setValue(message);


                            });
                        }
                    });
                }
            }
        }
    }
}
