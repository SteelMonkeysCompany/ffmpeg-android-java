package com.github.hiteshsondhi88.libffmpeg;

public interface FFmpegExecuteResponseHandler extends ResponseHandler {

    /**
     * on Success
     * @param message complete output of the FFmpeg command
     */
    public void onSuccess(String message);

    /**
     * on Progress
     * @param message current output of FFmpeg command
     */
    public void onProgress(String message);

    /**
     * on Progress
     * @param data current binary standard output of FFmpeg command
     * @param size size of fillled data
     */
    public void onProgress(byte[] data, int size);

    /**
     * on Failure
     * @param message complete output of the FFmpeg command
     */
    public void onFailure(String message);

}
