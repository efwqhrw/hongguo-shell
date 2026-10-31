package com.hongguo.shell;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * 红果短剧网页版外壳。
 *
 * 做的事只有四件：
 * 1. 把官方网页版 hongguoduanju.com 装进一个无边框 WebView；
 * 2. 页面加载完注入一段 JS，把「前往 App / 下载 / 扫码」这类导流元素隐藏掉；
 * 3. 拦掉 snssdk1128:// intent:// market:// 等唤起 App 的 scheme 和 .apk 下载；
 * 4. 视频全屏、返回键、旋转都按原生 App 的习惯处理。
 *
 * 没有网络请求改写、没有签名破解、没有第三方库、没有统计 SDK。
 */
public class MainActivity extends Activity {

    private static final String HOME = "https://hongguoduanju.com/";

    private FrameLayout root;
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    /** 页面内注入：按「可见文字」识别导流元素并隐藏；用 MutationObserver 应付 SPA 动态插入。 */
    private static final String HIDE_JS = """
            (function () {
              var WORDS = ['前往「红果短剧」','前往红果短剧','下载「红果短剧」','扫码下载',
                           '免费看全集','立即下载','打开APP','打开App','前往APP','下载APP'];
              function hit(t) {
                for (var i = 0; i < WORDS.length; i++) {
                  if (t.indexOf(WORDS[i]) !== -1) return true;
                }
                return false;
              }
              function run() {
                var els = document.querySelectorAll('a,button,div,span,p,section');
                for (var i = 0; i < els.length; i++) {
                  var e = els[i];
                  if (e.getAttribute('data-hg') === '1') continue;
                  if (e.children.length > 3) continue;
                  var t = (e.innerText || '').trim();
                  if (!t || t.length > 28) continue;
                  if (hit(t)) {
                    e.style.setProperty('display', 'none', 'important');
                    e.setAttribute('data-hg', '1');
                  }
                }
                var f = document.querySelectorAll(
                    'img[src*="qr"],[class*="download"],[id*="download"],[class*="Download"]');
                for (var k = 0; k < f.length; k++) {
                  if (f[k].querySelector && f[k].querySelector('video')) continue;
                  f[k].style.setProperty('display', 'none', 'important');
                }
              }
              run();
              try {
                new MutationObserver(function () { run(); })
                    .observe(document.documentElement, { childList: true, subtree: true });
              } catch (e) {}
            })();
            """;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 去掉 WebView 特有的 "; wv" 标记，避免站点走"请下载 App"分支
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));

        CookieManager.getInstance().setAcceptCookie(true);
        web.setBackgroundColor(Color.BLACK);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(HIDE_JS, null);
            }
        });

        // 任何下载（基本都是"下载 App"的 APK）一律拦掉
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                Toast.makeText(MainActivity.this, "已阻止下载", Toast.LENGTH_SHORT).show());

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                hideSystemBars();
            }

            @Override
            public void onHideCustomView() {
                exitCustomView();
            }
        });

        web.loadUrl(HOME);
    }

    /** 只放行 http(s)；拦掉所有唤起 App 的 scheme 和 APK 下载。 */
    private boolean handleUrl(Uri u) {
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            Toast.makeText(this, "已阻止跳转到 App", Toast.LENGTH_SHORT).show();
            return true;
        }
        String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
        if (path.endsWith(".apk")) {
            Toast.makeText(this, "已阻止下载安装包", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void exitCustomView() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        web.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitCustomView();
            return;
        }
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
