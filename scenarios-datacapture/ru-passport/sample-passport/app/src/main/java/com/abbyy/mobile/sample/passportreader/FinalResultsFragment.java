// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.abbyy.mobile.sample.passportreader.interfaces.IFinalResultsFragment;

import java.util.ArrayList;
import java.util.List;

// Фрагмент для отображения окончательных результатов распознавания
// Fragment for final recognition results displaying
public class FinalResultsFragment extends Fragment implements IFinalResultsFragment {
	public FinalResultsFragment()
	{
	}

	// Создание экземляра фрагмента. В аргументах: имена и значения распознанных полей
	// ResultFragment instance creating. The names and values of recognized fields are in arguments
	public static FinalResultsFragment newInstance( @NonNull ArrayList<String> names, @NonNull ArrayList<String> values )
	{
		FinalResultsFragment fragment = new FinalResultsFragment();
		Bundle args = new Bundle();
		args.putStringArrayList( ARG_NAMES, names );
		args.putStringArrayList( ARG_VALUES, values );
		fragment.setArguments( args );
		return fragment;
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		if( getArguments() != null ) {
			names = getArguments().getStringArrayList( ARG_NAMES );
			values = getArguments().getStringArrayList( ARG_VALUES );
			if( names == null || values == null || ( names.size() != values.size() ) ) {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.fragment_final_results, container, false );

		fillRecognizedFields();

		fieldsRecyclerView = (RecyclerView) view.findViewById( R.id.fields_list );
		fieldsRecyclerView.setAdapter( recyclerViewAdapter );
		fieldsRecyclerView.setItemAnimator( new DefaultItemAnimator() );
		fieldsRecyclerView.setLayoutManager( new LinearLayoutManager( view.getContext() ) );

		retryButton = (ImageButton) view.findViewById( R.id.retry_button );
		retryButton.setOnClickListener( new View.OnClickListener() {
			@Override public void onClick( View v )
			{
				callback.onRetryButtonPressed();
			}
		} );

		return view;
	}

	// Callback для обратного взаимодействия
	// Callback for interaction
	public interface OnInteractionCallback {
		// Поизошло нажатие на кнопку перераспознавания
		// User has pressed on retry button
		void onRetryButtonPressed();
	}

	// Установка имен и значений распознанных полей
	// Recognized fields names and values setter
	@Override
	public void setNamesAndValues( List<String> names, List<String> values )
	{
		this.names = names;
		this.values = values;
		fillRecognizedFields();
		recyclerViewAdapter.notifyDataSetChanged();
	}

	// OnInteractionCallback setter
	@Override
	public void setCallback( OnInteractionCallback value )
	{
		callback = value;
	}

	private OnInteractionCallback callback;

	private static final String ARG_NAMES = "finalNames";
	private static final String ARG_VALUES = "finalValues";

	private List<String> names;
	private List<String> values;

	private ImageButton retryButton;

	private RecyclerView fieldsRecyclerView;
	private List<RecyclerViewAdapter.DataFieldInfo> recognizedFields = new ArrayList<>();

	private void fillRecognizedFields()
	{
		if( names.size() != names.size() ) {
			throw new IllegalArgumentException( "Names and values count mismatch" );
		}
		recognizedFields.clear();
		for( int i = 0; i < names.size(); i++ ) {
			RecyclerViewAdapter.DataFieldInfo item = new RecyclerViewAdapter.DataFieldInfo();
			item.name = names.get( i );
			item.value = values.get( i );
			recognizedFields.add( item );
		}
	}

	private RecyclerView.Adapter recyclerViewAdapter = new RecyclerViewAdapter( recognizedFields, R.layout.editable_field_layout );
}
