/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.tool;

import android.content.Context;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;

// ResourceParser 类用于解析和管理与笔记相关的资源，如背景颜色、字体大小等
public class ResourceParser {

    // 定义背景颜色的常量
    public static final int YELLOW = 0;
    public static final int BLUE = 1;
    public static final int WHITE = 2;
    public static final int GREEN = 3;
    public static final int RED = 4;

    // 默认背景颜色为黄色
    public static final int BG_DEFAULT_COLOR = YELLOW;

    // 定义字体大小的常量
    public static final int TEXT_SMALL = 0;
    public static final int TEXT_MEDIUM = 1;
    public static final int TEXT_LARGE = 2;
    public static final int TEXT_SUPER = 3;

    // 默认字体大小为中等
    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    // 内部类，用于管理笔记编辑界面的背景资源
    public static class NoteBgResources {
        // 笔记编辑界面的背景资源数组
        private final static int[] BG_EDIT_RESOURCES = new int[]{
                R.drawable.edit_yellow,
                R.drawable.edit_blue,
                R.drawable.edit_white,
                R.drawable.edit_green,
                R.drawable.edit_red
        };

        // 笔记编辑界面标题的背景资源数组
        private final static int[] BG_EDIT_TITLE_RESOURCES = new int[]{
                R.drawable.edit_title_yellow,
                R.drawable.edit_title_blue,
                R.drawable.edit_title_white,
                R.drawable.edit_title_green,
                R.drawable.edit_title_red
        };

        /**
         * 根据颜色 ID 获取笔记编辑界面的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id];
        }

        /**
         * 根据颜色 ID 获取笔记编辑界面标题的背景资源
         * @param id 颜色 ID
         * @return 对应的标题背景资源 ID
         */
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id];
        }
    }

    /**
     * 获取默认的背景颜色 ID
     * @param context 上下文对象
     * @return 默认背景颜色 ID
     */
    public static int getDefaultBgId(Context context) {
        // 检查是否开启了随机背景颜色的设置
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            // 若开启，则随机生成一个颜色 ID
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length);
        } else {
            // 若未开启，则返回默认颜色 ID
            return BG_DEFAULT_COLOR;
        }
    }

    // 内部类，用于管理笔记列表项的背景资源
    public static class NoteItemBgResources {
        // 笔记列表项第一个的背景资源数组
        private final static int[] BG_FIRST_RESOURCES = new int[]{
                R.drawable.list_yellow_up,
                R.drawable.list_blue_up,
                R.drawable.list_white_up,
                R.drawable.list_green_up,
                R.drawable.list_red_up
        };

        // 笔记列表项中间的背景资源数组
        private final static int[] BG_NORMAL_RESOURCES = new int[]{
                R.drawable.list_yellow_middle,
                R.drawable.list_blue_middle,
                R.drawable.list_white_middle,
                R.drawable.list_green_middle,
                R.drawable.list_red_middle
        };

        // 笔记列表项最后一个的背景资源数组
        private final static int[] BG_LAST_RESOURCES = new int[]{
                R.drawable.list_yellow_down,
                R.drawable.list_blue_down,
                R.drawable.list_white_down,
                R.drawable.list_green_down,
                R.drawable.list_red_down,
        };

        // 笔记列表项单个的背景资源数组
        private final static int[] BG_SINGLE_RESOURCES = new int[]{
                R.drawable.list_yellow_single,
                R.drawable.list_blue_single,
                R.drawable.list_white_single,
                R.drawable.list_green_single,
                R.drawable.list_red_single
        };

        /**
         * 根据颜色 ID 获取笔记列表项第一个的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id];
        }

        /**
         * 根据颜色 ID 获取笔记列表项最后一个的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id];
        }

        /**
         * 根据颜色 ID 获取笔记列表项单个的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id];
        }

        /**
         * 根据颜色 ID 获取笔记列表项中间的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id];
        }

        /**
         * 获取文件夹的背景资源
         * @return 文件夹的背景资源 ID
         */
        public static int getFolderBgRes() {
            return R.drawable.list_folder;
        }
    }

    // 内部类，用于管理小部件的背景资源
    public static class WidgetBgResources {
        // 2x 小部件的背景资源数组
        private final static int[] BG_2X_RESOURCES = new int[]{
                R.drawable.widget_2x_yellow,
                R.drawable.widget_2x_blue,
                R.drawable.widget_2x_white,
                R.drawable.widget_2x_green,
                R.drawable.widget_2x_red,
        };

        /**
         * 根据颜色 ID 获取 2x 小部件的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        // 4x 小部件的背景资源数组
        private final static int[] BG_4X_RESOURCES = new int[]{
                R.drawable.widget_4x_yellow,
                R.drawable.widget_4x_blue,
                R.drawable.widget_4x_white,
                R.drawable.widget_4x_green,
                R.drawable.widget_4x_red
        };

        /**
         * 根据颜色 ID 获取 4x 小部件的背景资源
         * @param id 颜色 ID
         * @return 对应的背景资源 ID
         */
        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    // 内部类，用于管理文本外观资源
    public static class TextAppearanceResources {
        // 文本外观资源数组
        private final static int[] TEXTAPPEARANCE_RESOURCES = new int[]{
                R.style.TextAppearanceNormal,
                R.style.TextAppearanceMedium,
                R.style.TextAppearanceLarge,
                R.style.TextAppearanceSuper
        };

        /**
         * 根据 ID 获取文本外观资源
         * @param id 文本外观 ID
         * @return 对应的文本外观资源 ID
         */
        public static int getTexAppearanceResource(int id) {
            /**
             * HACKME: 修复在共享偏好中存储资源 ID 可能出现的问题。
             * 如果 ID 大于资源数组的长度，返回默认字体大小对应的资源 ID
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        /**
         * 获取文本外观资源数组的长度
         * @return 资源数组的长度
         */
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}
