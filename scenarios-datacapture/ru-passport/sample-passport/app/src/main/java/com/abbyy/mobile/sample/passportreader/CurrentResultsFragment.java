// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.abbyy.mobile.sample.passportreader.interfaces.ICurrentResultsFragment;

import java.util.ArrayList;
import java.util.List;

// Фрагмент для отображения результатов распознавания (в процессе распознавнания)
// The fragment for the results displaying (during recognition process)
public class CurrentResultsFragment extends Fragment implements ICurrentResultsFragment {

	static final String ARG_PICTURE_TOP = "resultFragmentPictureTop";
	static final String ARG_PICTURE_BOTTOM = "resultFragmentPictureBottom";
	static final int PICTURE_ID_DEFAULT_VALUE = -1;
	static final int ANIMATION_PERIOD_MILLISECONDS = 500;

	public CurrentResultsFragment() {}

	// Создание экземляра фрагмента. В UI есть две картинки подсказки. Их идентификаторы в аргументах.
	// ResultFragment instance creating. There are two prompt pictures in the UI. The ids of them are in arguments.
	public static CurrentResultsFragment newInstance( Integer pictureTop, Integer pictureBottom )
	{
		CurrentResultsFragment fragment = new CurrentResultsFragment();
		Bundle args = new Bundle();
		if( pictureTop != null ) {
			args.putInt( ARG_PICTURE_TOP, pictureTop );
		}
		if( pictureBottom != null ) {
			args.putInt( ARG_PICTURE_BOTTOM, pictureBottom );
		}
		fragment.setArguments( args );
		return fragment;
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		if( getArguments() != null ) {
			topImageId = getArguments().getInt( ARG_PICTURE_TOP, PICTURE_ID_DEFAULT_VALUE );
			if( topImageId == PICTURE_ID_DEFAULT_VALUE ) {
				topImageId = null;
			}
			bottomImageId = getArguments().getInt( ARG_PICTURE_BOTTOM, PICTURE_ID_DEFAULT_VALUE );
			if( bottomImageId == PICTURE_ID_DEFAULT_VALUE ) {
				bottomImageId = null;
			}
		}

	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.fragment_current_results, container, false );

		fieldsRecyclerView = (RecyclerView) view.findViewById( R.id.fields_list );
		fieldsRecyclerView.setAdapter( recyclerViewAdapter );
		fieldsRecyclerView.setLayoutManager( new LinearLayoutManager( view.getContext() ) );
		fieldsRecyclerView.setItemAnimator( new DefaultItemAnimator() );
		fieldsRecyclerView.setOnTouchListener( listener );

		topImageView = (ImageView) view.findViewById( R.id.passport_picture_top );
		bottomImageView = (ImageView) view.findViewById( R.id.passport_picture_bottom );
		setImage( topImageId, topImageView );
		setImage( bottomImageId, bottomImageView );

		initProgressBar( view );
		return view;
	}

	private void initProgressBar( View view )
	{
		progressBar = (LinearLayout) view.findViewById( R.id.progress_bar );

		int childCount = progressBar.getChildCount();
		for( int i = 0; i < childCount; i++ ) {
			final ImageView picture = (ImageView) progressBar.getChildAt( i );
			final Animation animationUp = AnimationUtils.loadAnimation( picture.getContext(), R.anim.progress_animation_up );
			final Animation animationDown = AnimationUtils.loadAnimation( picture.getContext(), R.anim.progress_animation_down );

			final Animation.AnimationListener upListener = new Animation.AnimationListener() {
				@Override public void onAnimationEnd( Animation animation )
				{
					picture.startAnimation( animationDown );
				}

				@Override public void onAnimationStart( Animation animation ) {}

				@Override public void onAnimationRepeat( Animation animation ) {}
			};
			Animation.AnimationListener downListener = new Animation.AnimationListener() {
				@Override public void onAnimationEnd( Animation animation )
				{
					animationUp.setStartOffset( 0 );
					picture.startAnimation( animationUp );
				}

				@Override public void onAnimationStart( Animation animation ) {}

				@Override public void onAnimationRepeat( Animation animation ) {}
			};

			animationDown.setAnimationListener( downListener );
			animationUp.setAnimationListener( upListener );
			animationUp.setStartOffset( ANIMATION_PERIOD_MILLISECONDS / childCount * i );
			picture.startAnimation( animationUp );
		}

		setStable( false );
	}

	@Override
	public void setOnTouchListener( View.OnTouchListener listener )
	{
		// Необходимо подключить этот listener, не смотря на то, что он подключен к родительской view,
		// т.к. RecyclerView перехватывает нажатия.
		// It is essential to attach this listener because RecyclerView handles touch events.
		this.listener = listener;
		if( fieldsRecyclerView != null ) {
			fieldsRecyclerView.setOnTouchListener( listener );
		}
	}

	// Добавление нового распознанного поля
	// Inserts new recognized field
	@Override
	public void insertField( int index, String name, String text )
	{
		RecyclerViewAdapter.DataFieldInfo item = new RecyclerViewAdapter.DataFieldInfo();
		item.name = name;
		item.value = text;
		recognizedFields.add( index, item );
		recyclerViewAdapter.notifyItemInserted( index );
	}

	// Обновление значение распознанного поля
	// Updates value of the new recognized field
	@Override
	public void updateFieldValue( int index, String text )
	{
		recognizedFields.get( index ).value = text;
		recyclerViewAdapter.notifyItemChanged( index );
	}

	// Удаляет распознанное поле по индексу
	// Removes recognized field by index
	@Override
	public void removeFieldAt( int index )
	{
		recognizedFields.remove( index );
		recyclerViewAdapter.notifyItemRemoved( index );
	}

	// В UI есть две картинки подсказки. Этот setter устанавливает верхнюю из них
	// There are two prompt pictures in the UI. This setter configures the top one
	@Override
	public void setTopPicture( @Nullable Integer id )
	{
		topImageId = id;
		if( topImageView != null ) {
			setImage( topImageId, topImageView );
		}
	}

	// В UI есть две картинки подсказки. Этот setter устанавливает нижнюю из них
	// There are two prompt pictures in the UI. This setter configures the bottom one
	@Override
	public void setBottomPicture( @Nullable Integer id )
	{
		bottomImageId = id;
		if( bottomImageView != null ) {
			setImage( id, bottomImageView );
		}
	}

	// Очищает результаты
	// Clears the recognition results
	@Override
	public void clearResult()
	{
		recognizedFields.clear();
		recyclerViewAdapter.notifyDataSetChanged();
	}

	// Управление стабильностью (и цветом прогресс-бара)
	// Stability setter (influences progress-bar color)
	@Override
	public void setStable( boolean stable )
	{
		int id = stable ? R.drawable.progress_green_circle : R.drawable.progress_yellow_circle;

		if( progressBar != null ) {
			int childCount = progressBar.getChildCount();
			for( int i = 0; i < childCount; i++ ) {
				final ImageView picture = (ImageView) progressBar.getChildAt( i );
				picture.setImageDrawable( getResources().getDrawable( id ) );
			}
		}
	}

	private ImageView topImageView;
	private ImageView bottomImageView;
	private LinearLayout progressBar;

	private Integer topImageId;
	private Integer bottomImageId;

	private RecyclerView fieldsRecyclerView;
	private List<RecyclerViewAdapter.DataFieldInfo> recognizedFields = new ArrayList<>();
	private View.OnTouchListener listener;

	private void setImage( @Nullable Integer id, ImageView view )
	{
		if( id == null ) {
			view.setImageDrawable( null );
		} else {
			view.setImageDrawable( getResources().getDrawable( id ) );
		}
	}

	private RecyclerView.Adapter recyclerViewAdapter =
		new RecyclerViewAdapter( recognizedFields, R.layout.field_layout );
}
