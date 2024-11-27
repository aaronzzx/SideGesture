package com.aaron.sidegesture.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;

import androidx.annotation.RequiresApi;

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/27
 */
public class AccessibilityUtils {

    //实现对（x，y）坐标进行点击操作。
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean click(AccessibilityService service, int x, int y) {
        if (service == null) {
            return false;
        }
        Point point = new Point(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(point.x, point.y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0L, 200L));
        GestureDescription gesture = builder.build();

        return service.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        }, null);
    }
}
