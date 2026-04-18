package com.vn.backend.utils;

import com.vn.backend.constants.AppConst;

public class FileUtils {

    public static String getFileNameFromDefaultUrl(String url){
        if(url == null || !url.startsWith(AppConst.ENDPOINT_DOWNLOAD_FILE)) {
            return null;
        }
        return url.substring(AppConst.ENDPOINT_DOWNLOAD_FILE.length());
    }
}
