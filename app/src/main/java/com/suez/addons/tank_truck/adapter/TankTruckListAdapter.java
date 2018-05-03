package com.suez.addons.tank_truck.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;

import java.util.List;

/**
 * Created by joseph on 18-4-28.
 */

public class TankTruckListAdapter extends RecyclerView.Adapter<TankTruckListAdapter.TankTruckListViewHolder> {

    private List datas;
    private int itemLayout;
    private OnItemClickListener mOnItemClickListener;

    public TankTruckListAdapter(List datas, int itemLayout){
        this.datas = datas;
        this.itemLayout = itemLayout;
    }

    public interface OnItemClickListener {
        void ItemOnClick(int position);

        void ItemOnLongClick(int position);
    }

    public void setmOnItemClickListener(OnItemClickListener onItemClickListener){
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public TankTruckListViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        TankTruckListViewHolder holder = new TankTruckListViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final TankTruckListViewHolder holder, final int position){
        holder.mTextTankItemNum.setText(((ODataRow) datas.get(position)).getString("name"));

        if (mOnItemClickListener!=null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.ItemOnClick(position);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickListener.ItemOnLongClick(position);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount(){
        return datas.size();
    }

    class TankTruckListViewHolder extends RecyclerView.ViewHolder{
        public TextView mTextTankItemNum;

        public TankTruckListViewHolder(View itemView){
            super(itemView);
            this.mTextTankItemNum = (TextView) itemView.findViewById(R.id.txtTankItemNum);
        }
    }
}
