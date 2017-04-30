package com.jean.work.model;

/**
 * Created by rantianhua on 17/4/28.
 */
public class FileMd5Model {

    public String filePath;
    public String md5;

    public FileMd5Model(String filePath, String md5) {
        this.filePath = filePath;
        this.md5 = md5;
    }
}
