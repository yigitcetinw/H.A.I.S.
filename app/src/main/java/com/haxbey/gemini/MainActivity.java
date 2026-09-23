package com.haxbey.gemini;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "AQ.Ab8RN6IeOlKdDZ8qoGZSAf9yVa3mqOp9fKe6q9YTLHeqt27R-w";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;

    private MessageAdapter adapter;
    private List<Message> messageList;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        client = new OkHttpClient();

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });
    }

    private void sendMessage(String userText) {
        messageList.add(new Message(userText, Message.TYPE_USER));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
        etMessage.setText("");
        progressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject partObj = new JSONObject();

            partObj.put("text", userText);
            parts.put(partObj);
            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonBody.put("contents", contents);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("X-goog-api-key", API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addAiMessage("Bağlantı hatası oluştu: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String reply = jsonResponse
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            runOnUiThread(() -> addAiMessage(reply));
                        } catch (Exception e) {
                            runOnUiThread(() -> addAiMessage("Yanıt ayrıştırılamadı."));
                        }
                    } else {
                        runOnUiThread(() -> addAiMessage("API Hatası: " + response.code()));
                    }
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            addAiMessage("İstek oluşturulurken hata oluştu.");
        }
    }

    private void addAiMessage(String text) {
        messageList.add(new Message(text, Message.TYPE_AI));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
    }
}
