package com.abbyy.mobile.sample;

//import android.recyclerview.widget.RecyclerView;
import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<String> machineIdData;
    private List<String> scanDateData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerViewAdapter(Context context, List<String> machineIdData, List<String> scanDateData) {
        this.mInflater = LayoutInflater.from(context);
        this.machineIdData = machineIdData;
        this.scanDateData = scanDateData;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String machineId = machineIdData.get(position);
        String scanDate = scanDateData.get(position);
        holder.subjectTextView.setText(machineId);
        holder.scanDateTextView.setText(scanDate);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return machineIdData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView subjectTextView;
        TextView scanDateTextView;

        ViewHolder(View itemView) {
            super(itemView);
            subjectTextView = itemView.findViewById(R.id.subjectText);
            itemView.setOnClickListener(this);
            scanDateTextView = itemView.findViewById(R.id.dateText);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getMachineId(int id) {
        return machineIdData.get(id);
    }

    String getScanDate(int id) {
        return scanDateData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}