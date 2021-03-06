package es.dpatrongomez.papas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import es.dpatrongomez.papas.modelo.UserData;


public class MainActivity extends AppCompatActivity {

    ProgressBar carga;
    WebView papas;
    UserData datos;
    String url;
    CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();


        datos = (UserData) extras.get("datos");

        System.out.println(datos);
        url = extras.getString("url");

        if (url == null) {
            url = "https://papas.jccm.es/";
        }

        carga = findViewById(R.id.carga);
        papas = findViewById(R.id.papas);

        carga.setMax(100);

        papas.loadUrl(url);
        papas.getSettings().setJavaScriptEnabled(true);
        papas.getSettings().setSupportZoom(true);
        papas.getSettings().setUseWideViewPort(true);
        papas.getSettings().setAllowContentAccess(true);
        papas.getSettings().setAppCacheEnabled(true);
        papas.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        papas.getSettings().setDomStorageEnabled(true);
        papas.getSettings().setAllowFileAccess(true);
        papas.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        papas.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {

                if (datos.getUser() != null && datos.getPassword() != null) {
                    super.onPageFinished(view, url);
                    papas.loadUrl("javascript: var usuario=document.querySelector('input[id=\"username\"]').value ='" + datos.getUser() + "';");
                    papas.loadUrl("javascript: var password=document.querySelector('input[type=\"password\"]').value ='" + datos.getPassword() + "';");
                    papas.loadUrl("javascript: var x = document.querySelector('input[type=\"submit\"]').click();");
                    datos.setUser(null);
                    datos.setPassword(null);

                }

                if (papas.getUrl().toString().contains("logout")) {
                    Intent i = new Intent(MainActivity.this, Login.class);
                    startActivity(i);
                    finish();
                }
            }
        });
        papas.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                carga.setProgress(newProgress);
                if (newProgress < 100) {
                    carga.setVisibility(ProgressBar.VISIBLE);
                } else {
                    carga.setVisibility(ProgressBar.GONE);
                }
            }


        });


        papas.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                if (!check_permission(1)) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                } else {
                    //String urlParseada="";
                    // String contentParseado=contentDisposition.substring(0,contentDisposition.length()-2);

                    //   urlParseada=url.substring(0,url.length()-1);

                    //Toast.makeText(getApplicationContext(),url, Toast.LENGTH_LONG).show();
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription(getString(R.string.dl_downloading));
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    assert dm != null;
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_anterior:
                onBackPressed();
                break;
            case R.id.menu_siguiente:
                onForwardPressed();
                break;
            case R.id.info:
                Intent intent = new Intent(this, Info.class);
                startActivity(intent);
                break;
            case R.id.donacion:
                Uri paypal = Uri.parse("https://paypal.me/dpatrongomez");
                Intent donacion = new Intent(Intent.ACTION_VIEW, paypal);
                startActivity(donacion);
                break;
            case R.id.cerrarSesion:
                url = "https://ssopapas.jccm.es/ssopapas/logout";
                papas.loadUrl(url);

                cookieSyncMngr.startSync();
                CookieManager cookieManager=CookieManager.getInstance();
                cookieManager.removeAllCookie();
                cookieManager.removeSessionCookie();
                cookieSyncMngr.stopSync();
                cookieSyncMngr.sync();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onForwardPressed() {
        if (papas.canGoForward()) {
            papas.goForward();
        } else {
            Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), R.string.erroravanzar, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (papas.canGoBack()) {
            papas.goBack();
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("¿Quieres salir de la aplicación");
            builder.setCancelable(true);
            builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.finishAffinity(MainActivity.this);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        }
    }

    //Checking if particular permission is given or not
    public boolean check_permission(int permission) {
        switch (permission) {
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }
}
