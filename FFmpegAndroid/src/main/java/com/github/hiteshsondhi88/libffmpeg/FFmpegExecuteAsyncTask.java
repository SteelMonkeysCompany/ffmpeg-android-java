package com.github.hiteshsondhi88.libffmpeg;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

class FFmpegExecuteAsyncTask extends AsyncTask<Void, Object, CommandResult> {

    private final String[] cmd;
    private final FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler;
    private final ShellCommand shellCommand;
    private final long timeout;
    private long startTime;
    private Process process;
    private String output = "";

    FFmpegExecuteAsyncTask(String[] cmd, long timeout, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.ffmpegExecuteResponseHandler = ffmpegExecuteResponseHandler;
        this.shellCommand = new ShellCommand();
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
        if (ffmpegExecuteResponseHandler != null) {
            ffmpegExecuteResponseHandler.onStart();
        }
    }

    @Override
    protected CommandResult doInBackground(Void... params) {
        try {
            process = shellCommand.run(cmd);
            if (process == null) {
                return CommandResult.getDummyFailureResponse();
            }
            Log.d("Running publishing updates method");
            boolean pipe = cmd[cmd.length - 1].startsWith("pipe:");
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
        if (values != null && values[0] != null && ffmpegExecuteResponseHandler != null) {
            if (values[0] instanceof String) {
                ffmpegExecuteResponseHandler.onProgress((String)values[0]);
            } else {
                ffmpegExecuteResponseHandler.onProgress((byte[])values[0], (int)values[1]);
            }
        }
    }

    @Override
    protected void onPostExecute(CommandResult commandResult) {
        if (ffmpegExecuteResponseHandler != null) {
            output += commandResult.output;
            if (commandResult.success) {
                ffmpegExecuteResponseHandler.onSuccess(output);
            } else {
                ffmpegExecuteResponseHandler.onFailure(output);
            }
            ffmpegExecuteResponseHandler.onFinish();
        }
    }

    private void checkAndUpdateProcess() throws TimeoutException, InterruptedException {
        while (!Util.isProcessCompleted(process)) {
            // checking if process is completed
            if (Util.isProcessCompleted(process)) {
                return;
            }

            // Handling timeout
            if (timeout != Long.MAX_VALUE && System.currentTimeMillis() > startTime + timeout) {
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
        byte[] buf = new byte[64 * 1024];
        InputStream stream = process.getInputStream();
        while (!Util.isProcessCompleted(process)) {
            //Thread.sleep(1);
            totalReaded += readStream(buf, stream);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Log.e("Interrupted exception ", e);
        }
        totalReaded += readStream(buf, stream);
        publishProgress("Binary totalReaded " + totalReaded);
    }

    private int readStream(byte[] buf, InputStream stream)
    {
        int totalReaded = 0;
        int readed = 0;
        do {
            try {
                readed = stream.read(buf, 0, buf.length);
                if (readed > 0) {
                    totalReaded += readed;
                    byte[] bufCopy = new byte[readed];
                    System.arraycopy(buf, 0, bufCopy, 0, readed);
                    publishProgress(bufCopy, readed);
                    //fs.Write(buf, 0, readed);
                }
            } catch (Exception e) {
                Log.e(" stream.read exception!!", e);
            }
        } while (readed == buf.length);
        return totalReaded;
    }

}
