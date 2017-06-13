package com.coolplayer.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

/**
 * Created by lingshuo on 2017/6/12.
 *
 * @author lingshuo
 * @date 2017/06/12
 */

public class MediaScannerFile {



    /**
     * 扫描指定的文件
     *
     * @param context
     * @param filePath
     * @param sListener
     */
    public static MediaScannerConnection scanFile(Context context, String[] filePath, String[] mineType,
                                                  MediaScannerConnection.OnScanCompletedListener sListener) {

        ClientProxy client = new ClientProxy(filePath, mineType, sListener);

        try {
            MediaScannerConnection  connection = new MediaScannerConnection(
                    context.getApplicationContext(), client);
            client.mConnection = connection;
            connection.connect();
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    static class ClientProxy implements MediaScannerConnection.MediaScannerConnectionClient {
        final String[] mPaths;
        final String[] mMimeTypes;
        final MediaScannerConnection.OnScanCompletedListener mClient;
        MediaScannerConnection mConnection;
        int mNextPath;

        ClientProxy(String[] paths, String[] mimeTypes,
                    MediaScannerConnection.OnScanCompletedListener client) {
            mPaths = paths;
            mMimeTypes = mimeTypes;
            mClient = client;
        }

        @Override
        public void onMediaScannerConnected() {
            scanNextPath();
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (mClient != null) {
                mClient.onScanCompleted(path, uri);
            }
            scanNextPath();
        }

        /**
         * 自动扫描下一个
         */
        void scanNextPath() {
            if (mNextPath >= mPaths.length) {
                mConnection.disconnect();
                return;
            }
            String mimeType = mMimeTypes != null ? mMimeTypes[mNextPath] : null;
            mConnection.scanFile(mPaths[mNextPath], mimeType);
            mNextPath++;
        }
    }
}

