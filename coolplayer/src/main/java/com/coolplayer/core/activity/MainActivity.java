package com.coolplayer.core.activity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Toast;

import com.coolplayer.R;
import com.coolplayer.core.adapter.MyFileItemRecyclerViewAdapter;
import com.coolplayer.core.view.BottomPlayerView;
import com.coolplayer.dto.Constant;
import com.coolplayer.dto.MediaCenter;
import com.coolplayer.dto.Mp3Info;
import com.coolplayer.service.PlayService;
import com.coolplayer.utils.MediaUtil;
import com.coolplayer.widget.MyItemClickListener;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static android.R.drawable.ic_media_pause;
import static android.R.drawable.ic_media_play;
import static com.coolplayer.dto.Constant.CTL_ACTION;
import static com.coolplayer.dto.Constant.MUSIC_CURRENT;
import static com.coolplayer.dto.Constant.MUSIC_DURATION;
import static com.coolplayer.dto.Constant.UPDATE_ACTION;
import static com.coolplayer.dto.MediaCenter.current;
import static com.coolplayer.dto.MediaCenter.isPause;
import static com.coolplayer.dto.MediaCenter.isPlaying;
import static com.coolplayer.dto.MediaCenter.mp3Infos;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements MyItemClickListener {
    private static final String TAG = "MainActivity";
    private RecyclerView list;
    BottomPlayerView bottomPlayerView;
    private MediaPlayer mMediaPlayer;
    private Cursor cursor;

    private String title;       //歌曲标题
    private String artist;      //歌曲艺术家
    private String url;         //歌曲路径
    private long currentTime;    //当前歌曲播放时间
    private long duration;       //歌曲长度
    private int flag;           //播放标识

    private int repeatState;
    private final int isCurrentRepeat = 1; // 单曲循环
    private final int isAllRepeat = 2;      // 全部循环
    private final int isNoneRepeat = 3;     // 无重复播放
    private boolean isNoneShuffle;           // 顺序播放
    private boolean isShuffle;          // 随机播放
    private PlayerReceiver playerReceiver;
    private Bitmap mBitmap;

    private static final int WHAT_INIT = 2001;
    private static final int WHAT_TIME = 2002;
    private static final int WHAT_UPDATE = 2003;

    private NotificationManager manager;
    private RemoteViews remoteViews;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            updateNotify();
            switch (msg.what) {
                case WHAT_INIT:
                    flag = Constant.PLAYING_MSG;
                    isPlaying = true;
                    isPause = false;
                    initUI(current, flag);
                    break;
                case WHAT_TIME:
                    bottomPlayerView.mPlayView.tv_timeNow.setText(MediaUtil.formatTime(currentTime));
                    bottomPlayerView.mPlayView.seekBar_audio.setProgress((int) currentTime);
                    break;
                case WHAT_UPDATE:
                    isPlaying = true;
                    isPause = false;
                    flag = Constant.CONTINUE_MSG;
                    initUI(current, flag);
                    break;
            }
        }
    };

    private void updateNotify() {
        if(current>=0 && mp3Infos!=null){
            try{
                Mp3Info info =mp3Infos.get(current);
                // 设置通知栏的图片文字
                remoteViews =new RemoteViews(getPackageName(),R.layout.notice);
                remoteViews.setImageViewBitmap(R.id.widget_album, this.mBitmap);
                remoteViews.setTextViewText(R.id.widget_title, info.getTitle());
                remoteViews.setTextViewText(R.id.widget_artist, info.getArtist());
                if (isPlaying) {
                    remoteViews.setImageViewResource(R.id.widget_play, ic_media_play);
                }else {
                    remoteViews.setImageViewResource(R.id.widget_play, ic_media_pause);
                }

                setNotification();
            }catch (Exception e){
                e.getLocalizedMessage();
            }
        }
    }

    private void initUI(int current, int flag) {
        try {
            Mp3Info info = mp3Infos.get(current);
            title = info.getTitle();
            artist = info.getArtist();
            duration = info.getDuration();
            bottomPlayerView.mPlayView.tv_title.setText(title);
            bottomPlayerView.mPlayView.tv_subtitle.setText(artist);
            bottomPlayerView.mPlayView.tv_timeNow.setText(MediaUtil.formatTime(0));
            bottomPlayerView.mPlayView.tv_timeTotal.setText(MediaUtil.formatTime(duration));
            bottomPlayerView.mPlayView.seekBar_audio.setProgress((int) currentTime);
            bottomPlayerView.mPlayView.seekBar_audio.setMax((int) duration);

            try {
                if(mBitmap!=null){
                    mBitmap.recycle();
                }
                mBitmap = MediaUtil.getArtwork(this, info.getId(), info.getAlbumId(), true, true);
                bottomPlayerView.mPlayView.img_album.setImageBitmap(mBitmap);
            } catch (Exception e) {
                e.getLocalizedMessage();
            }
            if(isPlaying){
                showPlayIcon(false);
            }else{
                showPlayIcon(true);
            }
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
    }

    private void showPlayIcon(boolean show) {
        if (show) {
            bottomPlayerView.mPlayView.btn_pause.setVisibility(View.INVISIBLE);
            bottomPlayerView.mPlayView.btn_play.setVisibility(View.VISIBLE);
        } else {
            bottomPlayerView.mPlayView.btn_pause.setVisibility(View.VISIBLE);
            bottomPlayerView.mPlayView.btn_play.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vitamio.isInitialized(getApplicationContext());
        setContentView(R.layout.activity_main);
        list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        bottomPlayerView = (BottomPlayerView) findViewById(R.id.buttom_player_view);
        //请求权限
        MainActivityPermissionsDispatcher.requestPermissionWithCheck(this, this);
        if (cursor != null) {
            MediaCenter.mp3Infos = MediaUtil.getMusicInfo(cursor);
            MediaCenter.mp3InfoList = MediaUtil.getMusicMaps(mp3Infos);
            list.setAdapter(new MyFileItemRecyclerViewAdapter(MediaCenter.mp3InfoList, this));
        }
        ViewOnclickListener ViewOnClickListener = new ViewOnclickListener();
        bottomPlayerView.mPlayView.btn_play.setOnClickListener(ViewOnClickListener);
        bottomPlayerView.mPlayView.btn_pause.setOnClickListener(ViewOnClickListener);
        bottomPlayerView.mPlayView.btn_before.setOnClickListener(ViewOnClickListener);
        bottomPlayerView.mPlayView.btn_next.setOnClickListener(ViewOnClickListener);
        bottomPlayerView.mPlayView.seekBar_audio.setOnSeekBarChangeListener(new SeekBarChangeListener());
        showPlayIcon(true);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 请求权限
     *
     * @param context
     */
    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void requestPermission(Context context) {
        this.cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
        initUI(current, flag);
    }


    private void registerReceiver() {
        playerReceiver = new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        registerReceiver(playerReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBitmap!=null){
            mBitmap.recycle();
            mBitmap=null;
        }
        unregisterReceiver(playerReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (remoteViews != null) {
            manager.cancel(100);
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
        if (cursor != null) {
            MediaCenter.mp3Infos = MediaUtil.getMusicInfo(cursor);
            MediaCenter.mp3InfoList = MediaUtil.getMusicMaps(mp3Infos);
            list.setAdapter(new MyFileItemRecyclerViewAdapter(MediaCenter.mp3InfoList, this));
        }
    }

    @Override
    public void onItemClick(View view, int postion) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, PlayService.class);
        intent.putExtra(Constant.URL, mp3Infos.get(postion).getUrl());
        intent.putExtra(Constant.POS, postion);
        intent.putExtra(Constant.FLAG, Constant.PLAY_MSG);
        startService(intent);
    }


    /**
     * 控件点击事件
     *
     * @author wwj
     */
    private class ViewOnclickListener implements View.OnClickListener {
        Intent intent = new Intent();

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_play:
                case R.id.btn_pause:
                    if (isPlaying) {
                        showPlayIcon(true);
                        intent.setClass(MainActivity.this, PlayService.class);
                        intent.putExtra(Constant.FLAG, Constant.PAUSE_MSG);
                        startService(intent);
                        isPlaying = false;
                        isPause = true;

                    } else if (isPause) {
                        showPlayIcon(false);
                        intent.setClass(MainActivity.this, PlayService.class);
                        intent.putExtra(Constant.FLAG, Constant.CONTINUE_MSG);
                        startService(intent);
                        isPause = false;
                        isPlaying = true;
                    } else {
                        url = mp3Infos.get(current).getUrl();
                        flag = Constant.PLAY_MSG;
                        play();
                        break;
                    }
                    break;
                case R.id.btn_before:       //上一首歌曲
                    previous_music();
                    break;
                case R.id.btn_next:           //下一首歌曲
                    next();
                    break;
//                case R.id.repeat_music:         //重复播放音乐
//                    if (repeatState == isNoneRepeat) {
//                        repeat_one();
//                        shuffleBtn.setClickable(false); //是随机播放变为不可点击状态
//                        repeatState = isCurrentRepeat;
//                    } else if (repeatState == isCurrentRepeat) {
//                        repeat_all();
//                        shuffleBtn.setClickable(false);
//                        repeatState = isAllRepeat;
//                    } else if (repeatState == isAllRepeat) {
//                        repeat_none();
//                        shuffleBtn.setClickable(true);
//                        repeatState = isNoneRepeat;
//                    }
//                    Intent intent = new Intent(REPEAT_ACTION);
//                    switch (repeatState) {
//                        case isCurrentRepeat: // 单曲循环
//                            repeatBtn
//                                    .setBackgroundResource(R.drawable.repeat_current_selector);
//                            Toast.makeText(PlayerActivity.this, R.string.repeat_current,
//                                    Toast.LENGTH_SHORT).show();
//
//
//                            intent.putExtra("repeatState", isCurrentRepeat);
//                            sendBroadcast(intent);
//                            break;
//                        case isAllRepeat: // 全部循环
//                            repeatBtn
//                                    .setBackgroundResource(R.drawable.repeat_all_selector);
//                            Toast.makeText(PlayerActivity.this, R.string.repeat_all,
//                                    Toast.LENGTH_SHORT).show();
//                            intent.putExtra("repeatState", isAllRepeat);
//                            sendBroadcast(intent);
//                            break;
//                        case isNoneRepeat: // 无重复
//                            repeatBtn
//                                    .setBackgroundResource(R.drawable.repeat_none_selector);
//                            Toast.makeText(PlayerActivity.this, R.string.repeat_none,
//                                    Toast.LENGTH_SHORT).show();
//                            intent.putExtra("repeatState", isNoneRepeat);
//                            break;
//                    }
//                    break;
//                case R.id.shuffle_music:            //随机播放状态
//                    Intent shuffleIntent = new Intent(SHUFFLE_ACTION);
//                    if (isNoneShuffle) {            //如果当前状态为非随机播放，点击按钮之后改变状态为随机播放
//                        shuffleBtn
//                                .setBackgroundResource(R.drawable.shuffle_selector);
//                        Toast.makeText(PlayerActivity.this, R.string.shuffle,
//                                Toast.LENGTH_SHORT).show();
//                        isNoneShuffle = false;
//                        isShuffle = true;
//                        shuffleMusic();
//                        repeatBtn.setClickable(false);
//                        shuffleIntent.putExtra("shuffleState", true);
//                        sendBroadcast(shuffleIntent);
//                    } else if (isShuffle) {
//                        shuffleBtn
//                                .setBackgroundResource(R.drawable.shuffle_none_selector);
//                        Toast.makeText(PlayerActivity.this, R.string.shuffle_none,
//                                Toast.LENGTH_SHORT).show();
//                        isShuffle = false;
//                        isNoneShuffle = true;
//                        repeatBtn.setClickable(true);
//                        shuffleIntent.putExtra("shuffleState", false);
//                        sendBroadcast(shuffleIntent);
//                    }
//                    break;
            }
        }
    }


    /**
     * 实现监听Seekbar的类
     *
     * @author wwj
     */
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if (fromUser) {
                mHandler.removeMessages(WHAT_TIME);
                audioTrackChange(progress); //用户控制进度的改变
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

    }

    /**
     * 播放音乐
     */
    public void play() {
        //开始播放的时候为顺序播放
        repeat_none();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, PlayService.class);
        intent.putExtra(Constant.URL, url);
        intent.putExtra(Constant.POS, current);
        intent.putExtra(Constant.FLAG, flag);
        startService(intent);
    }

    /**
     * 随机播放
     */
    public void shuffleMusic() {
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra(Constant.CONTROL, 4);
        sendBroadcast(intent);
    }

    public void audioTrackChange(int progress) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, PlayService.class);
        intent.putExtra(Constant.URL, url);
        intent.putExtra(Constant.POS, current);
        if (isPause) {
            intent.putExtra(Constant.FLAG, Constant.PAUSE_MSG);
        } else {
            intent.putExtra(Constant.FLAG, Constant.PROGRESS_CHANGE);
        }
        intent.putExtra(Constant.PROGRESS, progress);
        startService(intent);
        mHandler.obtainMessage(WHAT_TIME);
    }

    /**
     * 单曲循环
     */
    public void repeat_one() {
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra(Constant.CONTROL, 1);
        sendBroadcast(intent);
    }

    /**
     * 全部循环
     */
    public void repeat_all() {
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra(Constant.CONTROL, 2);
        sendBroadcast(intent);
    }

    /**
     * 顺序播放
     */
    public void repeat_none() {
        Intent intent = new Intent(CTL_ACTION);
        intent.putExtra(Constant.CONTROL, 3);
        sendBroadcast(intent);
    }

    /**
     * 上一首
     */
    public void previous_music() {
        current = current - 1;
        if (current >= 0) {
            Mp3Info mp3Info = mp3Infos.get(current);   //上一首MP3
            bottomPlayerView.mPlayView.tv_title.setText(mp3Info.getTitle());
            bottomPlayerView.mPlayView.tv_subtitle.setText(mp3Info.getArtist());
            url = mp3Info.getUrl();
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, PlayService.class);
            intent.putExtra(Constant.URL, mp3Info.getUrl());
            intent.putExtra(Constant.POS, current);
            intent.putExtra(Constant.FLAG, Constant.PRIVIOUS_MSG);
            startService(intent);
        } else {
            Toast.makeText(this, "没有上一首了", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 下一首
     */
    public void next() {
        bottomPlayerView.mPlayView.btn_play.setVisibility(View.VISIBLE);
        current = current + 1;
        if (current <= mp3Infos.size() - 1) {
            Mp3Info mp3Info = mp3Infos.get(current);
            url = mp3Info.getUrl();
            bottomPlayerView.mPlayView.tv_title.setText(mp3Info.getTitle());
            bottomPlayerView.mPlayView.tv_subtitle.setText(mp3Info.getArtist());
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, PlayService.class);
            intent.putExtra(Constant.URL, mp3Info.getUrl());
            intent.putExtra(Constant.POS, current);
            intent.putExtra(Constant.FLAG, Constant.NEXT_MSG);
            startService(intent);
        } else {
            Toast.makeText(this, "没有下一首了", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 用来接收从service传回来的广播的内部类
     *
     * @author wwj
     */
    public class PlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            if (action.equals(MUSIC_CURRENT)) {
                currentTime = intent.getIntExtra(Constant.CURRENT, -1);
                current = intent.getIntExtra(Constant.POS, -1);
                mHandler.sendEmptyMessage(WHAT_TIME);
//                bottomPlayerView.mPlayView.tv_timeNow.setText(MediaUtil.formatTime(currentTime));
//                bottomPlayerView.mPlayView.seekBar_audio.setProgress((int)currentTime);
            } else if (action.equals(MUSIC_DURATION)) {
                duration = intent.getIntExtra(Constant.DURATION, -1);
                current = intent.getIntExtra(Constant.POS, -1);
                currentTime = intent.getLongExtra(Constant.CURRENT, -1);
                mHandler.sendEmptyMessage(WHAT_INIT);
//                bottomPlayerView.mPlayView.seekBar_audio.setMax(duration);
//                bottomPlayerView.mPlayView.tv_timeTotal.setText(MediaUtil.formatTime(duration));
            } else if (action.equals(UPDATE_ACTION)) {
                //获取Intent中的current消息，current代表当前正在播放的歌曲
                current = intent.getIntExtra(Constant.POS, -1);
                url = mp3Infos.get(current).getUrl();
                duration = intent.getIntExtra(Constant.DURATION, -1);
                mHandler.sendEmptyMessage(WHAT_UPDATE);
//                if(current >= 0) {
//                    bottomPlayerView.mPlayView.tv_title.setText(mp3Infos.get(current).getTitle());
//                    bottomPlayerView.mPlayView.tv_subtitle.setText(mp3Infos.get(current).getArtist());
//                }
//                if(current == 0) {
//                    bottomPlayerView.mPlayView.tv_timeTotal.setText(MediaUtil.formatTime(mp3Infos.get(current).getDuration()));
//                    bottomPlayerView.mPlayView.btn_play.setVisibility(View.VISIBLE);
//                    bottomPlayerView.mPlayView.btn_pause.setVisibility(View.GONE);
//                    isPause = true;
//                }
            }
        }

    }


    //通知栏播放
    private void setNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        Intent intent = new Intent(this, MainActivity.class);
        // 点击跳转到主界面
        PendingIntent intent_go = PendingIntent.getActivity(this, 5, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice, intent_go);

        // 4个参数context, requestCode, intent, flags
        PendingIntent intent_close = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_close, intent_close);

        // 设置上一曲
        Intent prv = new Intent();
        prv.setAction(Constant.COL_BEFORE);
        PendingIntent intent_prev = PendingIntent.getBroadcast(this, 1, prv,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev, intent_prev);

        // 设置播放
        if (isPlaying) {
            Intent playorpause = new Intent();
            playorpause.setAction(Constant.COL_PAUSE);
            PendingIntent intent_play = PendingIntent.getBroadcast(this, 2,
                    playorpause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, intent_play);
        }
        if (!isPlaying) {
            Intent playorpause = new Intent();
            playorpause.setAction(Constant.COL_PLAY);
            PendingIntent intent_play = PendingIntent.getBroadcast(this, 6,
                    playorpause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, intent_play);
        }

        // 下一曲
        Intent next = new Intent();
        next.setAction(Constant.COL_NEXT);
        PendingIntent intent_next = PendingIntent.getBroadcast(this, 3, next,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_next, intent_next);

//        // 设置收藏
//        PendingIntent intent_fav = PendingIntent.getBroadcast(this, 4, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        remoteViews.setOnClickPendingIntent(R.id.widget_fav, intent_fav);

        builder.setSmallIcon(R.drawable.ic_launcher); // 设置顶部图标
        builder.setCustomBigContentView(remoteViews);
        Notification notify = builder.build();
        notify.contentView = remoteViews; // 设置下拉图标
        notify.bigContentView = remoteViews; // 防止显示不完全,需要添加apisupport
        notify.flags = Notification.FLAG_ONGOING_EVENT;
        notify.icon = R.drawable.ic_launcher;

        manager.notify(100, notify);
    }
}

