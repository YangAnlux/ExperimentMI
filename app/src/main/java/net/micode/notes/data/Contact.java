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

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

// Contact类用于从联系人数据库中根据电话号码获取联系人姓名
public class Contact {
    // 用于缓存联系人电话号码和姓名的映射关系，避免重复查询
    private static HashMap<String, String> sContactCache;
    // 日志标签，用于记录日志信息
    private static final String TAG = "Contact";

    // 用于查询联系人的选择条件，通过电话号码匹配联系人
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    // 根据给定的上下文和电话号码，从联系人数据库中获取联系人姓名的方法
    public static String getContact(Context context, String phoneNumber) {
        // 如果缓存为空，则初始化缓存
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        // 检查缓存中是否已经存在该电话号码对应的联系人姓名
        if (sContactCache.containsKey(phoneNumber)) {
            // 如果存在，直接从缓存中返回联系人姓名
            return sContactCache.get(phoneNumber);
        }

        // 根据电话号码生成具体的查询选择条件
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        // 使用生成的选择条件进行数据库查询，获取联系人的显示名称
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[]{Phone.DISPLAY_NAME},
                selection,
                new String[]{phoneNumber},
                null);

        // 如果查询结果不为空且游标可以移动到第一条记录
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 从游标中获取联系人的显示名称
                String name = cursor.getString(0);
                // 将电话号码和联系人姓名存入缓存
                sContactCache.put(phoneNumber, name);
                // 返回联系人姓名
                return name;
            } catch (IndexOutOfBoundsException e) {
                // 如果在获取数据时出现索引越界异常，记录错误日志
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // 无论是否出现异常，都要关闭游标，释放资源
                cursor.close();
            }
        } else {
            // 如果没有匹配的联系人记录，记录日志信息
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}/*
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

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

// Contact类用于从联系人数据库中根据电话号码获取联系人姓名
public class Contact {
    // 用于缓存联系人电话号码和姓名的映射关系，避免重复查询
    private static HashMap<String, String> sContactCache;
    // 日志标签，用于记录日志信息
    private static final String TAG = "Contact";

    // 用于查询联系人的选择条件，通过电话号码匹配联系人
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    // 根据给定的上下文和电话号码，从联系人数据库中获取联系人姓名的方法
    public static String getContact(Context context, String phoneNumber) {
        // 如果缓存为空，则初始化缓存
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        // 检查缓存中是否已经存在该电话号码对应的联系人姓名
        if (sContactCache.containsKey(phoneNumber)) {
            // 如果存在，直接从缓存中返回联系人姓名
            return sContactCache.get(phoneNumber);
        }

        // 根据电话号码生成具体的查询选择条件
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        // 使用生成的选择条件进行数据库查询，获取联系人的显示名称
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[]{Phone.DISPLAY_NAME},
                selection,
                new String[]{phoneNumber},
                null);

        // 如果查询结果不为空且游标可以移动到第一条记录
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 从游标中获取联系人的显示名称
                String name = cursor.getString(0);
                // 将电话号码和联系人姓名存入缓存
                sContactCache.put(phoneNumber, name);
                // 返回联系人姓名
                return name;
            } catch (IndexOutOfBoundsException e) {
                // 如果在获取数据时出现索引越界异常，记录错误日志
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // 无论是否出现异常，都要关闭游标，释放资源
                cursor.close();
            }
        } else {
            // 如果没有匹配的联系人记录，记录日志信息
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}
