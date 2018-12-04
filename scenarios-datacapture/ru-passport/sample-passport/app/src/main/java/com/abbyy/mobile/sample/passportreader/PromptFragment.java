// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abbyy.mobile.sample.passportreader.interfaces.IPromptFragment;

// Фрагмент с отображением подсказки
// Fragment for prompt displaying
public class PromptFragment extends Fragment implements IPromptFragment {
	static final String PICTURE_ID_PARAM = "pictureIdPromptFragment";
	static final String RECOGNIZED_PAGE_PICTURE_ID_PARAM = "recognizedTextPictureIdPromptFragment";
	static final String TEXT_PARAM = "text";
	static final int DEFAULT_VALUE = -1;

	private Integer promptPictureId;
	private Integer recognizedPagePictureId;
	private String promptText;

	public PromptFragment() { }

	// Создание нового экземпляра PromptFragment. Аргументы: крупная картинка-подсказка, небольшая картинка-подсказка
	// уже распознанной страницы и сообщение.
	// The new instance of PromptFragment. Arguments: huge prompt picture, small picture of already recognized text and
	// message.
	public static PromptFragment newInstance( Integer promptPictureId, Integer recognizedPagePictureId, String text )
	{
		PromptFragment fragment = new PromptFragment();
		Bundle args = new Bundle();
		if( promptPictureId != null ) {
			args.putInt( PICTURE_ID_PARAM, promptPictureId );
		}
		if( recognizedPagePictureId != null ) {
			args.putInt( RECOGNIZED_PAGE_PICTURE_ID_PARAM, recognizedPagePictureId );
		}
		args.putString( TEXT_PARAM, text );
		fragment.setArguments( args );
		return fragment;
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		if( getArguments() != null ) {
			promptText = getArguments().getString( TEXT_PARAM );
			promptPictureId = getArguments().getInt( PICTURE_ID_PARAM, DEFAULT_VALUE );
			recognizedPagePictureId = getArguments().getInt( RECOGNIZED_PAGE_PICTURE_ID_PARAM, DEFAULT_VALUE );
			if( promptPictureId == DEFAULT_VALUE ) {
				promptPictureId = null;
			}
			if( recognizedPagePictureId == DEFAULT_VALUE ) {
				recognizedPagePictureId = null;
			}
		}
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState )
	{
		View view = inflater.inflate( R.layout.fragment_prompt, container, false );
		updateUiValues( view );
		return view;
	}

	// Установка идентификатора картинки - подсказки
	// Prompt picture id setter
	@Override public void setPromptPictureId( @Nullable Integer promptPictureId )
	{
		this.promptPictureId = promptPictureId;
		updateUiValues( getView() );
	}

	// Установка идентификатора картинки уже распознанной страницы
	// Already recognized picture id setter
	@Override public void setRecognizedPagePictureId( @Nullable Integer pictureId )
	{
		this.recognizedPagePictureId = pictureId;
		updateUiValues( getView() );
	}

	// Установка текста подсказки
	// Prompt text setter
	@Override public void setPromptText( @Nullable String promptText )
	{
		this.promptText = promptText;
		updateUiValues( getView() );
	}

	private void updatePicture( int viewId, @Nullable Integer pictureId, @NonNull View view )
	{
		ImageView imageView = (ImageView) view.findViewById( viewId );
		imageView.setImageDrawable( pictureId == null ? null : getResources().getDrawable( pictureId ) );
	}

	private void updateUiValues( View view )
	{
		if( view != null ) {
			( (TextView) view.findViewById( R.id.prompt_text ) ).setText( promptText != null ? promptText : "" );
			updatePicture( R.id.prompt_image, promptPictureId, view );
			updatePicture( R.id.recognized_page_image, recognizedPagePictureId, view );
		}
	}
}
