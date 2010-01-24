/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ser1.timetracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static net.ser1.timetracker.DBHelper.TAGS_TABLE;
import static net.ser1.timetracker.DBHelper.TAG;
import static net.ser1.timetracker.DBHelper.TASK_ID;

/**
 * This isn't thread-safe.
 * @author ser
 */
public class TagHandler {

    public static final String TAG_EXISTS = "SELECT COUNT(*) FROM " + TAGS_TABLE + " WHERE " + TAG + " = ?";
    private final Tasks tasks;
    private final SQLiteDatabase db;
    private ContentValues values = new ContentValues();

    protected TagHandler(Tasks tasks, SQLiteDatabase db) {
        this.tasks = tasks;
        this.db = db;
    }
    
    
    private static final String TAG_EQ = TAG + " = ?";

    /**
     * Removes a tag, and all task associations.  No-op if no such tag exists.
     * @param name the name of the tag to remove
     */
    protected void deleteTag(String name) {
        db.delete(TAGS_TABLE, TAG_EQ, new String[]{name.toLowerCase()});
    }


    /**
     * Alters a tag's name, leaving all associations. Fails with no effect if
     * the target tag name already exists.
     * @param oldName
     * @param newName
     */
    protected boolean modifyTag(String oldName, String newName) {
        if (db.rawQuery(TAG_EXISTS, new String[]{oldName.toLowerCase()}).getCount() > 0) {
            return false;
        } else {
            mergeTags(oldName, newName);
            return true;
        }
    }


    /**
     * Merges all tasks associated with oldname into newname, and removes
     * oldname. If newTag does not exist, it is created.  This doesn't fail.
     * @param oldTag the name of the tag to move tasks from
     * @param newTag the name of the tag to move tasks to
     */
    protected void mergeTags(String oldTag, String newTag) {
        values.clear();
        values.put(TAG, newTag.toLowerCase());
        db.update(TAGS_TABLE, values, TAG_EQ, new String[]{oldTag.toLowerCase()});
    }

    
    private final String[] COLUMNS = { TASK_ID };
    /**
     * Returns a list of all task IDs associated with a tag.
     * @param tagName
     * @return
     */
    protected int[] getTasks(String tagName) {
        Cursor cursor = db.query(TAGS_TABLE, COLUMNS, TAG_EQ,
                new String[]{tagName.toLowerCase()}, null, null, null );
        int[] rv = new int[cursor.getCount()];
        int idx = 0;
        if (cursor.moveToFirst()) {
            do {
                rv[idx++] = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        return rv;
    }

    
    /**
     * Returns a list of all tasks associated with a tag.
     * @param tagName
     * @return all tasks associated with the supplied tag, or an empty list
     * if none are.
     */
    protected List<Task> getTasks(Tag tag) {
        int[] ids = getTasks(tag.getName());
        List<Task> rv = new ArrayList<Task>();
        for (int id : ids) {
            for (Task task : tasks.getTasks()) {
                if (task.getId() == id)
                    rv.add(task);
            }
        }
        return rv;
    }

    protected String[] getTagNames() {
        Cursor c = db.query(true,TAGS_TABLE,new String[]{TAG},
                null,null,null,null,null,null);
        String[] rv = new String[ c.getCount() ];
        int ctr = 0;
        if (c.moveToFirst()) {
            do {
                rv[ctr++] = c.getString(0);
            } while (c.moveToNext());
        }
        return rv;
    }

    protected Set<Tag> getTags() {
        Cursor c = db.query(TAGS_TABLE, new String[] { TAG, TASK_ID },
                null,null,null,null,TAG);
        
        // Optimization.  Throw task IDs into a buffer, and copy into the tag
        // when done.
        String currentTag = null;
        int[] buffer = new int[c.getCount()];
        int bufferLength = 0;

        HashMap<String,Tag> rv = new HashMap<String,Tag>( c.getCount() );
        if (c.moveToFirst()) {
            do {
                String tn = c.getString(0);
                Tag t = rv.get(tn);
                if (rv == null) {
                    t = new Tag(tn);
                    rv.put(tn, t);
                }
                if (tn.equals(currentTag)) {
                    buffer[bufferLength++] = c.getInt(1);
                } else if (currentTag != null) {
                    int[] taskIds = new int[bufferLength];
                    System.arraycopy(buffer, 0, taskIds, 0, bufferLength);
                    t.setTasks(taskIds);
                    bufferLength = 0;
                    currentTag = tn;
                } else {
                    currentTag = tn;
                }
            } while (c.moveToNext());
        }
        Set<Tag> retVal = new HashSet<Tag>(rv.size());
        retVal.addAll(rv.values());
        return retVal;
    }

    protected void tag(Task task, Tag tag) {
        if (db.query(TAGS_TABLE, new String[]{TAG}, "? = ? and ? = ?",
                new String[]{TAG,tag.getName(),TASK_ID,String.valueOf(task.getId())},
                null,null,null).getCount() > 0) {
            return;
        } else {
            values.clear();
            values.put(TAG, Integer.SIZE);
            tag.addTask(task);

            db.insert(TAGS_TABLE, null, values);
        }
    }

    protected void untag(Task task, Tag tag) {
        if (db.query(TAGS_TABLE, new String[]{TAG}, "? = ? and ? = ?",
                new String[]{TAG,tag.getName(),TASK_ID,String.valueOf(task.getId())},
                null,null,null).getCount() == 0) {
            return;
        } else {
            db.delete(TAGS_TABLE, "? = ? and ? = ?",
                    new String[]{TAG,tag.getName(),TASK_ID,String.valueOf(task.getId())});
        }
    }
}
