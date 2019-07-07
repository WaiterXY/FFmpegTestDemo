package com.maiml.ffmpegdemo2;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.maiml.ffmpeglibrary.FFmpeg;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

import static android.os.SystemClock.currentThreadTimeMillis;

public class MainActivity extends AppCompatActivity {
    private static String Tag = "ffdemo";
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private String videoPath = null;
    private String videoFileName;
    private String videoFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = (Button) findViewById(R.id.demo_button_open_system_photo_file);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideo();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 当 requstCode 为 1001，且 data 不为空时，处理视频信息
        if (requestCode == 1001 && data!= null) {
            Uri uri = data.getData();
            Log.d(Tag, "VideoUri-->" + uri);
            handleImageOnKitKat(data);
            getVideoHWScale();
        }
    }

    // 判断视频的高宽比
    private void getVideoHWScale() {
        Log.d(Tag, "getVideoHWScale invoke！" + " VideoPath is ->" + videoPath);
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(videoPath);
        int videoWidth = mExtractVideoInfoUtil.getVideoWidth();
        int videoHeight = mExtractVideoInfoUtil.getVideoHeight();
        Log.d(Tag, "VideoWidth:" + videoWidth);
        Log.d(Tag, "VideoHeight:" + videoHeight);
        float scale = (float) videoHeight / (float) videoWidth;
        float scale_edge_max = 1.34f;
        float scale_edge_min = 1.32f;
        Log.d(Tag, "scale=" + scale);
        if (scale >= scale_edge_max || scale < scale_edge_min) {
            Log.d(Tag, "scale is not 4:3, need to add black edging！");
            addBlackEdge(videoHeight, videoWidth, videoPath, videoPath);
        } else {
            Log.d(Tag, "scale is 4:3！ do nothing");
            Toast.makeText(this, "视频已经是 4：3 比例，无需转换", Toast.LENGTH_SHORT).show();
        }
    }

    // 获取本地视频文件的 uri
    private void handleImageOnKitKat(Intent data) {
        videoPath = null;
        Uri uri = data.getData();
        Log.d(Tag, "uri-1->" + uri);

        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                Log.d(Tag, "PathType->:" + 1);
                Log.d(Tag, uri.toString());
                String id = docId.split(":")[1];
                Log.d(Tag, "videoPath-id>:" + id);
//                String selection = MediaStore.Images.Media._ID + "=" + id;
                String selection = MediaStore.Video.Media._ID + "=" + id;
                Log.d(Tag, "videoPath-selection>:" + selection);
                videoPath = getVideoPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection);
                Log.d(Tag, "videoPath-11>:" + videoPath);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Log.d(Tag, "PathType->:" + 2);
                Log.d(Tag, uri.toString());
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                videoPath = getVideoPath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(Tag, "PathType->:" + 3);
            Log.d(Tag, "content: " + uri.toString());
            videoPath = getVideoPath(uri, null);
            Log.d(Tag, "videoPath->:" + videoPath);
        }
        Log.d(Tag, "videoPath-final>:" + videoPath);
        // 获取文件名
        videoFileName = videoPath.substring(videoPath.lastIndexOf("/") + 1);
        // 获取文件夹路径
        videoFilePath = videoPath.substring(0, videoPath.lastIndexOf("/") + 1);
        Log.d(Tag, "videoFileName is:" + videoFileName + " and videoFilePath is:" + videoFilePath);
    }

    private String getVideoPath(Uri uri, String selection) {
        Log.d(Tag, "getvideopath invoke!");
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    // 打开系统的视频文件夹
    private void chooseVideo() {
        Intent intent = new Intent();
        /* 开启Pictures画面Type设定为image */
        // intent.setType("image/*");
        // intent.setType("audio/*"); //选择音频
        // intent.setType("video/*;image/*");//同时选择视频和图片
        intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
        // 使用Intent.ACTION_GET_CONTENT这个Action
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // 取得视频后返回本画面
        startActivityForResult(intent, 1001);
    }

    private void addBlackEdge(int videoHeight, int videoWidth, String uri, String enduri) {
        String fileList = uri;
        String path = enduri;
        int finalHeight = (videoWidth * 3) / 4;
        int finalWidth = (finalHeight - videoHeight) / 2;
//        long startTime = 0;
//        long endTime = 0;
        Log.d("finalHeight", finalHeight + "");
        Log.d("finalWidth", finalWidth + "");

        final StringBuilder builder = new StringBuilder();
        builder.append("ffmpeg ");
        builder.append("-i ");
        builder.append(uri + " ");
        builder.append("-vf ");
//        builder.append("pad=540:720:30:0:black ");
        builder.append("pad=");
        builder.append(finalHeight + "");
        builder.append(":");
        builder.append(videoWidth + "");
        builder.append(":");
        builder.append(finalWidth + "");
        builder.append(" ");
//        builder.append(":");
//        builder.append("black ");
        builder.append(videoFilePath);
        builder.append("Changed_" + Calendar.getInstance().getTimeInMillis() + "_" + videoFileName);
//        builder.append("video" + "_" + videoFileName);
//        builder.append("/storage/emulated/0/video_20190707_121900.mp4");
        Log.e(Tag, "command = " + builder.toString());

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final long startTime = currentThreadTimeMillis();
//                Log.d(Tag, "commands is->" + builder.toString());
//                FFmpeg.getsInstance().executeCommand(builder.toString().split(" "));
//                final long endTime = SystemClock.currentThreadTimeMillis();
//            }
//        });
//        Log.d(Tag, "commands is->" + builder.toString());
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final long startTime = currentThreadTimeMillis();
//                FFmpeg.getsInstance().executeCommand(commands);
//                final long endTime = currentThreadTimeMillis();
//                runOnUiThread(new);
//            }
//        });
        final long startTime = currentThreadTimeMillis();
        int i = FFmpeg.getsInstance().executeCommand(builder.toString().split(" "));
        final long endTime = SystemClock.currentThreadTimeMillis();
        Log.e("tag", "----- res = " + i);
        Toast.makeText(this, "视频处理完毕，耗时" + (endTime - startTime) + "毫秒" + " 请在" + videoFilePath + "中查看 4：3 视频文件", Toast.LENGTH_LONG).show();
        openAssignFolder(videoFilePath);
    }

    private void openAssignFolder(String path){
        Log.d(Tag,"openAssignFolder is invoke!");
        File file = new File(path);
        if(null==file || !file.exists()){
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "file/*");
        try {
            startActivity(intent);
//            startActivity(Intent.createChooser(intent,"选择浏览工具"));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }


}