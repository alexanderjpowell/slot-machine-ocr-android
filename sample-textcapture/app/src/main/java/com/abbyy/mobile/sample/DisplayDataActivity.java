package com.abbyy.mobile.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class DisplayDataActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_data);

        ArrayList<String> machineIdList = new ArrayList<String>();
        machineIdList.add("Machine ID: 2478");
        machineIdList.add("Machine ID: 6239");
        machineIdList.add("Machine ID: 7824");
        machineIdList.add("Machine ID: 2478");
        machineIdList.add("Machine ID: 6239");
        machineIdList.add("Machine ID: 7824");
        machineIdList.add("Machine ID: 2478");
        machineIdList.add("Machine ID: 6239");
        machineIdList.add("Machine ID: 7824");
        machineIdList.add("Machine ID: 2478");
        machineIdList.add("Machine ID: 6239");
        machineIdList.add("Machine ID: 7824");

        ArrayList<String> dateList = new ArrayList<String>();
        dateList.add("December 12, 2018 - 3:41pm");
        dateList.add("December 12, 2018 - 2:15pm");
        dateList.add("December 12, 2018 - 3:50pm");
        dateList.add("December 12, 2018 - 4:02pm");
        dateList.add("December 12, 2018 - 3:41pm");
        dateList.add("December 12, 2018 - 2:15pm");
        dateList.add("December 12, 2018 - 3:50pm");
        dateList.add("December 12, 2018 - 4:02pm");
        dateList.add("December 12, 2018 - 3:41pm");
        dateList.add("December 12, 2018 - 2:15pm");
        dateList.add("December 12, 2018 - 3:50pm");
        dateList.add("December 12, 2018 - 4:02pm");

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecyclerViewAdapter(this, machineIdList, dateList);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onItemClick(View view, int position) {
        Toast.makeText(this, "You clicked " + mAdapter.getMachineId(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}
