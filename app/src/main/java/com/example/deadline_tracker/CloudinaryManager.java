package com.example.deadline_tracker;

import android.content.Context;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static final String CLOUD_NAME  = "dypmkxzcw";
    private static final String API_KEY     = "338178119728697";
    private static final String API_SECRET  = "y9dbkZMcZz4BHtpqog4cEvqrz9Y";

    private static boolean initialized = false;

    public interface UploadListener {
        void onSuccess(String fileUrl, String publicId);
        void onProgress(int percent);
        void onError(String error);
    }

    public static void init(Context context) {
        if (initialized) return;
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name",  CLOUD_NAME);
        config.put("api_key",     API_KEY);
        config.put("api_secret",  API_SECRET);
        MediaManager.init(context, config);
        initialized = true;
    }

    // Upload any file (PDF or image) to Cloudinary
    public static void uploadFile(Context context, Uri fileUri,
                                  String folder, UploadListener listener) {
        init(context);

        MediaManager.get().upload(fileUri)
                .option("folder", folder)
                .option("resource_type", "auto") // handles both PDF and image
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int percent = (int) ((bytes * 100) / totalBytes);
                        listener.onProgress(percent);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url      = (String) resultData.get("secure_url");
                        String publicId = (String) resultData.get("public_id");
                        listener.onSuccess(url, publicId);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        listener.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        listener.onError("Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }
}
