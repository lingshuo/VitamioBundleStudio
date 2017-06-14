package com.coolplayer.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lingshuo on 2017/6/13.
 *
 * @author lingshuo
 * @date 2017/06/13
 */

public class MediaCenter {
    public static List<Mp3Info> mp3Infos = new ArrayList<>();
    public static List<HashMap<String, String>> mp3InfoList = new ArrayList<>();
    public static boolean isPlaying;
    public static boolean isPause;
    public static int current;
}
