package com.wiser.refreshlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MAdapter extends RecyclerView.Adapter<MAdapter.MHolder> {

    private Context context;

    private List<String> list;

    public MAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    public void addHeadData(String s) {
        if (this.list == null) return;
        this.list.add(0, s);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new MHolder(LayoutInflater.from(context).inflate(R.layout.item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MHolder mHolder, final int i) {
        mHolder.tvData.setText(list.get(i));
        mHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context,"第"+i+"条",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    class MHolder extends RecyclerView.ViewHolder {

        TextView tvData;

        public MHolder(@NonNull View itemView) {
            super(itemView);
            tvData = itemView.findViewById(R.id.tv_item);
        }
    }

}
