package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Response implements AsResults {

    private final String mUrl;
    private final byte[] mData;
    private final int mCode;
    private final String mMessage;
    private Bitmap mBitmapCache;

    protected Response(byte[] data, String url, int code, String message) throws IOException {
        mData = data;
        mUrl = url;
        mCode = code;
        mMessage = message;
    }

    public String url() {
        return mUrl;
    }

    public int code() {
        return mCode;
    }

    public String phrase() {
        return mMessage;
    }

    public boolean isSuccess() {
        return mCode == HttpURLConnection.HTTP_OK;
    }

    public Response throwIfNotSuccess() throws ResponseException {
        if (!isSuccess()) throw new ResponseException(this);
        return this;
    }

    public byte[] asBytes() {
        return mData;
    }

    public String asString() {
        try {
            final byte[] bytes = asBytes();
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public Spanned asHtml() {
        final String content = asString();
        if (content == null)
            return null;
        return Html.fromHtml(content);
    }

    @Override
    public Bitmap asBitmap() {
        if (mBitmapCache == null) {
            final InputStream is = new ByteArrayInputStream(asBytes());
            mBitmapCache = BitmapFactory.decodeStream(is);
            Util.closeQuietly(is);
        }
        return mBitmapCache;
    }

    public JSONObject asJsonObject() throws ResponseException {
        final String content = asString();
        if (content == null)
            throw new ResponseException("No content was returned in this response.");
        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            throw new ResponseException(this, e);
        }
    }

    public JSONArray asJsonArray() throws ResponseException {
        final String content = asString();
        if (content == null)
            throw new ResponseException("No content was returned in this response.");
        try {
            return new JSONArray(content);
        } catch (JSONException e) {
            throw new ResponseException(this, e);
        }
    }

    public void asFile(File destination) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destination);
            os.write(asBytes());
            os.flush();
        } finally {
            Util.closeQuietly(os);
        }
    }

    @Override
    public String toString() {
        return String.format("%s, %d %s, %d bytes", mUrl, mCode, mMessage, mData != null ? mData.length : 0);
    }
}