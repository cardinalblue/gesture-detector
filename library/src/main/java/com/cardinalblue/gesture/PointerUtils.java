//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
//          yolung.lu@cardinalblue.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.cardinalblue.gesture;

import android.graphics.PointF;

public class PointerUtils {

    public static final int DELTA_X = 0;
    public static final int DELTA_Y = 1;
    public static final int DELTA_SCALE_X = 2;
    public static final int DELTA_SCALE_Y = 3;
    public static final int DELTA_RADIANS = 4;

    /**
     * Get an array of [tx, ty, sx, sy, rotation] sequence representing
     * the transformation from the given event.
     */
    public static float[] getTransformFromPointers(PointF[] startPointers,
                                                   PointF[] stopPointers) {
        if (startPointers.length < 2 || stopPointers.length < 2) {
            throw new IllegalStateException(
                "The event must have at least two down pointers.");
        }

        float[] transform = new float[]{0f, 0f, 1f, 1f, 0f};

        // Start pointer 1.
        final float startX1 = startPointers[0].x;
        final float startY1 = startPointers[0].y;
        // Start pointer 2.
        final float startX2 = startPointers[1].x;
        final float startY2 = startPointers[1].y;

        // Stop pointer 1.
        final float stopX1 = stopPointers[0].x;
        final float stopY1 = stopPointers[0].y;
        // Stop pointer 2.
        final float stopX2 = stopPointers[1].x;
        final float stopY2 = stopPointers[1].y;

        // Start vector.
        final float startVecX = startX2 - startX1;
        final float startVecY = startY2 - startY1;
        // Stop vector.
        final float stopVecX = stopX2 - stopX1;
        final float stopVecY = stopY2 - stopY1;

        // Start pivot.
        final float startPivotX = (startX1 + startX2) / 2f;
        final float startPivotY = (startY1 + startY2) / 2f;
        // Stop pivot.
        final float stopPivotX = (stopX1 + stopX2) / 2f;
        final float stopPivotY = (stopY1 + stopY2) / 2f;

        // Calculate the translation.
        transform[DELTA_X] = stopPivotX - startPivotX;
        transform[DELTA_Y] = stopPivotY - startPivotY;
        // Calculate the rotation degree.
        transform[DELTA_RADIANS] = (float) (
            Math.atan2(stopVecY, stopVecX) -
            Math.atan2(startVecY, startVecX));
        // Calculate the scale change.
        final float dScale = (float) (Math.hypot(stopVecX,
                                                 stopVecY) /
                                      Math.hypot(startVecX,
                                                 startVecY));
        transform[DELTA_SCALE_X] = dScale;
        transform[DELTA_SCALE_Y] = dScale;
//        Log.d("xyz", String.format(Locale.ENGLISH,
//                                   "getTransform: " +
//                                   "dx=%.3f, dy=%.3f, " +
//                                   "dScale=%.3f, " +
//                                   "dRadians=%.3f",
//                                   transform[DELTA_X], transform[DELTA_Y],
//                                   transform[DELTA_SCALE_X],
//                                   transform[DELTA_RADIANS]));

        return transform;
    }
}