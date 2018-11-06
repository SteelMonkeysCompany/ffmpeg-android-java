package com.github.hiteshsondhi88.libffmpeg;

import java.util.Map;

public class FFmpegCommand
{
    public static class SyncObject {
        public boolean pause = false;
    }
    public static int PIPE_BUFFER_SIZE = 64 * 1024;

    public String[] cmdArgs;
    public Map<String, String> environvenmentVars;
    String[] cmd;
    public long timeout = Long.MAX_VALUE;
    public FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler;
    public SyncObject pipeSyncObject;
    public int pipeSizeForPublishProgress = PIPE_BUFFER_SIZE;
    public int pipeBufferSize = PIPE_BUFFER_SIZE;
}
