package com.coolplayer.dto;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.coolplayer.R;

/**
 * 播放界面
 */

public class PlayerViewDto {
    public static final String MODE_SMALL_PLAYER = "small";
    public static final String MODE_LARGE_PLAYER = "large";
    public static final String MODE_WIDGET_PLAYER = "widget";
    public View root;
    public ImageView img_album;
    public TextView tv_timeNow;
    public TextView tv_timeTotal;
    public SeekBar seekBar_audio;
    public TextView tv_title;
    public TextView tv_subtitle;
    public ImageButton btn_play;
    public ImageButton btn_pause;
    public ImageButton btn_before;
    public ImageButton btn_next;
    public ImageButton btn_like;
    public ImageButton btn_dislike;
    public Button btn_mode;
    public Button btn_list;

    public void initView() {
        img_album = (ImageView) root.findViewById(R.id.img_album);
        tv_timeNow = (TextView) root.findViewById(R.id.tv_timeNow);
        tv_timeTotal = (TextView) root.findViewById(R.id.tv_timeTotal);
        seekBar_audio = (SeekBar) root.findViewById(R.id.seekBar_audio);
        tv_title = (TextView) root.findViewById(R.id.tv_title);
        tv_subtitle = (TextView) root.findViewById(R.id.tv_subtitle);
        btn_play = (ImageButton) root.findViewById(R.id.btn_play);
        btn_pause = (ImageButton) root.findViewById(R.id.btn_pause);
        btn_before = (ImageButton) root.findViewById(R.id.btn_before);
        btn_next = (ImageButton) root.findViewById(R.id.btn_next);
//        btn_like = (ImageButton) root.findViewById(R.id.btn_like);
//        btn_dislike = (ImageButton) root.findViewById(R.id.btn_dislike);
        btn_mode = (Button) root.findViewById(R.id.btn_mode);
        btn_list = (Button) root.findViewById(R.id.btn_list);
    }

    public void initData(String mode,Mp3Info data){
        switch (mode){
            case MODE_SMALL_PLAYER:
                break;
            case MODE_LARGE_PLAYER:
                break;
            case MODE_WIDGET_PLAYER:
                break;
        }
    }
}
