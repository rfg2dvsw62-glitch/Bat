package com.zika.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebSettings;

import java.io.InputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private String pendingType = "";
    private static final int REQ_PICK = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openFileChooser(String type) {
                pendingType = type;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if ("sticker".equals(type) || "image".equals(type)) {
                    intent.setType("image/*");
                } else {
                    intent.setType("*/*");
                }
                startActivityForResult(Intent.createChooser(intent, "选择文件"), REQ_PICK);
            }
        }, "AndroidBridge");

        webView.loadUrl("https://rfg2dvsw62-glitch.github.io/zikaa/");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                byte[] bytes = readAllBytes(is);
                is.close();

                if ("sticker".equals(pendingType) || "image".equals(pendingType)) {
                    String mime = getContentResolver().getType(uri);
                    if (mime == null) mime = "image/png";
                    String base64 = "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                    String func = "sticker".equals(pendingType) ? "androidReceiveSticker" : "androidReceiveImage";
                    final String js = func + "('" + base64 + "');";
                    webView.post(new Runnable() {
                        @Override
                        public void run() { webView.evaluateJavascript(js, null); }
                    });
                } else if ("importAll".equals(pendingType)) {
                    String text = new String(bytes, "UTF-8");
                    final String js = "androidReceiveImportAll(" + jsString(text) + ");";
                    webView.post(new Runnable() {
                        @Override
                        public void run() { webView.evaluateJavascript(js, null); }
                    });
                } else if ("importCards".equals(pendingType)) {
                    String text = new String(bytes, "UTF-8");
                    final String js = "androidReceiveImportCards(" + jsString(text) + ");";
                    webView.post(new Runnable() {
                        @Override
                        public void run() { webView.evaluateJavascript(js, null); }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] readAllBytes(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private String jsString(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
