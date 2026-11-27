package com.evsuite.launcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CustomIconsTest {
    @Test public void smallImagesAreDecodedWhole() {
        assertEquals(1, CustomIcons.sampleSize(288, 288, 288));
        assertEquals(1, CustomIcons.sampleSize(500, 100, 288));
    }

    @Test public void largeImagesStayBetweenTargetAndTwiceIt() {
        assertEquals(2, CustomIcons.sampleSize(576, 400, 288));
        // A 50-megapixel photo: 8160 / 16 = 510 pixels on the long side, never the full frame.
        int sample = CustomIcons.sampleSize(8160, 6120, 288);
        assertEquals(16, sample);
    }

    @Test public void theLongestSideDecidesSoAPanoramaIsBoundedToo() {
        assertEquals(64, CustomIcons.sampleSize(20000, 100, 288));
    }
}
