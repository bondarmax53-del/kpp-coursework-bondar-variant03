package ua.nupp.maintenance.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityCalculatorTest {
    private static final double EPS = 1e-9;

    @Test
    void weightsSumToOne() {
        assertEquals(1.0, PriorityCalculator.W1 + PriorityCalculator.W2 + PriorityCalculator.W3, EPS);
    }

    @Test
    void factorsAreNormalizedTo0_100() {
        assertEquals(0.0, PriorityCalculator.f1(1), EPS);
        assertEquals(50.0, PriorityCalculator.f1(3), EPS);
        assertEquals(100.0, PriorityCalculator.f2(5), EPS);
        assertEquals(0.0, PriorityCalculator.f3(0), EPS);
        assertEquals(50.0, PriorityCalculator.f3(36), EPS);
        assertEquals(100.0, PriorityCalculator.f3(500), EPS);   // простій обмежено 72 год
    }

    @Test
    void minimumAndMaximum() {
        assertEquals(0.0, PriorityCalculator.calculate(1, 1, 0), EPS);
        assertEquals(100.0, PriorityCalculator.calculate(5, 5, 72), EPS);
    }

    @Test
    void exampleFromReport() {
        // F1 = 75, F2 = 50, F3 = 33,33 → 0,4·75 + 0,35·50 + 0,25·33,33 = 55,83
        assertEquals(55.83, PriorityCalculator.calculate(4, 3, 24), EPS);
    }

    @Test
    void criticalityHasTheBiggestWeight() {
        double moreCritical = PriorityCalculator.calculate(5, 1, 0);
        double moreSevere = PriorityCalculator.calculate(1, 5, 0);
        double longerDowntime = PriorityCalculator.calculate(1, 1, 72);
        assertTrue(moreCritical > moreSevere && moreSevere > longerDowntime);
    }

    @Test
    void rejectsValuesOutOfRange() {
        assertThrows(ValidationException.class, () -> PriorityCalculator.calculate(0, 3, 10));
        assertThrows(ValidationException.class, () -> PriorityCalculator.calculate(3, 6, 10));
        assertThrows(ValidationException.class, () -> PriorityCalculator.calculate(3, 3, -1));
    }
}
