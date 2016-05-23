package cn.ucai.superwechat.task;

import android.content.Context;
import android.content.Intent;

import com.android.volley.Response;

import java.util.ArrayList;
import java.util.HashMap;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.activity.BaseActivity;
import cn.ucai.superwechat.bean.Contact;
import cn.ucai.superwechat.bean.Group;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.utils.Utils;

/**
 * Created by Administrator on 2016/5/23.
 * DownloadPublicGroupTask     update_public_group
 * url http://10.0.2.2:8080/SuperWeChatServer/Server?request=download_public_groups&m_user_name=&page_id=&page_size=
 */
public class DownloadPublicGroupTask extends BaseActivity{
    private static final String TAG = DownloadPublicGroupTask.class.getName();
    Context mContext;
    String username;
    String url;
    int pageId;
    int pageSize;

    public DownloadPublicGroupTask(Context mContext, String username, int pageId, int pageSize) {
        this.mContext = mContext;
        this.username = username;
        this.pageId = pageId;
        this.pageSize = pageSize;
        initUrl();
    }

    private void initUrl() {
        try {
            url = new ApiParams().with(I.Contact.USER_NAME,username)
                    .with(I.PAGE_ID,pageId+"")
                    .with(I.PAGE_SIZE,pageSize+"")
                    .getRequestUrl(I.REQUEST_FIND_PUBLIC_GROUPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(){
        executeRequest(new GsonRequest<Group[]>(url,Group[].class,
                responseDownloadContactListTaskListener(),errorListener()));
    }

    private Response.Listener<Group[]> responseDownloadContactListTaskListener() {
        return new Response.Listener<Group[]>(){
            @Override
            public void onResponse(Group[] contacts) {
                if (contacts != null){
                    ArrayList<Group> groupsList =
                            SuperWeChatApplication.getInstance().getPublicGroupList();

                    ArrayList<Group> list = Utils.array2List(contacts);
                    groupsList.clear();
                    groupsList.addAll(list);

                    ArrayList<Group> groupList = SuperWeChatApplication.getInstance().getGroupList();
                    /*groupList.clear();
                    for(Group g:list){
                        groupList.put(g.getMGroupName(),g);
                    }*/
                    mContext.sendStickyBroadcast(new Intent("update_public_group"));
                }
            }
        };
    }
}
