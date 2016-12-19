package com.ximisoft.torchflashlight;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * Created by Abdel44 on 09/09/2015.
 */
public class WebViewActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mywebview);
        ActionBar actionBar = getActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        Intent i = getIntent();
        WebView lWebView = (WebView) findViewById(R.id.mywebview);
        lWebView.loadUrl("file:///android_asset/licences.html");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
