package com.paradoxie.ffmpegdemo2;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.paradoxie.ffmpeglibrary.FFmpeg;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.SystemClock.currentThreadTimeMillis;

public class MainActivity extends AppCompatActivity {
    private static String Tag = "ffdemo";
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private String videoPath = null;
    private String videoFileName;
    private String videoFilePath;
    private String changedVideoPath;
    private ProgressDialog mProgressDialog;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: //视频处理完成
                    mProgressDialog.dismiss();
                    showAskDialog(msg.obj.toString());
                    break;
                case 1:
                    mProgressDialog.dismiss();
                    toastMessages("抱歉，视频处理失败了😔");
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //显示ProgressDialog
        createProgressDialog();
        mProgressDialog.dismiss();
        Button button1 = (Button) findViewById(R.id.demo_button_open_system_photo_file);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideo();
            }
        });
    }

    private void toastMessages(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgress(ProgressDialog.STYLE_SPINNER);//圆形
        mProgressDialog.setMessage("视频转化高宽比ing，请您稍等...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 当 requstCode 为 1001，且 data 不为空时，处理视频信息
        if (requestCode == 1001 && data != null) {
            Uri uri = data.getData();
            Log.d(Tag, "VideoUri-->" + uri);
            handleImageOnKitKat(data);
            getVideoHWScale();
        }
    }

    // 判断视频的高宽比
    private void getVideoHWScale() {
        Log.d(Tag, "getVideoHWScale invoke！" + " VideoPath is ->" + videoPath);
        // 获取视频参数
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(videoPath);
        int videoWidth = mExtractVideoInfoUtil.getVideoWidth();
        int videoHeight = mExtractVideoInfoUtil.getVideoHeight();
        int videoBitRate = mExtractVideoInfoUtil.getVideoBitrate();
        Log.d(Tag, "VideoWidth:" + videoWidth);
        Log.d(Tag, "VideoHeight:" + videoHeight);
        Log.d(Tag, "videoBitRate:" + videoBitRate);

        float scale = (float) videoHeight / (float) videoWidth;
        float scale_edge_max = 1.34f;
        float scale_edge_min = 1.32f;
        Log.d(Tag, "scale=" + scale);

        if (scale >= scale_edge_max || scale < scale_edge_min) {
            Log.d(Tag, "scale is not 4:3, need to add black edging！");
            addBlackEdge(videoHeight, videoWidth, videoBitRate, videoPath);
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
                Log.d(Tag, "videoId->:" + id);
//                String selection = MediaStore.Images.Media._ID + "=" + id;
                String selection = MediaStore.Video.Media._ID + "=" + id;
                videoPath = getVideoPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection);

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
        Log.d(Tag, "videoPath->:" + videoPath);
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

    private void addBlackEdge(int videoHeight, int videoWidth, int videoBitRate, String uri) {
        int finalHeight = (videoWidth * 3) / 4;
        int finalWidth = (finalHeight - videoHeight) / 2;
        Log.d("finalHeight", finalHeight + "");
        Log.d("finalWidth", finalWidth + "");
        // 生成 ffmpeg 命令
        // e.g.:ffmpeg -i /storage/emulated/0/DCIM/Camera/VID_20190706_211040.mp4 -vf pad=540:720:30 -b:v 7905760 /storage/emulated/0/DCIM/Camera/Changed_VID_20190706_211040.mp4
        final StringBuilder builder = new StringBuilder();
        builder.append("ffmpeg ");
        builder.append("-i ");
        builder.append(uri + " ");
        builder.append("-vf ");
        builder.append("pad=");
        builder.append(finalHeight + "");
        builder.append(":");
        builder.append(videoWidth + "");
        builder.append(":");
        builder.append(finalWidth + "");
        builder.append(" ");
        // 边框默认为黑色
//        builder.append(":");
//        builder.append("black ");
        // 如果是 H264 编码格式的视频，直接使用原画面,否者保留原视频码率
//        boolean isH264 = true;
//        if(isH264){
//            Log.d(Tag,"This video is belong to H264!");
        // 实验结果表明，加了这个，无法添加黑边
//            builder.append("-vcodec copy ");
//        } else {
//            Log.d(Tag,"This video is not H264");
        // 设置视频码率
        builder.append("-b:v ");
        builder.append(videoBitRate + " ");
//        }
        builder.append(videoFilePath);
        // 通过加时间戳的方式命名，防止文件覆盖
//        builder.append("Changed_" + Calendar.getInstance().getTimeInMillis() + "_" + videoFileName);
        builder.append("Changed_" + videoFileName);
        Log.d(Tag, "command = " + builder.toString());

        // 生成视频所处的路径
        final StringBuilder builder1 = new StringBuilder();
        builder1.append(videoFilePath);
        builder1.append("Changed_" + videoFileName);
        changedVideoPath = builder1.toString();
//        mProgressBar.setVisibility(View.VISIBLE);
        mProgressDialog.show();

        // 创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        cachedThreadPool.execute(new Runnable() {

            @Override
            public void run() {
                final long startTime = currentThreadTimeMillis();
                int result = FFmpeg.getsInstance().executeCommand(builder.toString().split(" "));
                final long endTime = SystemClock.currentThreadTimeMillis();
                Log.d("tag", "result = " + result);

                if (result == 0) {
                    Message msg = new Message();
                    msg.obj = (endTime - startTime) + "";
                    msg.what = 0;
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(1);
                }
            }
        });
    }

    private void showAskDialog(String costTime) {
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
//        normalDialog.setIcon(R.drawable.icon_dialog);
        normalDialog.setTitle("转化视频比例成功😊！");
        normalDialog.setMessage("耗时" + costTime + "毫秒，是否查看转码之后的视频？");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openVideo(changedVideoPath);
                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                        // 打开视频文件所处的文件夹
//                        openAssignFolder(videoFilePath);
                        return;
                    }
                });
        // 显示
        normalDialog.show();
    }

    // 打开转化之后的视频文件
    private void openVideo(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
//        String path = Environment.getExternalStorageDirectory().getPath()+ path;//该路径可以自定义
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }

    // 打开转化视频之后的文件夹
    private void openAssignFolder(String path) {
        Log.d(Tag, "openAssignFolder is invoke!");
        File file = new File(path);
        if (null == file || !file.exists()) {
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
