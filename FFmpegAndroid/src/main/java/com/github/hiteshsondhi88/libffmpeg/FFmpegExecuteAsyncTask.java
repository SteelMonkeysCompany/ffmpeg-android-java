package com.github.hiteshsondhi88.libffmpeg;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

class FFmpegExecuteAsyncTask extends AsyncTask<Void, Object, CommandResult> {

    private final ShellCommand shellCommand;
    private long startTime;
    private Process process;
    private String output = "";

    // Used for piping
    private int bufReaded = 0;
    private FFmpegCommand command;

    FFmpegExecuteAsyncTask(FFmpegCommand command) {
        this.command = command;
        this.shellCommand = new ShellCommand();
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
        if (command.ffmpegExecuteResponseHandler != null) {
            command.ffmpegExecuteResponseHandler.onStart();
        }
    }

    @Override
    protected CommandResult doInBackground(Void... params) {
        try {
            process = shellCommand.run(command.cmd);
            if (process == null) {
                return CommandResult.getDummyFailureResponse();
            }
            Log.d("Running publishing updates method");
            boolean pipe = command.cmd[command.cmd.length - 1].startsWith("pipe:");
            if (pipe) {
                checkAndUpdateProcessBinary();
            } else {
                checkAndUpdateProcess();
            }
            return CommandResult.getOutputFromProcess(process);
        } catch (TimeoutException e) {
            Log.e("FFmpeg timed out", e);
            return new CommandResult(false, e.getMessage());
        } catch (Exception e) {
            Log.e("Error running FFmpeg", e);
        } finally {
            Util.destroyProcess(process);
        }
        return CommandResult.getDummyFailureResponse();
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        if (values != null && values[0] != null && command.ffmpegExecuteResponseHandler != null) {
            if (values[0] instanceof String) {
                command.ffmpegExecuteResponseHandler.onProgress((String)values[0]);
            } else {
                command.ffmpegExecuteResponseHandler.onProgress((byte[])values[0]);
            }
        }
    }

    @Override
    protected void onPostExecute(CommandResult commandResult) {
        if (command.ffmpegExecuteResponseHandler != null) {
            output += commandResult.output;
            if (commandResult.success) {
                command.ffmpegExecuteResponseHandler.onSuccess(output);
            } else {
                command.ffmpegExecuteResponseHandler.onFailure(output);
            }
            command.ffmpegExecuteResponseHandler.onFinish();
        }
    }

    private void checkAndUpdateProcess() throws TimeoutException, InterruptedException {
        while (!Util.isProcessCompleted(process)) {
            // checking if process is completed
            if (Util.isProcessCompleted(process)) {
                return;
            }

            // Handling timeout
            if (command.timeout != Long.MAX_VALUE && System.currentTimeMillis() > startTime + command.timeout) {
                throw new TimeoutException("FFmpeg timed out");
            }

            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) {
                        return;
                    }

                    output += line+"\n";
                    publishProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isProcessCompleted() {
        return Util.isProcessCompleted(process);
    }

    private void checkAndUpdateProcessBinary() {
        int totalReaded = 0;
        byte[] buf = new byte[command.pipeSizeForPublishProgress];
        InputStream stream = process.getInputStream();
        while (!Util.isProcessCompleted(process)) {
            //Thread.sleep(1);
            totalReaded += readStream(buf, stream, false);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Log.e("Interrupted exception ", e);
        }
        totalReaded += readStream(buf, stream, true);
        if (bufReaded > 0)
        {
            Log.w("_pipeSizeForPublishProgress (" + command.pipeSizeForPublishProgress  + ") has incorrect values? Tail size is " + bufReaded);
            byte[] newBuf = new byte[bufReaded];
            System.arraycopy(buf, 0, newBuf, 0, bufReaded);
            buf = newBuf;
            publishProgress(buf);
        }
        publishProgress("Binary totalReaded " + totalReaded);
    }

    private int readStream(byte[] buf, InputStream stream, boolean forceReadToEnd)
    {
         int toRead = 0;
        int totalReaded = 0;
        int readed = 0;
        do {
            try {
                if (!forceReadToEnd && command.pipeSyncObject != null && command.pipeSyncObject.pause)
                {
                    return totalReaded;
                }

                toRead = Math.min(command.pipeBufferSize, command.pipeSizeForPublishProgress - bufReaded);
                readed = stream.read(buf, bufReaded, toRead);
                if (readed > 0) {
                    totalReaded += readed;
                    bufReaded += readed;
                    if (bufReaded == command.pipeSizeForPublishProgress)
                    {
                        publishProgress(buf, bufReaded);
                        buf = new byte[command.pipeSizeForPublishProgress];
                        bufReaded = 0;
                    }
                }
            } catch (Exception e) {
                Log.e(" stream.read exception!!", e);
            }
        } while (readed == toRead);
        return totalReaded;
    }

}
