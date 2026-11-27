package com.evsuite.launcher;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreferencesManagerMoveTest {
    private static List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @Test public void movingForwardShiftsTheTilesBetweenBack() {
        List<String> values = list("a", "b", "c", "d");
        assertTrue(PreferencesManager.move(values, 0, 2));
        assertEquals(list("b", "c", "a", "d"), values);
    }

    @Test public void movingBackwardShiftsTheTilesBetweenForward() {
        List<String> values = list("a", "b", "c", "d");
        assertTrue(PreferencesManager.move(values, 3, 0));
        assertEquals(list("d", "a", "b", "c"), values);
    }

    @Test public void movingToTheLastTileAppendsNothing() {
        List<String> values = list("a", "b", "c");
        assertTrue(PreferencesManager.move(values, 0, 2));
        assertEquals(list("b", "c", "a"), values);
    }

    @Test public void invalidOrNoOpMovesLeaveTheListAlone() {
        List<String> values = list("a", "b");
        assertFalse(PreferencesManager.move(values, 1, 1));
        assertFalse(PreferencesManager.move(values, -1, 0));
        assertFalse(PreferencesManager.move(values, 0, 2));
        assertFalse(PreferencesManager.move(values, 2, 0));
        assertEquals(list("a", "b"), values);
    }
}
