package io.ymusic.app.util;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public abstract class OnClickGesture<T> {
	
	public abstract void selected(T selectedItem);

	public void drag(T selectedItem, RecyclerView.ViewHolder viewHolder) {
		
	}
	
	public void more(T selectedItem, View view) {
		
	}
	
	public void held(T selectedItem, View view) {
		
	}
	
	public void download(T selectedItem) {
		
	}

	public void delete(T selectedItem) {

	}

	public void swipe(T selectedItem) {

	}
}
