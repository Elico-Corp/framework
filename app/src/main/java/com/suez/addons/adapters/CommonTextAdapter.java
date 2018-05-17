package com.suez.addons.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-15.
 */

public class CommonTextAdapter extends RecyclerView.Adapter<CommonTextAdapter.CommonTextViewHolder> {
    private List datas;
    private int itemLayout;
    private String[] fields;
    private int[] resIds;
    private List<HashMap<String, Object>> specify;
    private CommonTextAdapter.OnItemClickListener mOnItemClickListener;

    public CommonTextAdapter(List datas, int itemLayout, String[] fields, int[] resIds) {
        this.datas = datas;
        this.itemLayout = itemLayout;
        this.fields = fields;
        this.resIds = resIds;
    }

    public CommonTextAdapter(List datas, int itemLayout, String[] fields, int[] resIds, List<HashMap<String, Object>> maps) {
        this(datas, itemLayout, fields, resIds);
        this.specify = maps;
    }

    @Override
    public CommonTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new CommonTextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CommonTextViewHolder holder, int position) {
        for (int i=0; i<fields.length||i<resIds.length; i++) {
            holder.itemView.setBackgroundResource(R.drawable.suez_recycler_bg);
            ((TextView) holder.itemView.findViewById(resIds[i])).setText(((ODataRow)datas.get(position)).getString(fields[i]));
        }

        if (specify != null && specify.size() > 0) {
            for (HashMap map: specify) {
                ((TextView) holder.itemView.findViewById((Integer) map.get("resId"))).setText(String.valueOf(map.get("text")));
            }
        }

        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(holder.getAdapterPosition());
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickListener.onItemLongClick(holder.getAdapterPosition());
                    return true;
                }
            });
        }
    }

    public void setmOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    class CommonTextViewHolder extends RecyclerView.ViewHolder {
        public CommonTextViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }
}
