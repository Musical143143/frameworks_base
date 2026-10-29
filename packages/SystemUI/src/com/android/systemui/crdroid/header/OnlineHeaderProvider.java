/*
 * Copyright (C) 2026 The LunarisAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.crdroid.header;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Header provider for the "Online" source. The user supplies an index URL that returns a JSON
 * array of image URLs (or {"images": [...]}). One image is picked per day, downloaded on a
 * background thread, cached in the app's files directory and then shown like a local header.
 * Nothing touches the network on the UI thread; until the image arrives, getCurrent returns null.
 */
public class OnlineHeaderProvider implements StatusBarHeaderMachine.IStatusBarHeaderProvider {

    public static final String NAME = "online";
    private static final String TAG = "OnlineHeaderProvider";
    private static final String CACHE_DIR = "online_header";
    private static final int TIMEOUT_MS = 15000;
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private final Context mContext;
    private final ExecutorService mIo = Executors.newSingleThreadExecutor();
    private Runnable mOnLoaded;
    private Drawable mImage;
    private String mLoadedKey;
    private boolean mLoading;

    public OnlineHeaderProvider(Context context) {
        mContext = context;
    }

    /** Called by the machine so a finished download can refresh the header on the UI thread. */
    public void setOnLoadedListener(Runnable listener) {
        mOnLoaded = listener;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void settingsChanged(Uri uri) {
        String index = indexUrl();
        if (index == null) {
            mImage = null;
            mLoadedKey = null;
            return;
        }
        String key = index + "#" + dayKey();
        if (!key.equals(mLoadedKey)) {
            mImage = null;
            mLoadedKey = key;
            startLoad(index);
        }
    }

    @Override
    public void enableProvider() {
        settingsChanged(null);
    }

    @Override
    public void disableProvider() {
    }

    @Override
    public Drawable getCurrent(final Calendar now) {
        return mImage;
    }

    private String indexUrl() {
        String url = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_ONLINE_URL, UserHandle.USER_CURRENT);
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            return null;
        }
        return url.trim();
    }

    private static String dayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    private void startLoad(final String index) {
        if (mLoading) {
            return;
        }
        mLoading = true;
        final String requestKey = mLoadedKey;
        mIo.execute(() -> {
            Drawable loaded = null;
            try {
                String imageUrl = pickImage(index);
                if (imageUrl != null) {
                    File file = cacheFile(imageUrl);
                    if (!file.exists()) {
                        download(imageUrl, file);
                    }
                    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        loaded = new BitmapDrawable(mContext.getResources(), bmp);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Online header load failed", e);
            }
            final Drawable result = loaded;
            mContext.getMainExecutor().execute(() -> {
                mLoading = false;
                if (requestKey != null && requestKey.equals(mLoadedKey)) {
                    mImage = result;
                    if (mOnLoaded != null) {
                        mOnLoaded.run();
                    }
                } else {
                    // The URL or day changed while downloading; load the current one.
                    String current = indexUrl();
                    if (current != null) {
                        settingsChanged(null);
                    }
                }
            });
        });
    }

    /** Reads the index and chooses today's image; the choice is stable for the whole day. */
    private String pickImage(String index) throws Exception {
        String body = readText(index);
        JSONArray images;
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            images = new JSONObject(trimmed).optJSONArray("images");
        } else {
            images = new JSONArray(trimmed);
        }
        if (images == null || images.length() == 0) {
            return null;
        }
        int pick = Math.abs(dayKey().hashCode()) % images.length();
        return images.getString(pick);
    }

    private String readText(String address) throws Exception {
        HttpURLConnection conn = open(address);
        try (InputStream in = conn.getInputStream()) {
            byte[] buf = in.readNBytes((int) Math.min(MAX_BYTES, 1024 * 1024));
            return new String(buf, "UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    private void download(String address, File target) throws Exception {
        HttpURLConnection conn = open(address);
        File tmp = new File(target.getParentFile(), target.getName() + ".part");
        try (InputStream in = conn.getInputStream();
             OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[16 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_BYTES) {
                    throw new IllegalStateException("Header image too large");
                }
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
        if (!tmp.renameTo(target)) {
            tmp.delete();
        }
    }

    private HttpURLConnection open(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private File cacheFile(String imageUrl) {
        File dir = new File(mContext.getFilesDir(), CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, Integer.toHexString(imageUrl.hashCode()) + ".img");
    }
}
