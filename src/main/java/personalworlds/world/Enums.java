package personalworlds.world;

public class Enums {

    public enum DaylightCycle {

        SUN,
        MOON,
        CYCLE;

        public static DaylightCycle fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? DaylightCycle.CYCLE : values()[ordinal];
        }
    }
}
