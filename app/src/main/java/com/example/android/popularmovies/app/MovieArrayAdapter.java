package com.example.android.popularmovies.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by smithzv on 4/2/16.
 */
public class MovieArrayAdapter extends ArrayAdapter<MovieDetails> {

    // declaring our List of items
    private List<MovieDetails> listings;

    public MovieArrayAdapter(Context context, int resource, int textViewResourceId, List<MovieDetails> objects) {
        super(context, resource, textViewResourceId, objects);
        listings = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.list_item_listing, null);
        }

        MovieDetails details = listings.get(position);

        if (details != null) {
            TextView titleView = (TextView) v.findViewById(R.id.list_item_listing_textview);
            ImageView posterView = (ImageView) v.findViewById(R.id.list_item_poster);
            if (titleView != null){
                titleView.setText(details.title);
            }
            if (posterView != null)
            {
                Picasso.with(parent.getContext())
                        .load("http://image.tmdb.org/t/p/w185/" + details.posterUrl)
                        .into(posterView);
            }
        }

        return v;

    }
}
