package documentshare.cognizant.com.documentshare;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {


    private String DOCUMENT_URL = "https://www.acm.org/sigs/publications/pubform.doc";
    private final int REQUEST_WRITE_STORAGE = 1231;
    private boolean isPermissionAllowed = false;
    private final String SDCARD_PATH = "/sdcard/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button download = (Button)findViewById(R.id.download);
        download.setOnClickListener(downloadClickListener);

        Button share = (Button)findViewById(R.id.share);
        share.setOnClickListener(shareClickListener);
    }

    private View.OnClickListener downloadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean hasPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasPermission) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);
            } else {
                new DownloadFileFromURL().execute(DOCUMENT_URL);
            }

        }
    };

    private View.OnClickListener shareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            File fileDirectory = new File(SDCARD_PATH+getFileNameFromURL(DOCUMENT_URL));

            if(fileDirectory.exists()) {
                intentShareFile.setType(getMimeType(getFileNameFromURL(DOCUMENT_URL)));
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+SDCARD_PATH+getFileNameFromURL(DOCUMENT_URL)));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        "Sharing File...");
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
        }
    };

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        String urlTODownload;
        String fileName;
        ProgressDialog progress;
        private String TAG = DownloadFileFromURL.class.getSimpleName();

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                urlTODownload = url.toString();
                fileName = getFileNameFromURL(urlTODownload);
                trustAllCertificates();
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(SDCARD_PATH+fileName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(""+(int)((total*100)/lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                Log.i(TAG, "doInBackground: Download Success");

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissProgressDialog();
            String filePath = Environment.getExternalStorageDirectory().toString() + fileName;
        }


        void showProgressDialog(){
            if(progress == null) {
                progress = new ProgressDialog(MainActivity.this);
                progress.setMessage("Downloading File");
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
            }
            progress.show();
        }

        void dismissProgressDialog(){
            if(progress != null && progress.isShowing()){
                progress.dismiss();
            }
        }
    }

    private String getFileNameFromURL(String url){

        int index = url.lastIndexOf('/');
        if(index > 0){
            return url.substring(index+1,url.length());
        }
        return  null;
    }

    public void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                            return myTrustedAnchors;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    isPermissionAllowed = true;
                    new DownloadFileFromURL().execute(DOCUMENT_URL);
                } else
                {
                    isPermissionAllowed = false;
                }
            }
        }

    }

    public  String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
