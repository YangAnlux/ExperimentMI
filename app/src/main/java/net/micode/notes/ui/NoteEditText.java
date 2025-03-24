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
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

// 自定义的EditText类，用于笔记编辑
public class NoteEditText extends EditText {
    // 日志标签
    private static final String TAG = "NoteEditText";
    // 当前EditText在列表中的索引
    private int mIndex;
    // 删除操作前的选择起始位置
    private int mSelectionStartBeforeDelete;

    // 定义支持的链接协议
    private static final String SCHEME_TEL = "tel:" ;
    private static final String SCHEME_HTTP = "http:" ;
    private static final String SCHEME_EMAIL = "mailto:" ;

    // 存储链接协议和对应的菜单项文本资源ID的映射
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * 由{@link NoteEditActivity}调用，用于删除或添加EditText
     */
    public interface OnTextViewChangeListener {
        /**
         * 当按下{@link KeyEvent#KEYCODE_DEL}且文本为空时，删除当前EditText
         */
        void onEditTextDelete(int index, String text);

        /**
         * 当按下{@link KeyEvent#KEYCODE_ENTER}时，在当前EditText之后添加一个新的EditText
         */
        void onEditTextEnter(int index, String text);

        /**
         * 当文本发生变化时，隐藏或显示选项
         */
        void onTextChange(int index, boolean hasText);
    }

    // 文本变化监听器
    private OnTextViewChangeListener mOnTextViewChangeListener;

    // 构造函数，使用默认的属性集
    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0;
    }

    // 设置当前EditText在列表中的索引
    public void setIndex(int index) {
        mIndex = index;
    }

    // 设置文本变化监听器
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    // 构造函数，使用指定的属性集
    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }

    // 构造函数，使用指定的属性集和默认样式
    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    // 处理触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 获取触摸点的坐标
                int x = (int) event.getX();
                int y = (int) event.getY();
                // 调整坐标以适应布局
                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();
                x += getScrollX();
                y += getScrollY();

                // 获取文本布局
                Layout layout = getLayout();
                // 根据垂直坐标找到所在的行
                int line = layout.getLineForVertical(y);
                // 根据行和水平坐标找到偏移量
                int off = layout.getOffsetForHorizontal(line, x);
                // 设置文本选择位置
                Selection.setSelection(getText(), off);
                break;
        }

        return super.onTouchEvent(event);
    }

    // 处理按键按下事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                if (mOnTextViewChangeListener != null) {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                // 记录删除操作前的选择起始位置
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 处理按键抬起事件
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if (mOnTextViewChangeListener != null) {
                    // 如果选择起始位置为0且不是第一个EditText，则删除当前EditText
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                if (mOnTextViewChangeListener != null) {
                    // 获取选择起始位置
                    int selectionStart = getSelectionStart();
                    // 获取选择位置之后的文本
                    String text = getText().subSequence(selectionStart, length()).toString();
                    // 设置文本为选择位置之前的部分
                    setText(getText().subSequence(0, selectionStart));
                    // 在当前EditText之后添加新的EditText
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    // 处理焦点变化事件
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            if (!focused && TextUtils.isEmpty(getText())) {
                // 失去焦点且文本为空时，通知监听器文本变化
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                // 其他情况，通知监听器文本有内容
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    // 创建上下文菜单
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            // 获取选择的起始和结束位置
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            // 确保选择范围正确
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            // 获取选择范围内的URLSpan
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                int defaultResId = 0;
                // 根据链接协议查找对应的菜单项文本资源ID
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                if (defaultResId == 0) {
                    // 如果没有匹配的协议，使用默认的菜单项文本资源ID
                    defaultResId = R.string.note_link_other;
                }

                // 添加菜单项并设置点击监听器
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                // 处理链接点击事件
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
