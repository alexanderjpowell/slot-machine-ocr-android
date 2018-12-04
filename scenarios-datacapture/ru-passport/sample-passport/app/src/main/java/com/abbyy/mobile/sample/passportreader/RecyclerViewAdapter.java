// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

// Адаптер для RecyclerView. Может работать с разными layout'ами ячеек.
// RecyclerView adapter. Can work with different items' layouts.
public class RecyclerViewAdapter extends RecyclerView.Adapter {

	public RecyclerViewAdapter( List<DataFieldInfo> fields, int layoutId )
	{
		recognizedFields = fields;
		this.layoutId = layoutId;
	}

	@Override public RecyclerView.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType )
	{
		View result = LayoutInflater.from( parent.getContext() ).inflate( layoutId, parent, false );
		return new FieldViewHolder( result );
	}

	@Override public void onBindViewHolder( RecyclerView.ViewHolder holder, int position )
	{
		FieldViewHolder itemHolder = (FieldViewHolder) holder;
		DataFieldInfo itemData = recognizedFields.get( position );
		itemHolder.value.setText( itemData.value );
		itemHolder.name.setText( itemData.name );
	}

	@Override public int getItemCount()
	{
		return recognizedFields.size();
	}

	private static class FieldViewHolder extends RecyclerView.ViewHolder {
		private TextView name;
		private TextView value;

		public FieldViewHolder( View itemView )
		{
			super( itemView );
			name = (TextView) itemView.findViewById( R.id.field_name );
			value = (TextView) itemView.findViewById( R.id.field_value );
		}
	}

	private List<DataFieldInfo> recognizedFields;
	private int layoutId;

	public static class DataFieldInfo {
		public String name;
		public String value;
	}
};
