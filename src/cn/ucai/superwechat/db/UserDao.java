package cn.ucai.superwechat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.bean.User;

/**
 * Created by Administrator on 2016/5/20.
 */
public class UserDao extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "user";

    public UserDao(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, "user.db", factory, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "DROP TABLE IF EXISTS " + I.User.TABLE_NAME+" "+
                "CREATE TABLE " + I.User.TABLE_NAME +
                I.User.USER_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                I.User.USER_NAME + "  TEXT NOT NULL," +
                I.User.PASSWORD + "  TEXT NOT NULL," +
                I.User.NICK + "  TEXT NOT NULL," +
                I.User.UN_READ_MSG_COUNT + "  INTEGER DEFAULT 0" + ");";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    /** 添加一个用户 */
    public boolean addUser(User user){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(I.User.USER_ID,user.getMUserId());
        values.put(I.User.USER_NAME,user.getMUserName());
        values.put(I.User.PASSWORD,user.getMUserPassword());
        values.put(I.User.NICK,user.getMUserNick());
        values.put(I.User.UN_READ_MSG_COUNT,user.getMUserUnreadMsgCount());

        long insert = db.insert(I.User.TABLE_NAME, null, values);
        return insert>0;
    }
    /** 根据用户账号更改一个用户 */
    public boolean updateUser(User user){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(I.User.USER_ID,user.getMUserId());
        values.put(I.User.PASSWORD,user.getMUserPassword());
        values.put(I.User.NICK,user.getMUserNick());
        values.put(I.User.UN_READ_MSG_COUNT,user.getMUserUnreadMsgCount());

        long update = db.update(I.User.TABLE_NAME, values,"where "+I.User.USER_NAME+"=?",new String[]{user.getMUserName()});
        return update>0;
    }

    /** 根据用户账号查找用户 */
    public User findUserByName(String username){
        SQLiteDatabase db = getReadableDatabase();
        User user = null;

        String sql = "select * from " + I.User.TABLE_NAME + "where "+I.User.USER_NAME+"=?";
        Cursor cursor = db.rawQuery(sql, new String[]{username});
        while (cursor.moveToNext()){
            user = new User();
            user.setMUserId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(I.User.USER_ID))));
            user.setMUserPassword(cursor.getString(cursor.getColumnIndex(I.User.PASSWORD)));
            user.setMUserNick(cursor.getString(cursor.getColumnIndex(I.User.NICK)));
            user.setMUserName(cursor.getString(cursor.getColumnIndex(I.User.USER_NAME)));
            user.setMUserUnreadMsgCount(Integer.parseInt(cursor.getString(cursor.getColumnIndex(I.User.UN_READ_MSG_COUNT))));
        }

        return user;
    }

}
