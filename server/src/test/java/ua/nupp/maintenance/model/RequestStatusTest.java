package ua.nupp.maintenance.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestStatusTest {

    @Test
    void allowsOnlyOneStepForward() {
        assertTrue(RequestStatus.NEW.canMoveTo(RequestStatus.DIAGNOSED));
        assertTrue(RequestStatus.DIAGNOSED.canMoveTo(RequestStatus.IN_SERVICE));
        assertTrue(RequestStatus.IN_SERVICE.canMoveTo(RequestStatus.DONE));
    }

    @Test
    void forbidsSkippingBackwardAndSelfTransitions() {
        assertFalse(RequestStatus.NEW.canMoveTo(RequestStatus.IN_SERVICE));   // через крок
        assertFalse(RequestStatus.IN_SERVICE.canMoveTo(RequestStatus.NEW));   // назад
        assertFalse(RequestStatus.DIAGNOSED.canMoveTo(RequestStatus.DIAGNOSED));
        assertFalse(RequestStatus.NEW.canMoveTo(null));
    }

    @Test
    void doneIsFinal() {
        assertNull(RequestStatus.DONE.next());
        for (RequestStatus s : RequestStatus.values()) {
            assertFalse(RequestStatus.DONE.canMoveTo(s));
        }
    }
}
