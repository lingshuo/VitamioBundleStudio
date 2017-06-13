package com.coolplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.coolplayer.dto.Constant;
import com.coolplayer.dto.MediaCenter;
import com.coolplayer.dto.Mp3Info;
import com.coolplayer.utils.MediaUtil;

import java.util.List;

import io.vov.vitamio.MediaPlayer;

public class PlayService extends Service {
    private MediaPlayer mediaPlayer; // 媒体播放器对象
    private String path;            // 音乐文件路径
    private int msg;
    private boolean isPause;        // 暂停状态
    private int current = 0;        // 记录当前正在播放的音乐
    private List<Mp3Info> mp3Infos;   //存放Mp3Info对象的集合
    private int status = 3;         //播放状态，默认为顺序播放
    private MyReceiver myReceiver;  //自定义广播接收器
    private long currentTime;        //当前播放进度
    private long duration;           //播放长度
    //一个循环的Handler，循环更新播放界面
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 1) {
                if(mediaPlayer != null) {
                    currentTime = mediaPlayer.getCurrentPosition(); // 获取当前音乐播放的位置
                    Intent intent = new Intent();
                    intent.setAction(Constant.MUSIC_CURRENT);
                    intent.putExtra(Constant.CURRENT, (int)currentTime);
                    sendBroadcast(intent); // 给PlayerActivity发送广播
                    handler.sendEmptyMessageDelayed(1, 1000);
                }

            }
        }
    };
    public PlayService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "service created");
        mediaPlayer = new MediaPlayer(this);
        mp3Infos = MediaCenter.mp3Infos;


        /**
         * 设置音乐播放完成时的监听器
         */
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (status == 1) { // 单曲循环
                    mediaPlayer.start();
                } else if (status == 2) { // 全部循环
                    current++;
                    if(current > mp3Infos.size() - 1) {  //变为第一首的位置继续播放
                        current = 0;
                    }
                    Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
                    sendIntent.putExtra(Constant.POS, current);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(current).getUrl();
                    play(0);
                } else if (status == 3) { // 顺序播放
                    current++;  //下一首位置
                    if (current <= mp3Infos.size() - 1) {
                        Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
                        sendIntent.putExtra(Constant.POS, current);
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                        path = mp3Infos.get(current).getUrl();
                        play(0);
                    }else {
                        mediaPlayer.seekTo(0);
                        current = 0;
                        Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
                        sendIntent.putExtra(Constant.POS, current);
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                    }
                } else if(status == 4) {    //随机播放
                    current = getRandomIndex(mp3Infos.size() - 1);
                    System.out.println("currentIndex ->" + current);
                    Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
                    sendIntent.putExtra(Constant.POS, current);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(current).getUrl();
                    play(0);
                }
            }
        });

        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.CTL_ACTION);
        registerReceiver(myReceiver, filter);
    }
    /**
     * 获取随机位置
     * @param end
     * @return
     */
    protected int getRandomIndex(int end) {
        int index = (int) (Math.random() * end);
        return index;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        path = intent.getStringExtra(Constant.URL);        //歌曲路径
        current = intent.getIntExtra(Constant.POS, -1);   //当前播放歌曲的在mp3Infos的位置
        msg = intent.getIntExtra(Constant.FLAG, 0);         //播放信息
        if (msg == Constant.PLAY_MSG) {    //直接播放音乐
            play(0);
        } else if (msg == Constant.PAUSE_MSG) {    //暂停
            pause();
        } else if (msg == Constant.STOP_MSG) {     //停止
            stop();
        } else if (msg == Constant.CONTINUE_MSG) { //继续播放
            resume();
        } else if (msg == Constant.PRIVIOUS_MSG) { //上一首
            previous();
        } else if (msg == Constant.NEXT_MSG) {     //下一首
            next();
        } else if (msg == Constant.PROGRESS_CHANGE) {  //进度更新
            currentTime = intent.getIntExtra(Constant.PROGRESS, -1);
            play(currentTime);
        } else if (msg == Constant.PLAYING_MSG) {
            handler.sendEmptyMessage(1);
        }
        super.onStart(intent, startId);
    }

    /**
     * 播放音乐
     *
     * @param currentTime
     */
    private void play(long currentTime) {
        try {
            mediaPlayer.reset();// 把各项参数恢复到初始状态
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare(); // 进行缓冲
            mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器
            handler.sendEmptyMessage(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 暂停音乐
     */
    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPause = true;
        }
    }

    private void resume() {
        if (isPause) {
            mediaPlayer.start();
            isPause = false;
        }
    }

    /**
     * 上一首
     */
    private void previous() {
        Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
        sendIntent.putExtra(Constant.POS, current);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }
    /**
     * 下一首
     */
    private void next() {
        Intent sendIntent = new Intent(Constant.UPDATE_ACTION);
        sendIntent.putExtra(Constant.POS, current);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }

    /**
     * 停止音乐
     */
    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }
    /**
     *
     * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
     *
     */
    private final class PreparedListener implements MediaPlayer.OnPreparedListener {
        private long currentTime;

        public PreparedListener(long currentTime) {
            this.currentTime = currentTime;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start(); // 开始播放
            if (currentTime > 0) { // 如果音乐不是从头播放
                mediaPlayer.seekTo(currentTime);
            }
            Intent intent = new Intent();
            intent.setAction(Constant.MUSIC_DURATION);
            duration = mediaPlayer.getDuration();
            intent.putExtra(Constant.DURATION, (int)duration);  //通过Intent来传递歌曲的总长度
            intent.putExtra(Constant.CURRENT,currentTime);
            intent.putExtra(Constant.POS, current);
            sendBroadcast(intent);
        }
    }

    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra(Constant.CONTROL, -1);
            switch (control) {
                case 1:
                    status = 1; // 将播放状态置为1表示：单曲循环
                    break;
                case 2:
                    status = 2; //将播放状态置为2表示：全部循环
                    break;
                case 3:
                    status = 3; //将播放状态置为3表示：顺序播放
                    break;
                case 4:
                    status = 4; //将播放状态置为4表示：随机播放
                    break;
            }
        }
    }
}
