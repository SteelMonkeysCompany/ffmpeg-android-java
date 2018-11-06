package com.github.hiteshsondhi88.libffmpeg;

import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

@SuppressWarnings("unused")
interface FFmpegInterface {

    /**
     * Load binary to the device according to archituecture. This also updates FFmpeg binary if the binary on device have old version.
     * @param ffmpegLoadBinaryResponseHandler {@link FFmpegLoadBinaryResponseHandler}
     * @throws FFmpegNotSupportedException
     */
    public void loadBinary(FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler) throws FFmpegNotSupportedException;

    /**
     * Executes a command
     * @param command
     * @throws FFmpegCommandAlreadyRunningException
     */
    public void execute(FFmpegCommand command) throws FFmpegCommandAlreadyRunningException;

    /**
     * Tells FFmpeg version currently on device
     * @return FFmpeg version currently on device
     * @throws FFmpegCommandAlreadyRunningException
     */
    public String getDeviceFFmpegVersion() throws FFmpegCommandAlreadyRunningException;

    /**
     * Tells FFmpeg version shipped with current library
     * @return FFmpeg version shipped with Library
     */
    public String getLibraryFFmpegVersion();

    /**
     * Checks if FFmpeg command is Currently running
     * @return true if FFmpeg command is running
     */
    public boolean isFFmpegCommandRunning();

    /**
     * Kill Running FFmpeg process
     * @return true if process is killed successfully
     */
    public boolean killRunningProcesses();

}
