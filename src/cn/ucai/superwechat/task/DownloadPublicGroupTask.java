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
            url = new ApiParams().with(I.User.USER_NAME,username)
                    .with(I.PAGE_ID,pageId+"")
                    .with(I.PAGE_SIZE,pageSize+"")
                    .getRequestUrl(I.REQUEST_FIND_PUBLIC_GROUPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(){
        executeRequest(new GsonRequest<Group[]>(url,Group[].class,
                responseDownloadPublicGroupTaskListener(),errorListener()));
    }

    private Response.Listener<Group[]> responseDownloadPublicGroupTaskListener() {
        return new Response.Listener<Group[]>(){
            @Override
            public void onResponse(Group[] response) {
                if (response != null && response.length>0){
                    Log.i("main","DownloadPublicGroupTask--->"+ response.length);

                    ArrayList<Group> list = Utils.array2List(response);
                    ArrayList<Group> groupsList =
                            SuperWeChatApplication.getInstance().getPublicGroupList();
                    for(Group g:list){
                        if (!groupsList.contains(g)){
                            groupsList.add(g);
                        }
                    }
                    mContext.sendStickyBroadcast(new Intent("update_public_group"));
                }
            }
        };
    }
}
