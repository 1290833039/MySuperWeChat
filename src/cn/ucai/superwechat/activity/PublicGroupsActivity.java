/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ucai.superwechat.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.easemob.chat.EMCursorResult;
import com.easemob.chat.EMGroupInfo;
import com.easemob.chat.EMGroupManager;
import com.easemob.exceptions.EaseMobException;

import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.bean.Group;
import cn.ucai.superwechat.task.DownloadPublicGroupTask;
import cn.ucai.superwechat.utils.UserUtils;

public class PublicGroupsActivity extends BaseActivity {
	private ProgressBar pb;
	private ListView listView;
	private GroupsAdapter adapter;

	private ArrayList<Group> groupsList;
	private boolean isLoading;
	private boolean isFirstLoading = true;
	private boolean hasMoreData = true;
	private String cursor;
	private final int pagesize = 20;
    private int pageId=0;
    private LinearLayout footLoadingLayout;
    private ProgressBar footLoadingPB;
    private TextView footLoadingText;
    private Button searchBtn;
    private PublicGroupChangeReceiver mReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(cn.ucai.superwechat.R.layout.activity_public_groups);
        groupsList = new ArrayList<Group>();
        initView();
		setListener();
        //获取及显示数据
        loadAndShowData();
	}

    private void setListener() {
        setItemClickListener();
        setScrollListener();
        registerPublicGroupChangeReceiver();
        setQueryTextChangedListener();
    }

    private void setQueryTextChangedListener() {
        final EditText query = (EditText) findViewById(R.id.query);
        final ImageButton clearSearch = (ImageButton) findViewById(R.id.search_clear);
        query.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
                if (s.length() > 0) {
                    clearSearch.setVisibility(View.VISIBLE);
                } else {
                    clearSearch.setVisibility(View.INVISIBLE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        });
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                query.getText().clear();
            }
        });
    }

    private void setScrollListener() {
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == OnScrollListener.SCROLL_STATE_IDLE){
                    if(listView.getCount() != 0){
                        int lasPos = view.getLastVisiblePosition();
                        if(hasMoreData && !isLoading && lasPos == listView.getCount()-1){
                            //页数+1
                            pageId++;
                            new DownloadPublicGroupTask(PublicGroupsActivity.this,
                                    SuperWeChatApplication.getInstance().getUserName(),
                                    pageId,pagesize).execute();

                            loadAndShowData();
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
    }

    private void setItemClickListener() {
        //设置item点击事件
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(PublicGroupsActivity.this, GroupSimpleDetailActivity.class).
                        putExtra("groupinfo", adapter.getItem(position)));
            }
        });
    }

    private void initView() {
        pb = (ProgressBar) findViewById(cn.ucai.superwechat.R.id.progressBar);
        listView = (ListView) findViewById(cn.ucai.superwechat.R.id.list);
        searchBtn = (Button) findViewById(cn.ucai.superwechat.R.id.btn_search);

        View footView = getLayoutInflater().inflate(cn.ucai.superwechat.R.layout.listview_footer_view, null);
        footLoadingLayout = (LinearLayout) footView.findViewById(cn.ucai.superwechat.R.id.loading_layout);
        footLoadingPB = (ProgressBar)footView.findViewById(cn.ucai.superwechat.R.id.loading_bar);
        footLoadingText = (TextView) footView.findViewById(cn.ucai.superwechat.R.id.loading_text);
        listView.addFooterView(footView, null, false);
        footLoadingLayout.setVisibility(View.GONE);

    }
    /**
	 * 搜索
	 * @param view
	 */
	public void search(View view){
	    startActivity(new Intent(this, PublicGroupsSeachActivity.class));
	}

	private void loadAndShowData(){
	    new Thread(new Runnable() {
            public void run() {
                try {
                    isLoading = true;
                    ArrayList<Group> publicGroupList = SuperWeChatApplication.getInstance().getPublicGroupList();
                    for(Group group: publicGroupList){
                        if (!groupsList.contains(group)){
                            groupsList.add(group);
                        }
                    }


                   // final EMCursorResult<Group> result = EMGroupManager.getInstance().getPublicGroupsFromServer(pagesize, cursor);
                    //获取group list
                   // final List<Group> returnGroups = result.getData();
//                    runOnUiThread(new Runnable() {
//                        public void run() {
                            searchBtn.setVisibility(View.VISIBLE);
                      //      groupsList.addAll(returnGroups);
                            if(publicGroupList.size() != 0){
                                //获取cursor
//                                cursor = result.getCursor();
                                if(publicGroupList.size() == (pageId+1)*pagesize)
                                    footLoadingLayout.setVisibility(View.VISIBLE);
                            }
                            if(isFirstLoading){
                                pb.setVisibility(View.INVISIBLE);
                                isFirstLoading = false;
                                //设置adapter
                                adapter = new GroupsAdapter(PublicGroupsActivity.this, 1, groupsList);
                                listView.setAdapter(adapter);
                            }else{
                                if(groupsList.size() < (pageId+1)*pagesize){
                                    hasMoreData = false;
                                    footLoadingLayout.setVisibility(View.VISIBLE);
                                    footLoadingPB.setVisibility(View.GONE);
                                    footLoadingText.setText("No more data");
                                }
                                adapter.notifyDataSetChanged();
                            }
                            isLoading = false;
//                        }
//                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            isLoading = false;
                            pb.setVisibility(View.INVISIBLE);
                            footLoadingLayout.setVisibility(View.GONE);
                            Toast.makeText(PublicGroupsActivity.this, "加载数据失败，请检查网络或稍后重试", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
	}
	/**
	 * adapter
	 *
	 */
	private class GroupsAdapter extends BaseAdapter implements SectionIndexer {

		private LayoutInflater inflater;
        ArrayList<Group> mGroupList;
        Context mContext;

        private SparseIntArray positionOfSection;
        private SparseIntArray sectionOfPosition;
        private MyFilter myFilter;
        private boolean notiyfyByFilter;
        List<String> list;
        List<Group> copyGroupList;

		public GroupsAdapter(Context context, int res, ArrayList<Group> groups) {
			this.inflater = LayoutInflater.from(context);
            mGroupList = groups;
            copyGroupList = new ArrayList<Group>();
            copyGroupList.addAll(groups);
            this.mContext = context;
		}

        @Override
        public int getCount() {
            return mGroupList==null?0:mGroupList.size();
        }

        @Override
        public Group getItem(int position) {
            return mGroupList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(cn.ucai.superwechat.R.layout.row_group, null);
			}
            Group group = getItem(position);
			((TextView) convertView.findViewById(cn.ucai.superwechat.R.id.name)).setText(getItem(position).getMGroupName());
            UserUtils.setGroupBeanAvatar(group.getMGroupHxid(),
                    (NetworkImageView) convertView.findViewById(R.id.avatar));
			return convertView;
		}

        @Override
        public Object[] getSections() {
            positionOfSection = new SparseIntArray();
            sectionOfPosition = new SparseIntArray();
            int count = getCount();
            list = new ArrayList<String>();
            list.add(mContext.getString(R.string.search_header));
            positionOfSection.put(0, 0);
            sectionOfPosition.put(0, 0);
            for (int i = 1; i < count; i++) {

                String letter = getItem(i).getHeader();
                Log.i("main", "contactadapter getsection getHeader:" + letter + " name:" + getItem(i).getMGroupName());
                int section = list.size() - 1;
                if (list.get(section) != null && !list.get(section).equals(letter)) {
                    list.add(letter);
                    section++;
                    positionOfSection.put(section, i);
                }
                sectionOfPosition.put(i, section);
            }
            return list.toArray(new String[list.size()]);
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return positionOfSection.get(sectionIndex);
        }

        @Override
        public int getSectionForPosition(int position) {
            return sectionOfPosition.get(position);
        }


        public Filter getFilter() {
            if(myFilter==null){
                myFilter = new MyFilter(mGroupList);
            }
            return myFilter;
        }

        private class  MyFilter extends Filter{
            List<Group> mOriginalList = null;

            public MyFilter(List<Group> myList) {
                this.mOriginalList = myList;
            }

            @Override
            protected synchronized FilterResults performFiltering(CharSequence prefix) {
                FilterResults results = new FilterResults();
                if(mOriginalList==null){
                    mOriginalList = new ArrayList<Group>();
                }
                Log.i("main", "contacts original size: " + mOriginalList.size());
                Log.i("main", "contacts copy size: " + copyGroupList.size());

                if(prefix==null || prefix.length()==0){
                    results.values = copyGroupList;
                    results.count = copyGroupList.size();
                }else{
                    String prefixString = prefix.toString();
                    final int count = mOriginalList.size();
                    final ArrayList<Group> newValues = new ArrayList<Group>();
                    for(int i=0;i<count;i++){
                        final Group group = mOriginalList.get(i);
                        String username = group.getMGroupName();

                        if(username.contains(prefixString)){
                            newValues.add(group);
                        }
                        else{
                            final String[] words = username.split(" ");
                            final int wordCount = words.length;

                            // Start at index 0, in case valueText starts with space(s)
                            for (int k = 0; k < wordCount; k++) {
                                if (words[k].startsWith(prefixString)) {
                                    newValues.add(group);
                                    break;
                                }
                            }
                        }
                    }
                    results.values=newValues;
                    results.count=newValues.size();
                }
                Log.i("main", "contacts filter results size: " + results.count);
                return results;
            }

            @Override
            protected synchronized void publishResults(CharSequence constraint,
                                                       FilterResults results) {
                mGroupList.clear();
                mGroupList.addAll((List<Group>)results.values);
                Log.i("main", "publish contacts filter results size: " + results.count);
                if (results.count > 0) {
                    notiyfyByFilter = true;
                    notifyDataSetChanged();
                    notiyfyByFilter = false;
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }

        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            if(!notiyfyByFilter){
                copyGroupList.clear();
                copyGroupList.addAll(mGroupList);
            }
        }
	}
	
	public void back(View view){
		finish();
	}

    class PublicGroupChangeReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            loadAndShowData();
        }
    }

    public void registerPublicGroupChangeReceiver(){
        mReceiver = new PublicGroupChangeReceiver();
        IntentFilter filter = new IntentFilter("update_public_group");
        registerReceiver(mReceiver,filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver!=null){
            unregisterReceiver(mReceiver);
        }
    }
}
