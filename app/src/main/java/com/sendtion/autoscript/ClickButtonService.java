package com.sendtion.autoscript;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 按钮点击事件
 */
public class ClickButtonService extends AccessibilityService {
    private final String TAG = getClass().getName();

    public static ClickButtonService mService;
    private int count;

    //初始化
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Utils.toast("可以开始点击事件模拟...");
        mService = this;
    }

    //实现辅助功能
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo nextPageInfo = findFirst(AbstractTF.newWebText("点击此区域加载下一页", true));
//        AccessibilityNodeInfo nextPageInfo = findFirst(AbstractTF.newText("显示星期", true));
        if (nextPageInfo != null) {
            Utils.toast("找到下一页按钮");//第一次运行可能会吐不出来
            Log.e(TAG, "onAccessibilityEvent: 找到下一页按钮");//可以查看日志
            try {
                Thread.sleep(5000);
                scrollView(nextPageInfo);
                count ++;
                Log.e("---", "count: " + count);
                if (count >= 25) {
                    clickView(nextPageInfo);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nextPageInfo.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        Utils.toast("(；′⌒`)\r\n红包功能被迫中断");
        mService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.toast("%>_<%\r\n红包功能已关闭");
        mService = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 公共方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 辅助功能是否启动
     */
    public static boolean isStart() {
        return mService != null;
    }

    /**
     * 点击该控件
     *
     * @return true表示点击成功
     */
    public static boolean clickView(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            } else {
                AccessibilityNodeInfo parent = nodeInfo.getParent();
                if (parent != null) {
                    boolean b = clickView(parent);
                    parent.recycle();
                    if (b) return true;
                }
            }
        }
        return false;
    }

    /**
     * 滑动屏幕
     */
    public static boolean scrollView(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
//            Log.e("---", "--------------------------------");
//            Log.e("---", "isScrollable: " + nodeInfo.isScrollable());
//            Log.e("---", "isFocusable: " + nodeInfo.isFocusable());
//            Log.e("---", "isFocused: " + nodeInfo.isFocused());
            if (nodeInfo.isScrollable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD); //向上滚动
                //nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); //向下滚动
                return true;
            } else {
                AccessibilityNodeInfo parent = nodeInfo.getParent();
                if (parent != null) {
                    boolean b = scrollView(parent);
                    parent.recycle();
                    if (b) return true;
                }
            }
        }
        return false;
    }

    /**
     * 查找第一个匹配的控件
     *
     * @param tfs 匹配条件，多个AbstractTF是&&的关系，如：
     *            AbstractTF.newContentDescription("表情", true),AbstractTF.newClassName(AbstractTF.ST_IMAGEVIEW)
     *            表示描述内容是'表情'并且是imageview的控件
     */
    @Nullable
    public AccessibilityNodeInfo findFirst(@NonNull AbstractTF... tfs) {
        if (tfs.length == 0) throw new InvalidParameterException("AbstractTF不允许传空");

        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return null;

        int idTextTFCount = 0, idTextIndex = 0;
        for (int i = 0; i < tfs.length; i++) {
            if (tfs[i] instanceof AbstractTF.IdTextTF) {
                idTextTFCount++;
                idTextIndex = i;
            }
        }
        switch (idTextTFCount) {
            case 0://id或text数量为0，直接循环查找
                AccessibilityNodeInfo returnInfo = findFirstRecursive(rootInfo, tfs);
                rootInfo.recycle();
                return returnInfo;
            case 1://id或text数量为1，先查出对应的id或text，然后再查其他条件
                if (tfs.length == 1) {
                    AccessibilityNodeInfo returnInfo2 = ((AbstractTF.IdTextTF) tfs[idTextIndex]).findFirst(rootInfo);
                    rootInfo.recycle();
                    return returnInfo2;
                } else {
                    List<AccessibilityNodeInfo> listIdText = ((AbstractTF.IdTextTF) tfs[idTextIndex]).findAll(rootInfo);
                    if (Utils.isEmptyArray(listIdText)) {
                        break;
                    }
                    AccessibilityNodeInfo returnInfo3 = null;
                    for (AccessibilityNodeInfo info : listIdText) {//遍历找到匹配的
                        if (returnInfo3 == null) {
                            boolean isOk = true;
                            for (AbstractTF tf : tfs) {
                                if (!tf.checkOk(info)) {
                                    isOk = false;
                                    break;
                                }
                            }
                            if (isOk) {
                                returnInfo3 = info;
                            } else {
                                info.recycle();
                            }
                        } else {
                            info.recycle();
                        }
                    }
                    rootInfo.recycle();
                    return returnInfo3;
                }
            default:
                throw new RuntimeException("由于时间有限，并且多了也没什么用，所以IdTF和TextTF只能有一个");
        }
        rootInfo.recycle();
        return null;
    }

    /**
     * @param tfs 由于是递归循环，会忽略IdTF和TextTF
     */
    public static AccessibilityNodeInfo findFirstRecursive(AccessibilityNodeInfo parent, @NonNull AbstractTF... tfs) {
        if (parent == null) return null;
        if (tfs.length == 0) throw new InvalidParameterException("AbstractTF不允许传空");

        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            boolean isOk = true;
            for (AbstractTF tf : tfs) {
                if (!tf.checkOk(child)) {
                    isOk = false;
                    break;
                }
            }
            if (isOk) {
                return child;
            } else {
                AccessibilityNodeInfo childChild = findFirstRecursive(child, tfs);
                child.recycle();
                if (childChild != null) {
                    return childChild;
                }
            }
        }
        return null;
    }

    /**
     * 查找全部匹配的控件
     *
     * @param tfs 匹配条件，多个AbstractTF是&&的关系，如：
     *            AbstractTF.newContentDescription("表情", true),AbstractTF.newClassName(AbstractTF.ST_IMAGEVIEW)
     *            表示描述内容是'表情'并且是imageview的控件
     */
    @NonNull
    public List<AccessibilityNodeInfo> findAll(@NonNull AbstractTF... tfs) {
        if (tfs.length == 0) throw new InvalidParameterException("AbstractTF不允许传空");

        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return list;

        int idTextTFCount = 0, idTextIndex = 0;
        for (int i = 0; i < tfs.length; i++) {
            if (tfs[i] instanceof AbstractTF.IdTextTF) {
                idTextTFCount++;
                idTextIndex = i;
            }
        }
        switch (idTextTFCount) {
            case 0://id或text数量为0，直接循环查找
                findAllRecursive(list, rootInfo, tfs);
                break;
            case 1://id或text数量为1，先查出对应的id或text，然后再循环
                List<AccessibilityNodeInfo> listIdText = ((AbstractTF.IdTextTF) tfs[idTextIndex]).findAll(rootInfo);
                if (Utils.isEmptyArray(listIdText)) {
                    break;
                }
                if (tfs.length == 1) {
                    list.addAll(listIdText);
                } else {
                    for (AccessibilityNodeInfo info : listIdText) {
                        boolean isOk = true;
                        for (AbstractTF tf : tfs) {
                            if (!tf.checkOk(info)) {
                                isOk = false;
                                break;
                            }
                        }
                        if (isOk) {
                            list.add(info);
                        } else {
                            info.recycle();
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("由于时间有限，并且多了也没什么用，所以IdTF和TextTF只能有一个");
        }
        rootInfo.recycle();
        return list;
    }

    /**
     * @param tfs 由于是递归循环，会忽略IdTF和TextTF
     */
    public static void findAllRecursive(List<AccessibilityNodeInfo> list, AccessibilityNodeInfo parent, @NonNull AbstractTF... tfs) {
        if (parent == null || list == null) return;
        if (tfs.length == 0) throw new InvalidParameterException("AbstractTF不允许传空");

        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            boolean isOk = true;
            for (AbstractTF tf : tfs) {
                if (!tf.checkOk(child)) {
                    isOk = false;
                    break;
                }
            }
            if (isOk) {
                list.add(child);
            } else {
                findAllRecursive(list, child, tfs);
                child.recycle();
            }
        }
    }

    /**
     * 立即发送移动的手势
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     *
     * @param path  移动路径
     * @param mills 持续总时间
     */
    @RequiresApi(24)
    public void dispatchGestureMove(Path path, long mills) {
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, mills)).build(), null, null);
    }

    /**
     * 点击指定位置
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     */
    @RequiresApi(24)
    public void dispatchGestureClick(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, 100)).build(), null, null);
    }

    /**
     * 由于太多,最好回收这些AccessibilityNodeInfo
     */
    public static void recycleAccessibilityNodeInfo(List<AccessibilityNodeInfo> listInfo) {
        if (Utils.isEmptyArray(listInfo)) return;

        for (AccessibilityNodeInfo info : listInfo) {
            info.recycle();
        }
    }
}