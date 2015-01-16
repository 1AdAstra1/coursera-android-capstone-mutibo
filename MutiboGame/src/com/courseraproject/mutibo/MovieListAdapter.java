package com.courseraproject.mutibo;

import java.util.ArrayList;
import java.util.List;

import com.courseraproject.mutibo.model.Movie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class MovieListAdapter extends ArrayAdapter<Movie> implements Filterable {
	private List<Movie> movies;
	private List<Movie> originalMovies;
	private LayoutInflater inflater = null;
	private int resourceId;

	public MovieListAdapter(Context context, int resource, List<Movie> items) {
		super(context, resource, items);
		movies = items;
		originalMovies = items;
		resourceId = resource;
		inflater = (LayoutInflater) context
	                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public Movie getItem(int index) {
		return movies.get(index);
	}

	@Override
	public int getCount(){
		return movies.size();
	}
	
	@Override
	 public void add(Movie m) {
        movies.add(m);
    }

	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
            convertView = inflater.inflate(resourceId,
                    parent, false);
        }
		TextView movieLabel = (TextView)  convertView.findViewById(android.R.id.text1);
		movieLabel.setText(getItem(position).getTitle());
		convertView.setTag(getItem(position));
        return convertView;
    }
	
	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,FilterResults results) {

				movies = (ArrayList<Movie>) results.values; 
				notifyDataSetChanged();  
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
				List<Movie> FilteredArrList = new ArrayList<Movie>();

				if (originalMovies == null) {
					originalMovies = new ArrayList<Movie>(movies); // saves the original data in 
				}

				if (constraint == null || constraint.length() == 0) {
					// set the Original result to return if there is no search string  
					results.count = originalMovies.size();
					results.values = originalMovies;
				} else {
					constraint = constraint.toString().toLowerCase();
					for (int i = 0; i < originalMovies.size(); i++) {
						Movie m = originalMovies.get(i);
						if (m.getTitle().startsWith(constraint.toString())) {
							FilteredArrList.add(m);
						}
					}
					// set the Filtered result to return
					results.count = FilteredArrList.size();
					results.values = FilteredArrList;
				}
				return results;
			}
			
		};
		return filter;
	}
}
