package models;

import javax.sound.sampled.AudioFormat;

record SpeechLevels(int framesPerWindow, double[] levels) {

    static SpeechLevels fromPcm16(byte[] audio, AudioFormat format) {
        int framesPerWindow = Math.max(1, Math.round(format.getFrameRate() * 0.030f));
        int bytesPerWindow = framesPerWindow * format.getFrameSize();
        double[] levels = new double[(audio.length - 1) / bytesPerWindow + 1];

        for (int window = 0; window < levels.length; window++) {
            int start = window * bytesPerWindow;
            int end = start + Math.min(bytesPerWindow, audio.length - start);
            double squares = 0;
            for (int offset = start; offset < end; offset += 2) {
                short sample = (short) ((audio[offset] & 0xff) | (audio[offset + 1] << 8));
                double normalized = sample / 32768.0;
                squares += normalized * normalized;
            }
            double rms = Math.sqrt(squares / ((end - start) / 2));
            levels[window] = Math.min(1.0, rms * 3.0);
        }
        return new SpeechLevels(framesPerWindow, levels);
    }

    double atFrame(long frame) {
        long window = frame / framesPerWindow;
        return window < levels.length ? levels[(int) window] : 0;
    }
}
