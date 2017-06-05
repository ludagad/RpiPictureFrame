package com.example.rpisftp;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by User on 16.5.2017 г..
 */

public final class UploadAsyncTask extends AsyncTask<ArrayList<Uri>, String, Long> {
    private String fileToSFTP;
    private String serverAddress;
    private String userId;
    private String password;
    private String remoteDirectory;
    private Activity activity;
    private Uri imageUri;
    private ArrayList<Uri> imageUriArr;
    private JSch jsch = new JSch();
    private Session session = null;
    private Channel channel;
    private ChannelSftp sftpChannel = (ChannelSftp) channel;



    public UploadAsyncTask(Activity appContext) {
        this.activity = appContext;
    }


    @Override
    protected Long doInBackground(ArrayList<Uri>... arrLstUri) {
        long result = 0;
        long uploadCnt = 0;

        this.imageUriArr = arrLstUri[0];

        if (0 != ReadProps()) {
            if (0 != sftpOpenSession()) {
                for (int i=0; i<this.imageUriArr.size(); i++) {
                    this.imageUri = this.imageUriArr.get(i);
                    if (0 != RescaleFile() /*StoreFile()*/) {
                        //ToDo (@Priority: Medium): Get file name / content type from URI.
                        if (0 != sftpUploadFile()) {
                            uploadCnt++;
                        } else {
                            break;
                        }
                        //ToDo (@Priority: High): Delete temp file from cache.
                    }
                }
                sftpCloseSession(); //Close session if OpenSession has not failed. Otherwise next OpenSession might fail.
            }
            if (uploadCnt == this.imageUriArr.size()) {
                result = -1;
            }
        }
        return result;
    }

    private int ReadProps() {
        int result=0;
        InputStream inputStream = null;
        Properties myProps = new Properties();

        try {
            AssetManager assetManager = (this.activity.getApplicationContext()).getAssets();
            inputStream = assetManager.open("config.properties");
            myProps.load(inputStream);

            this.serverAddress = myProps.getProperty("serverAddress").trim();
            this.userId = myProps.getProperty("userId").trim();
            this.password = myProps.getProperty("password").trim();
            this.remoteDirectory = myProps.getProperty("remoteDirectory").trim();

            result = -1;
        } catch (IOException e) {
            Log.e("UploadAsyncTask", e.toString());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    private int StoreFile() {
        int result=0;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = this.activity.getContentResolver().openInputStream(this.imageUri);

            File outputDir = this.activity.getCacheDir(); // context being the Activity pointer
            String imageFileName = getFileName();
            File outputFile = File.createTempFile("myFile", ".jpg", outputDir);
            outputStream = new FileOutputStream(outputFile);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            this.fileToSFTP = outputFile.getAbsolutePath();

            result = -1;
        } catch (IOException e) {
            Log.e("UploadAsyncTask", e.toString());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    // outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    private int RescaleFile() {
        int result=0;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int inWidth = 0;
        int inHeight = 0;
        final int dstWidth = 800;
        final int dstHeight = 480;

        try {
            //Open input stream with intent passed image
            inputStream = this.activity.getContentResolver().openInputStream(this.imageUri);

            // decode image size (decode metadata only, not the whole image)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            inputStream = null;

            // save width and height
            inWidth = options.outWidth;
            inHeight = options.outHeight;

            // decode full image pre-resized
            inputStream = this.activity.getContentResolver().openInputStream(this.imageUri);
            options = new BitmapFactory.Options();
            // calc rought re-size (this is no exact resize)
            options.inSampleSize = Math.max(inWidth/dstWidth, inHeight/dstHeight);
            // decode full image
            Bitmap roughBitmap = BitmapFactory.decodeStream(inputStream, null, options);

            // calc exact destination size
            Matrix m = new Matrix();
            RectF inRect = new RectF(0, 0, roughBitmap.getWidth(), roughBitmap.getHeight());
            RectF outRect = new RectF(0, 0, dstWidth, dstHeight);
            m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
            float[] values = new float[9];
            m.getValues(values);

            // resize bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(roughBitmap, (int) (roughBitmap.getWidth() * values[0]), (int) (roughBitmap.getHeight() * values[4]), true);

            // save image
            try {
                File outputDir = this.activity.getCacheDir(); // context being the Activity pointer
                //TBI... String imageFileName = getFileName();
                File outputFile = File.createTempFile("myFile", ".jpg", outputDir);
                this.fileToSFTP = outputFile.getAbsolutePath();
                outputStream = new FileOutputStream(outputFile);
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);

                result = -1;
            } catch (Exception e) {
                Log.e("Image", e.getMessage(), e);
            } finally {
                if (outputStream != null) {
                    try {
                        // outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            Log.e("Image", e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private int sftpOpenSession() {
        int result = 0;

        try {
            this.session = jsch.getSession(this.userId, this.serverAddress, 22);
            //session.setTimeout(20000);
            //session.setServerAliveInterval(15000);
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.setPassword(this.password);
            publishProgress("Session Connecting ...");
            this.session.connect();
            this.channel = this.session.openChannel("sftp");
            publishProgress("Channel Connecting ...");
            this.channel.connect();
            this.sftpChannel = (ChannelSftp) this.channel;
            result = -1;
        } catch (JSchException e) {
            e.printStackTrace();
            publishProgress(e.toString());
        }
        return result;
    }
    private int sftpUploadFile() {
        int result = 0;

        try {
            publishProgress("Start Upload ...");
            this.sftpChannel.put(this.fileToSFTP, this.remoteDirectory);
            publishProgress("Upload complete");
            result = -1;
        } catch (SftpException e) {
            publishProgress(e.toString());
            e.printStackTrace();
        }
        return result;
    }

    private void sftpCloseSession() {
        this.sftpChannel.exit();
        this.session.disconnect();
        publishProgress("Session closed");
    }


    public String getFileName() {
        String result = null;
        if (this.imageUri.getScheme().equals("file")) {
            result = imageUri.getLastPathSegment();
        }
        return result;
    }

    protected void onProgressUpdate(String... progress) {
        //setProgressPercent(progress[0]);
    }


    protected void onPostExecute(Long result) {
        this.activity.finish();
    }
}
