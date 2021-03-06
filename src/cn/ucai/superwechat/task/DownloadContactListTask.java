package cn.ucai.superwechat.task;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Response;

import java.util.ArrayList;
import java.util.HashMap;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.activity.BaseActivity;
import cn.ucai.superwechat.bean.Contact;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.utils.Utils;

/**
 * Created by Administrator on 2016/5/23.
 * DownloadContactListTask     update_contact_list
 * url  http://10.0.2.2:8080/SuperWeChatServer/Server?request=download_contact_all_list&m_contact_user_name=
 * DownloadAllGroupTask,I.REQUEST_DOWNLOAD_GROUPS,update_group_list
 */
public class DownloadContactListTask extends BaseActivity{
    private static final String TAG = DownloadContactListTask.class.getName();
    Context mContext;
    String username;
    String path;

    public DownloadContactListTask(Context mContext, String username) {
        this.mContext = mContext;
        this.username = username;
        initPath();
    }
    //11.下载好友列表，全部数据
    private void initPath() {
        try {
            path = new ApiParams().with(I.Contact.USER_NAME,username)
                    .getRequestUrl(I.REQUEST_DOWNLOAD_CONTACT_ALL_LIST);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void execute(){
        executeRequest(new GsonRequest<Contact[]>(path,Contact[].class,
                responseDownloadContactListTaskListener(),errorListener()));
    }

    private Response.Listener<Contact[]> responseDownloadContactListTaskListener() {
        return new Response.Listener<Contact[]>(){
            @Override
            public void onResponse(Contact[] contacts) {
                if (contacts != null){
                    Log.i("main","DownloadContactListTask"+ contacts.length);

                    ArrayList<Contact> contactList =
                            SuperWeChatApplication.getInstance().getContactList();
                    //得到返回的结果
                    ArrayList<Contact> list = Utils.array2List(contacts);
                    contactList.clear();
                    contactList.addAll(list);
                    HashMap<String,Contact> userList = SuperWeChatApplication.getInstance().getUserList();
                    userList.clear();
                    for(Contact c:list){
                        userList.put(c.getMContactCname(),c);
                    }

                    mContext.sendStickyBroadcast(new Intent("update_contact_list"));
                }
            }
        };
    }
}
