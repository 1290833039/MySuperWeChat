package cn.ucai.superwechat.task;

import android.content.Context;
import android.content.Intent;

import com.android.volley.Response;

import java.util.ArrayList;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.activity.BaseActivity;
import cn.ucai.superwechat.bean.Group;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.utils.Utils;

/**
 * Created by Administrator on 2016/5/23.
 * DownloadAllGroupTask     update_group_list
 */
public class DownloadAllGroupTask extends BaseActivity {
    private static final String TAG = DownloadAllGroupTask.class.getName();
    Context mContext;
    String username;
    String url;
    int pageId;
    int pageSize;

    public DownloadAllGroupTask(Context mContext, String username, int pageId, int pageSize) {
        this.mContext = mContext;
        this.username = username;
        this.pageId = pageId;
        this.pageSize = pageSize;
        initUrl();
    }

    private void initUrl() {
        try {
            url = new ApiParams().with(I.Contact.USER_NAME,username)
                    .getRequestUrl(I.REQUEST_DOWNLOAD_GROUPS);
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
            public void onResponse(Group[] response) {
                if (response != null){
                    ArrayList<Group> groupsList =
                            SuperWeChatApplication.getInstance().getGroupList();

                    /*ArrayList<Group> list = Utils.array2List(contacts);
                    groupsList.clear();
                    groupsList.addAll(list);
                    ArrayList<Group> groupList = SuperWeChatApplication.getInstance().getGroupList();
                    groupList.clear();
                    for(Group g:list){
                        groupList.put(g.getMGroupName(),g);
                    }*/
                    mContext.sendStickyBroadcast(new Intent("update_group_list"));
                }
            }
        };
    }
}
