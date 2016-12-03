/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.graphics.Bitmap;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import barcodescanner.xservices.nl.barcodescanner.R;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;
  private int frameCount;

  DecodeHandler(CaptureActivity activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (!running) {
      return;
    }
    if (message.what == R.id.decode) {
      decode((byte[]) message.obj, message.arg1, message.arg2);

    } else if (message.what == R.id.quit) {
      running = false;
      Looper.myLooper().quit();

    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;

    if (frameCount == 3) {
      frameCount = 0;
      int[] argb = new int[width*height];
      YUV_NV21_TO_RGB(argb, data, width, height);
      for (int i = 0; i < argb.length; i++) {
        argb[i] = 0xffffff - argb[i];
      }
      encodeYUV420SP(data, argb, width, height);
    }
    frameCount++;

    PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
    if (source != null) {
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      try {
        rawResult = multiFormatReader.decodeWithState(bitmap);
      } catch (ReaderException re) {
        // continue
      } finally {
        multiFormatReader.reset();
      }
    }

    Handler handler = activity.getHandler();
    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
        bundleThumbnail(source, bundle);
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_failed);
        message.sendToTarget();
      }
    }
  }

  private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
  }

  private static void YUV_NV21_TO_RGB(int[] argb, byte[] yuv, int width, int height) {
    final int frameSize = width * height;

    final int ii = 0;
    final int ij = 0;
    final int di = +1;
    final int dj = +1;

    int a = 0;
    for (int i = 0, ci = ii; i < height; ++i, ci += di) {
      for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
        int y = (0xff & ((int) yuv[ci * width + cj]));
        int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
        int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
        y = y < 16 ? 16 : y;

        int a0 = 1192 * (y - 16);
        int a1 = 1634 * (v - 128);
        int a2 = 832 * (v - 128);
        int a3 = 400 * (u - 128);
        int a4 = 2066 * (u - 128);

        int r = (a0 + a1) >> 10;
        int g = (a0 - a2 - a3) >> 10;
        int b = (a0 + a4) >> 10;

        r = r < 0 ? 0 : (r > 255 ? 255 : r);
        g = g < 0 ? 0 : (g > 255 ? 255 : g);
        b = b < 0 ? 0 : (b > 255 ? 255 : b);

        argb[a++] = 0x000000 | (r << 16) | (g << 8) | b;
      }
    }
  }

  void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
    final int frameSize = width * height;

    int yIndex = 0;
    int uIndex = frameSize;
    int vIndex = frameSize+((yuv420sp.length-frameSize)/2);
    System.out.println(yuv420sp.length+" "+frameSize);


    int a, R, G, B, Y, U, V;
    int index = 0;
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {

        a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
        R = (argb[index] & 0xff0000) >> 16;
        G = (argb[index] & 0xff00) >> 8;
        B = (argb[index] & 0xff) >> 0;

        // well known RGB to YUV algorithm

        Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
        U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
        V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

        // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
        //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
        //    pixel AND every other scanline.
        yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
        if (j % 2 == 0 && index % 2 == 0) {
          yuv420sp[uIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
          yuv420sp[vIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
        }

        index ++;
      }
    }
  }

}
