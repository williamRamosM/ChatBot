package gui;

final class MouthAnimation {
    private static final double SILENCE = 0.04;
    private static final long MIN_FRAME_MS = 60;
    private static final long MAX_OPEN_MS = 150;

    private double recentPeak;
    private boolean open;
    private long nextChangeAt = Long.MIN_VALUE;
    private long closeBy;

    boolean update(double volume, long nowMillis) {
        recentPeak = Math.max(volume, recentPeak * 0.90);
        boolean next = open;

        if (volume <= SILENCE) {
            next = false;
        } else if (nowMillis >= nextChangeAt) {
            double relativeThreshold = recentPeak * (open ? 0.64 : 0.82);
            double threshold = Math.max(0.08, relativeThreshold);
            next = volume >= threshold && (!open || nowMillis < closeBy);
        }

        if (next != open) {
            open = next;
            nextChangeAt = nowMillis + MIN_FRAME_MS;
            closeBy = nowMillis + MAX_OPEN_MS;
        }
        return open;
    }
}
