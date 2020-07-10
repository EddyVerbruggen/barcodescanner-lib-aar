/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.common.detector;


import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

/**
 * @author Mariusz Dąbrowski
 */
public final class CornerDetector {

  private final BitMatrix image;
  private final int height;
  private final int width;
  private final int leftInit;
  private final int rightInit;
  private final int downInit;
  private final int upInit;
  private final int targetMatrixSize;


  /**
   * @throws NotFoundException if image is too small to accommodate {@code initSize}
   */
  public CornerDetector(BitMatrix image, int initSize, int x, int y, int targetMatrixSize) throws NotFoundException {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    int halfsize = initSize / 2;
    leftInit = x - halfsize;
    rightInit = x + halfsize;
    upInit = y - halfsize;
    downInit = y + halfsize;
    this.targetMatrixSize = targetMatrixSize * 2;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * @throws NotFoundException if no Data Matrix Code can be found
   */
  public ResultPoint[] detect() throws NotFoundException {

    int left = leftInit;
    int right = rightInit;
    int up = upInit;
    int down = downInit;
    boolean sizeExceeded = false;
    boolean aBlackPointFoundOnBorder = true;
    boolean atLeastOneBlackPointFoundOnBorder = false;

    boolean atLeastOneBlackPointFoundOnRight = false;
    boolean atLeastOneBlackPointFoundOnBottom = false;
    boolean atLeastOneBlackPointFoundOnLeft = false;
    boolean atLeastOneBlackPointFoundOnTop = false;

    while (aBlackPointFoundOnBorder) {

      aBlackPointFoundOnBorder = false;

      // .....
      // . |
      // .....
      boolean rightBorderNotWhite = true;
      while ((rightBorderNotWhite || !atLeastOneBlackPointFoundOnRight) && right < width) {
        rightBorderNotWhite = containsBlackPoint(up, down, right, false);
        if (rightBorderNotWhite) {
          right++;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnRight = true;
        } else if (!atLeastOneBlackPointFoundOnRight) {
          right++;
        }
      }

      if (right >= width) {
        sizeExceeded = true;
        break;
      }

      // .....
      // . .
      // .___.
      boolean bottomBorderNotWhite = true;
      while ((bottomBorderNotWhite || !atLeastOneBlackPointFoundOnBottom) && down < height) {
        bottomBorderNotWhite = containsBlackPoint(left, right, down, true);
        if (bottomBorderNotWhite) {
          down++;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnBottom = true;
        } else if (!atLeastOneBlackPointFoundOnBottom) {
          down++;
        }
      }

      if (down >= height) {
        sizeExceeded = true;
        break;
      }

      // .....
      // | .
      // .....
      boolean leftBorderNotWhite = true;
      while ((leftBorderNotWhite || !atLeastOneBlackPointFoundOnLeft) && left >= 0) {
        leftBorderNotWhite = containsBlackPoint(up, down, left, false);
        if (leftBorderNotWhite) {
          left--;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnLeft = true;
        } else if (!atLeastOneBlackPointFoundOnLeft) {
          left--;
        }
      }

      if (left < 0) {
        sizeExceeded = true;
        break;
      }

      // .___.
      // . .
      // .....
      boolean topBorderNotWhite = true;
      while ((topBorderNotWhite || !atLeastOneBlackPointFoundOnTop) && up >= 0) {
        topBorderNotWhite = containsBlackPoint(left, right, up, true);
        if (topBorderNotWhite) {
          up--;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnTop = true;
        } else if (!atLeastOneBlackPointFoundOnTop) {
          up--;
        }
      }

      if (up < 0) {
        sizeExceeded = true;
        break;
      }

      if (aBlackPointFoundOnBorder) {
        atLeastOneBlackPointFoundOnBorder = true;
      }
    }

    if (!sizeExceeded && atLeastOneBlackPointFoundOnBorder) {
      return findCorners(right, left, down, up);
    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  private ResultPoint[] findCorners(int right, int left, int down, int up) throws NotFoundException {
    //
    //      A------------              ------------B
    //      |           |      up      |           |
    //      |    -------|--------------|-------    |
    //      |    |      |              |      |    |
    //      |    |      |              |      |    |
    //      ------------AP            BP------------
    //           |                            |
    //           |                            |
    //      left |                            | right
    //           |                            |
    //           |                            |
    //      ------------DP            CP------------
    //      |    |      |             |       |    |
    //      |    |      |   down      |       |    |
    //      |    -------|-------------|--------    |
    //      |           |             |            |
    //      D-----------|             |------------C
    //


    float width = right - left;
    float height = down - up;
    float sampler = 16f / targetMatrixSize;
    float sampler2 = 4f / targetMatrixSize;
    int deltaX = (int) (width * sampler2);
    int deltaY = (int) (height * sampler2);
    int areaWidth = deltaX + (int) ((right - left) * sampler);
    int areaHeight = deltaY + (int) ((down - up) * sampler);

    ResultPoint a = new ResultPoint(left - deltaX, up - deltaY);
    ResultPoint b = new ResultPoint(right + deltaX, up - deltaY);
    ResultPoint c = new ResultPoint(right + deltaX, down + deltaY);
    ResultPoint d = new ResultPoint(left - deltaX, down + deltaY);

    ResultPoint ap = new ResultPoint(a.getX() + areaWidth, a.getY() + areaHeight);
    ResultPoint bp = new ResultPoint(b.getX() - areaWidth, b.getY() + areaHeight);
    ResultPoint cp = new ResultPoint(c.getX() - areaWidth, c.getY() - areaHeight);
    ResultPoint dp = new ResultPoint(d.getX() + areaWidth, d.getY() - areaHeight);

    ResultPoint topLeftCorner = getCornerFromArea((int) a.getX(), (int) ap.getX(), (int) a.getY(), (int) ap.getY(), false, false);
    ResultPoint topRightCorner = getCornerFromArea((int) bp.getX(), (int) b.getX(), (int) b.getY(), (int) bp.getY(), true, false);
    ResultPoint bottomRightCorner = getCornerFromArea((int) cp.getX(), (int) c.getX(), (int) cp.getY(), (int) c.getY(), true, true);
    ResultPoint bottomLeftCorner = getCornerFromArea((int) d.getX(), (int) dp.getX(), (int) dp.getY(), (int) d.getY(), false, true);

    float xCorrection = (topRightCorner.getX() - topLeftCorner.getX()) / targetMatrixSize;
    float yCorrection = (bottomRightCorner.getY() - topRightCorner.getY()) / targetMatrixSize;

    ResultPoint topLeftCornerCenter = new ResultPoint(topLeftCorner.getX() + xCorrection, topLeftCorner.getY() + yCorrection);
    ResultPoint topRightCornerCenter = new ResultPoint(topRightCorner.getX() - xCorrection, topRightCorner.getY() + yCorrection);
    ResultPoint bottomRightCornerCenter = new ResultPoint(bottomRightCorner.getX() - xCorrection, bottomRightCorner.getY() - yCorrection);
    ResultPoint bottomLeftCornerCenter = new ResultPoint(bottomLeftCorner.getX() + xCorrection, bottomLeftCorner.getY() - yCorrection);

    return new ResultPoint[]{topLeftCornerCenter, topRightCornerCenter, bottomRightCornerCenter, bottomLeftCornerCenter};
  }

  private ResultPoint getCornerFromArea(int left, int right, int top, int bottom, boolean maximizeX, boolean maximizeY) throws NotFoundException {
    int resX = maximizeX ? 0 : Integer.MAX_VALUE;
    int resY = maximizeY ? 0 : Integer.MAX_VALUE;
    for (int x = left; x < right; x++) {
      for (int y = top; y < bottom; y++) {
        if (x > 0 && y > 0 && x < image.getWidth() && y < image.getHeight()) {
          if (image.get(x, y)) {
            if (maximizeX) {
              if (x > resX) {
                resX = x;
              }
            } else {
              if (x < resX) {
                resX = x;
              }
            }
            if (maximizeY) {
              if (y > resY) {
                resY = y;
              }
            } else {
              if (y < resY) {
                resY = y;
              }
            }
          }
        }
      }
    }
    if (resX == 0 || resY == 0) {
      throw NotFoundException.getNotFoundInstance();
    } else {
      return new ResultPoint(resX, resY);
    }
  }


  /**
   * Determines whether a segment contains a black point
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return true if a black point has been found, else false.
   */
  private boolean containsBlackPoint(int a, int b, int fixed, boolean horizontal) {

    if (horizontal) {
      for (int x = a; x <= b; x++) {
        if (image.get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (int y = a; y <= b; y++) {
        if (image.get(fixed, y)) {
          return true;
        }
      }
    }

    return false;
  }

}
