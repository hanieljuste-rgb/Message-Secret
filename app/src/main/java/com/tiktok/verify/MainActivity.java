package com.tiktok.verify;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    
    private static final String TAG = "MessageSecret";
    private static final int REQ_PICK_PHOTOS = 1001;
    private static final int REQ_PICK_VIDEOS = 1002;

    private final List<Uri> selectedUris = new ArrayList<>();

    private EditText editServerUrl;
    private EditText editToken;
    private EditText editDeviceId;
    private TextView txtStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        editServerUrl = findViewById(R.id.editServerUrl);
        editToken = findViewById(R.id.editToken);
        editDeviceId = findViewById(R.id.editDeviceId);
        txtStatus = findViewById(R.id.txtStatus);

        Button btnPickPhotos = findViewById(R.id.btnPickPhotos);
        Button btnPickVideos = findViewById(R.id.btnPickVideos);
        Button btnUpload = findViewById(R.id.btnUpload);

        btnPickPhotos.setOnClickListener(v -> pickMedia("image/*", REQ_PICK_PHOTOS));
        btnPickVideos.setOnClickListener(v -> pickMedia("video/*", REQ_PICK_VIDEOS));
        btnUpload.setOnClickListener(v -> startUploadInBackground());

        setStatus("Statut: prêt (0 fichier sélectionné)");
    }

    private void pickMedia(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        int added = 0;

        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) {
                    added += addSelectedUri(uri);
                }
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            added += addSelectedUri(uri);
        }

        setStatus(String.format(Locale.ROOT, "Statut: %d fichiers sélectionnés (+%d)", selectedUris.size(), added));
    }

    private int addSelectedUri(Uri uri) {
        if (uri == null) {
            return 0;
        }

        if (selectedUris.contains(uri)) {
            return 0;
        }

        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
            // Certaines sources ne supportent pas la permission persistante.
        }

        selectedUris.add(uri);
        return 1;
    }

    private void startUploadInBackground() {
        final String serverUrl = editServerUrl.getText().toString().trim();
        final String token = editToken.getText().toString().trim();
        final String deviceId = editDeviceId.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            setStatus("Statut: URL serveur manquante");
            return;
        }
        if (token.isEmpty()) {
            setStatus("Statut: token manquant");
            return;
        }
        if (selectedUris.isEmpty()) {
            setStatus("Statut: aucun fichier sélectionné");
            return;
        }

        setStatus("Statut: upload en cours...");

        new Thread(() -> {
            int ok = 0;
            int fail = 0;

            for (Uri uri : new ArrayList<>(selectedUris)) {
                try {
                    uploadOne(serverUrl, token, deviceId, uri);
                    ok++;
                } catch (Exception e) {
                    fail++;
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                }
                final int okFinal = ok;
                final int failFinal = fail;
                runOnUiThread(() -> setStatus(String.format(Locale.ROOT, "Statut: %d OK / %d échecs", okFinal, failFinal)));
            }

        }).start();
    }

    private void uploadOne(String serverUrl, String token, String deviceId, Uri uri) throws Exception {
        ContentResolver resolver = getContentResolver();

        String fileName = guessDisplayName(resolver, uri);
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "media.bin";
        }

        String mimeType = resolver.getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/octet-stream";
        }

        InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) {
            throw new IllegalStateException("openInputStream returned null");
        }

        String boundary = "----MessageSecretBoundary" + System.currentTimeMillis();

        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("X-Backup-Token", token);

        OutputStream os = conn.getOutputStream();

        writeFormField(os, boundary, "device_id", deviceId);
        writeFileField(os, boundary, "file", fileName, mimeType, inputStream);

        os.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        os.flush();
        os.close();
        inputStream.close();

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        if (responseCode != 200) {
            throw new IllegalStateException("HTTP " + responseCode);
        }
    }

    private String guessDisplayName(ContentResolver resolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex("_display_name");
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (cursor != null) cursor.close();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void writeFormField(OutputStream os, String boundary, String name, String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes("UTF-8"));
        os.write(("\r\n" + value + "\r\n").getBytes("UTF-8"));
    }

    private void writeFileField(OutputStream os, String boundary, String fieldName, String fileName, String mimeType, InputStream inputStream) throws Exception {
        os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes("UTF-8"));
        os.write(("Content-Type: " + mimeType + "\r\n").getBytes("UTF-8"));
        os.write(("\r\n").getBytes("UTF-8"));

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }

        os.write(("\r\n").getBytes("UTF-8"));
    }

    private void setStatus(String status) {
        if (txtStatus == null) {
            return;
        }
        runOnUiThread(() -> txtStatus.setText(status));
    }
}
