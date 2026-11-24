package ua.nupp.maintenance.service;

/**
 * Розрахунок інтегрального показника пріоритету заявки:
 *
 *     I = 0,40·F1 + 0,35·F2 + 0,25·F3,
 *
 * де кожен фактор нормовано до шкали 0..100:
 *   F1 — критичність обладнання:   (criticality − 1) / 4 · 100, criticality ∈ [1; 5];
 *   F2 — тяжкість несправності:    (severity − 1) / 4 · 100,    severity ∈ [1; 5];
 *   F3 — вплив простою:            min(downtimeHours, 72) / 72 · 100.
 *
 * Простій понад 72 години (3 доби) вважається максимальним впливом.
 * Оскільки сума ваг дорівнює 1, I теж лежить у межах 0..100.
 */
public final class PriorityCalculator {
    public static final double W1 = 0.40;
    public static final double W2 = 0.35;
    public static final double W3 = 0.25;
    public static final int MAX_DOWNTIME_HOURS = 72;

    private PriorityCalculator() {
    }

    public static double f1(int criticality) {
        requireScale(criticality, "Критичність обладнання");
        return (criticality - 1) / 4.0 * 100.0;
    }

    public static double f2(int severity) {
        requireScale(severity, "Тяжкість несправності");
        return (severity - 1) / 4.0 * 100.0;
    }

    public static double f3(int downtimeHours) {
        if (downtimeHours < 0) {
            throw new ValidationException("Тривалість простою не може бути від'ємною");
        }
        return Math.min(downtimeHours, MAX_DOWNTIME_HOURS) * 100.0 / MAX_DOWNTIME_HOURS;
    }

    /** Повертає I, округлений до двох знаків після коми. */
    public static double calculate(int criticality, int severity, int downtimeHours) {
        double i = W1 * f1(criticality) + W2 * f2(severity) + W3 * f3(downtimeHours);
        return Math.round(i * 100.0) / 100.0;
    }

    private static void requireScale(int value, String what) {
        if (value < 1 || value > 5) {
            throw new ValidationException(what + " має бути в межах 1..5, отримано " + value);
        }
    }
}
