package fr.julien.packagedautoterminals.common;

/**
 * A gesture that only acts on its second click on the same target, within a short delay.
 *
 * <p>The first click arms it and returns false: the caller asks the player to click again.
 */
public final class SecondClick {

    private final long delay;
    private Object armed;
    private long expiry;

    public SecondClick(long delay) {
        this.delay = delay;
    }

    /** Is this click the second one on the same target, in time? Otherwise it arms. */
    public boolean confirms(Object target, long now) {
        if (target.equals(armed) && now <= expiry) {
            armed = null;
            return true;
        }
        armed = target;
        expiry = now + delay;
        return false;
    }
}
