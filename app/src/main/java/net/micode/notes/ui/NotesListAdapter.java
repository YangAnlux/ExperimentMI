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

package net.micode.notes.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

// 该类继承自 CursorAdapter，用于将数据库中的笔记数据绑定到列表视图上
public class NotesListAdapter extends CursorAdapter {
    // 日志标签，用于调试和记录日志
    private static final String TAG = "NotesListAdapter";
    // 上下文对象，用于获取资源和执行操作
    private Context mContext;
    // 存储每个列表项的选中状态，键为列表项的位置，值为是否选中的布尔值
    private HashMap<Integer, Boolean> mSelectedIndex;
    // 笔记的数量
    private int mNotesCount;
    // 是否处于多选模式的标志
    private boolean mChoiceMode;

    // 内部类，用于存储应用小部件的属性，包括小部件 ID 和类型
    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    }

    // 构造函数，初始化适配器
    public NotesListAdapter(Context context) {
        // 调用父类的构造函数，传入上下文和初始游标（这里初始游标为 null）
        super(context, null);
        // 初始化选中状态的哈希表
        mSelectedIndex = new HashMap<Integer, Boolean>();
        // 保存上下文对象
        mContext = context;
        // 初始化笔记数量为 0
        mNotesCount = 0;
    }

    // 创建新的列表项视图时调用的方法
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // 创建一个新的 NotesListItem 视图对象
        return new NotesListItem(context);
    }

    // 将数据绑定到列表项视图上时调用的方法
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof NotesListItem) {
            // 根据游标创建一个 NoteItemData 对象，用于存储笔记的相关数据
            NoteItemData itemData = new NoteItemData(context, cursor);
            // 调用 NotesListItem 的 bind 方法，将数据绑定到视图上，并传入多选模式和当前项的选中状态
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    // 设置指定位置的列表项的选中状态
    public void setCheckedItem(final int position, final boolean checked) {
        // 将指定位置的选中状态存入哈希表
        mSelectedIndex.put(position, checked);
        // 通知适配器数据发生变化，刷新列表视图
        notifyDataSetChanged();
    }

    // 判断适配器是否处于多选模式
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    // 设置适配器的多选模式
    public void setChoiceMode(boolean mode) {
        // 清空选中状态的哈希表
        mSelectedIndex.clear();
        // 设置多选模式标志
        mChoiceMode = mode;
    }

    // 全选或全不选所有笔记项
    public void selectAll(boolean checked) {
        // 获取适配器当前使用的游标
        Cursor cursor = getCursor();
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                // 判断当前项是否为笔记类型
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    // 设置当前项的选中状态
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    // 获取所有选中的笔记项的 ID
    public HashSet<Long> getSelectedItemIds() {
        // 用于存储选中笔记项的 ID
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                // 获取指定位置的笔记项的 ID
                Long id = getItemId(position);
                if (id == Notes.ID_ROOT_FOLDER) {
                    // 记录错误日志，根文件夹 ID 不应该被选中
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    // 将选中的笔记项 ID 存入集合
                    itemSet.add(id);
                }
            }
        }
        return itemSet;
    }

    // 获取所有选中的笔记项对应的应用小部件属性
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        // 用于存储选中笔记项对应的应用小部件属性
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                // 获取指定位置的游标
                Cursor c = (Cursor) getItem(position);
                if (c != null) {
                    // 创建一个新的应用小部件属性对象
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    // 根据游标创建一个 NoteItemData 对象
                    NoteItemData item = new NoteItemData(mContext, c);
                    // 设置应用小部件的 ID
                    widget.widgetId = item.getWidgetId();
                    // 设置应用小部件的类型
                    widget.widgetType = item.getWidgetType();
                    // 将应用小部件属性对象存入集合
                    itemSet.add(widget);
                    // 注意：这里不要关闭游标，只有适配器可以关闭它
                } else {
                    // 记录错误日志，游标无效
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    // 获取选中的笔记项的数量
    public int getSelectedCount() {
        // 获取选中状态哈希表中的所有值
        Collection<Boolean> values = mSelectedIndex.values();
        if (null == values) {
            return 0;
        }
        // 获取值的迭代器
        Iterator<Boolean> iter = values.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (true == iter.next()) {
                // 统计选中的笔记项数量
                count++;
            }
        }
        return count;
    }

    // 判断是否所有笔记项都被选中
    public boolean isAllSelected() {
        // 获取选中的笔记项数量
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    // 判断指定位置的笔记项是否被选中
    public boolean isSelectedItem(final int position) {
        if (null == mSelectedIndex.get(position)) {
            return false;
        }
        return mSelectedIndex.get(position);
    }

    // 当内容发生变化时调用的方法
    @Override
    protected void onContentChanged() {
        // 调用父类的方法
        super.onContentChanged();
        // 重新计算笔记的数量
        calcNotesCount();
    }

    // 更换游标时调用的方法
    @Override
    public void changeCursor(Cursor cursor) {
        // 调用父类的方法
        super.changeCursor(cursor);
        // 重新计算笔记的数量
        calcNotesCount();
    }

    // 计算笔记的数量
    private void calcNotesCount() {
        // 初始化笔记数量为 0
        mNotesCount = 0;
        for (int i = 0; i < getCount(); i++) {
            // 获取指定位置的游标
            Cursor c = (Cursor) getItem(i);
            if (c != null) {
                // 判断当前项是否为笔记类型
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    // 笔记数量加 1
                    mNotesCount++;
                }
            } else {
                // 记录错误日志，游标无效
                Log.e(TAG, "Invalid cursor");
                return;
            }
        }
    }
}
