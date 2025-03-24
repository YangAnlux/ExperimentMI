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

// GTaskStringUtils 类用于定义与 GTask 相关的字符串常量，这些常量可能用于 JSON 数据的解析和构建等操作
public class GTaskStringUtils {

    // JSON 数据中表示操作 ID 的键
    public final static String GTASK_JSON_ACTION_ID = "action_id";
    // JSON 数据中表示操作列表的键
    public final static String GTASK_JSON_ACTION_LIST = "action_list";
    // JSON 数据中表示操作类型的键
    public final static String GTASK_JSON_ACTION_TYPE = "action_type";
    // JSON 数据中操作类型为创建的字符串值
    public final static String GTASK_JSON_ACTION_TYPE_CREATE = "create";
    // JSON 数据中操作类型为获取所有数据的字符串值
    public final static String GTASK_JSON_ACTION_TYPE_GETALL = "get_all";
    // JSON 数据中操作类型为移动的字符串值
    public final static String GTASK_JSON_ACTION_TYPE_MOVE = "move";
    // JSON 数据中操作类型为更新的字符串值
    public final static String GTASK_JSON_ACTION_TYPE_UPDATE = "update";

    // JSON 数据中表示创建者 ID 的键
    public final static String GTASK_JSON_CREATOR_ID = "creator_id";
    // JSON 数据中表示子实体的键
    public final static String GTASK_JSON_CHILD_ENTITY = "child_entity";
    // JSON 数据中表示客户端版本的键
    public final static String GTASK_JSON_CLIENT_VERSION = "client_version";
    // JSON 数据中表示已完成状态的键
    public final static String GTASK_JSON_COMPLETED = "completed";
    // JSON 数据中表示当前列表 ID 的键
    public final static String GTASK_JSON_CURRENT_LIST_ID = "current_list_id";
    // JSON 数据中表示默认列表 ID 的键
    public final static String GTASK_JSON_DEFAULT_LIST_ID = "default_list_id";
    // JSON 数据中表示已删除状态的键
    public final static String GTASK_JSON_DELETED = "deleted";
    // JSON 数据中表示目标列表的键
    public final static String GTASK_JSON_DEST_LIST = "dest_list";
    // JSON 数据中表示目标父级的键
    public final static String GTASK_JSON_DEST_PARENT = "dest_parent";
    // JSON 数据中表示目标父级类型的键
    public final static String GTASK_JSON_DEST_PARENT_TYPE = "dest_parent_type";
    // JSON 数据中表示实体增量的键
    public final static String GTASK_JSON_ENTITY_DELTA = "entity_delta";
    // JSON 数据中表示实体类型的键
    public final static String GTASK_JSON_ENTITY_TYPE = "entity_type";
    // JSON 数据中表示获取已删除数据的键
    public final static String GTASK_JSON_GET_DELETED = "get_deleted";
    // JSON 数据中表示 ID 的键
    public final static String GTASK_JSON_ID = "id";
    // JSON 数据中表示索引的键
    public final static String GTASK_JSON_INDEX = "index";
    // JSON 数据中表示最后修改时间的键
    public final static String GTASK_JSON_LAST_MODIFIED = "last_modified";
    // JSON 数据中表示最新同步点的键
    public final static String GTASK_JSON_LATEST_SYNC_POINT = "latest_sync_point";
    // JSON 数据中表示列表 ID 的键
    public final static String GTASK_JSON_LIST_ID = "list_id";
    // JSON 数据中表示列表集合的键
    public final static String GTASK_JSON_LISTS = "lists";
    // JSON 数据中表示名称的键
    public final static String GTASK_JSON_NAME = "name";
    // JSON 数据中表示新 ID 的键
    public final static String GTASK_JSON_NEW_ID = "new_id";
    // JSON 数据中表示笔记的键
    public final static String GTASK_JSON_NOTES = "notes";
    // JSON 数据中表示父级 ID 的键
    public final static String GTASK_JSON_PARENT_ID = "parent_id";
    // JSON 数据中表示前一个兄弟 ID 的键
    public final static String GTASK_JSON_PRIOR_SIBLING_ID = "prior_sibling_id";
    // JSON 数据中表示结果的键
    public final static String GTASK_JSON_RESULTS = "results";
    // JSON 数据中表示源列表的键
    public final static String GTASK_JSON_SOURCE_LIST = "source_list";
    // JSON 数据中表示任务集合的键
    public final static String GTASK_JSON_TASKS = "tasks";
    // JSON 数据中表示类型的键
    public final static String GTASK_JSON_TYPE = "type";
    // JSON 数据中类型为组的字符串值
    public final static String GTASK_JSON_TYPE_GROUP = "GROUP";
    // JSON 数据中类型为任务的字符串值
    public final static String GTASK_JSON_TYPE_TASK = "TASK";
    // JSON 数据中表示用户的键
    public final static String GTASK_JSON_USER = "user";

    // MIUI 文件夹的前缀字符串
    public final static String MIUI_FOLDER_PREFFIX = "[MIUI_Notes]";
    // 默认文件夹名称
    public final static String FOLDER_DEFAULT = "Default";
    // 通话笔记文件夹名称
    public final static String FOLDER_CALL_NOTE = "Call_Note";
    // 元数据文件夹名称
    public final static String FOLDER_META = "METADATA";
    // 元数据头部中 GTask ID 的键
    public final static String META_HEAD_GTASK_ID = "meta_gid";
    // 元数据头部中笔记的键
    public final static String META_HEAD_NOTE = "meta_note";
    // 元数据头部中数据的键
    public final static String META_HEAD_DATA = "meta_data";
    // 元数据笔记的名称，提示不要更新和删除
    public final static String META_NOTE_NAME = "[META INFO] DON'T UPDATE AND DELETE";
}
