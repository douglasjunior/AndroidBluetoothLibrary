/*
 * MIT License
 *
 * Copyright (c) 2015 Douglas Nassif Roma Junior
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.douglasjunior.bluetoothsample;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Code adapted from http://new-grumpy-mentat.blogspot.com.br/2014/06/java-escpos-image-printing.html
 */
public final class EscPosHelper {

    private EscPosHelper() {
    }

    private final static char ESC_CHAR = 0x1B;
    private final static byte[] FEED_LINE = {10};
    private final static byte[] SELECT_BIT_IMAGE_MODE = {ESC_CHAR, 0x2A, 33};
    private final static byte[] SET_LINE_SPACE_24 = new byte[]{ESC_CHAR, 0x33, 24};
    private final static byte[] SET_LINE_SPACE_30 = new byte[]{ESC_CHAR, 0x33, 30};

    /**
     * Send image to the printer to be printed.
     *
     * @param image 2D Array of RGB colors (Row major order)
     */
    public static byte[] printImage(Bitmap image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(SET_LINE_SPACE_24);
        for (int y = 0; y < image.getHeight(); y += 24) {
            baos.write(SELECT_BIT_IMAGE_MODE);// bit mode
            baos.write(new byte[]{(byte) (0x00ff & image.getWidth()), (byte) ((0xff00 & image.getWidth()) >> 8)});// width, low & high
            for (int x = 0; x < image.getWidth(); x++) {
                // For each vertical line/slice must collect 3 bytes (24 bytes)
                baos.write(collectSlice(y, x, image));
            }

            baos.write(FEED_LINE);
        }
        baos.write(SET_LINE_SPACE_30);

        return baos.toByteArray();
    }

    /**
     * Defines if a color should be printed (burned).
     *
     * @param color RGB color.
     * @return true if should be printed/burned (black), false otherwise (white).
     */
    private static boolean shouldPrintColor(int color) {
        final int threshold = 127;
        int a, r, g, b, luminance;
        a = (color >> 24) & 0xff;
        if (a != 0xff) { // ignore pixels with alpha channel
            return false;
        }
        r = (color >> 16) & 0xff;
        g = (color >> 8) & 0xff;
        b = color & 0xff;

        luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        return luminance < threshold;
    }

    /**
     * Collect a slice of 3 bytes with 24 dots for image printing.
     *
     * @param y     row position of the pixel.
     * @param x     column position of the pixel.
     * @param image 2D array of pixels of the image (RGB, row major order).
     * @return 3 byte array with 24 dots (field set).
     */
    private static byte[] collectSlice(int y, int x, Bitmap image) {
        byte[] slices = new byte[]{0, 0, 0};
        for (int yy = y, i = 0; yy < y + 24 && i < 3; yy += 8, i++) {// va a hacer 3 ciclos
            byte slice = 0;
            for (int b = 0; b < 8; b++) {
                int yyy = yy + b;
                if (yyy >= image.getHeight()) {
                    continue;
                }
                int color = image.getPixel(x, yyy);
                boolean v = shouldPrintColor(color);
                slice |= (byte) ((v ? 1 : 0) << (7 - b));
            }
            slices[i] = slice;
        }

        return slices;
    }
}
