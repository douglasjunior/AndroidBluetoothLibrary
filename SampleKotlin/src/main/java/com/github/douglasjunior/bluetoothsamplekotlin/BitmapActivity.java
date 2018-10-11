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

package com.github.douglasjunior.bluetoothsamplekotlin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.wpx.util.WPXUtils;

/**
 * Created by douglas on 26/05/17.
 */

public class BitmapActivity extends AppCompatActivity implements View.OnClickListener {

    private FloatingActionButton mFab;
    private ImageView mImgOriginal;
    private ImageView mImgBlackWhite;

    private BluetoothService mService;

    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bitmap);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        mService = BluetoothService.getDefaultInstance();

        mImgOriginal = (ImageView) findViewById(R.id.img_original);
        mImgBlackWhite = (ImageView) findViewById(R.id.img_blackwhite);

        new Thread() {
            @Override
            public void run() {
                final Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.bmw);

                final Bitmap resized = Bitmap.createScaledBitmap(original, 255, 255, false);

                final Bitmap editedBrightness = BitmapHelper.changeBitmapContrastBrightness(resized, 1, 50);

                resized.recycle();

                imageBitmap = editedBrightness;

                final Bitmap editedGray = BitmapHelper.changeBitmapBlackWhite(editedBrightness);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mImgOriginal.setImageBitmap(original);
                        mImgBlackWhite.setImageBitmap(editedGray);
                    }
                });
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[] bytes = WPXUtils.decodeBitmap(imageBitmap);
                    mService.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
