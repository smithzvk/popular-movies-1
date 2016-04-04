package com.example.android.popularmovies.app;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.popularmovies.app.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MovieDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MovieDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MovieDetailsFragment extends Fragment {
    private static final String ARG_MOVIE_ID = "movie_id";

    private String movieId;

    // private OnFragmentInteractionListener mListener;

    public MovieDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param movieId The ID of the movie in the database.
     * @return A new instance of fragment MovieDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MovieDetailsFragment newInstance(String movieId) {
        MovieDetailsFragment fragment = new MovieDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MOVIE_ID, movieId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movieId = getArguments().getString(ARG_MOVIE_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
        View view = inflater.inflate(R.layout.fragment_movie_details, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.poster);
        MovieDetails data = fetchMovieInfo(movieId);
        ((TextView) view.findViewById(R.id.title)).setText(data.title);
        ((TextView) view.findViewById(R.id.date)).setText("("+data.date+")");
        ((TextView) view.findViewById(R.id.avg_rating)).setText("Average rating: " + data.avgRating);
        ((TextView) view.findViewById(R.id.plot)).setText(data.plot);
        Picasso.with(getActivity())
                .load("http://image.tmdb.org/t/p/w500/" + data.posterUrl)
                .into(imageView);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    // public void onButtonPressed(Uri uri) {
    //     if (mListener != null) {
    //         mListener.onFragmentInteraction(uri);
    //     }
    // }

    // @Override
    // public void onAttach(Context context) {
    //     super.onAttach(context);
    //     if (context instanceof OnFragmentInteractionListener) {
    //         mListener = (OnFragmentInteractionListener) context;
    //     } else {
    //         throw new RuntimeException(context.toString()
    //                 + " must implement OnFragmentInteractionListener");
    //     }
    // }

    // @Override
    // public void onDetach() {
    //     super.onDetach();
    //     mListener = null;
    // }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    // public interface OnFragmentInteractionListener {
    //     // TODO: Update argument type and name
    //     void onFragmentInteraction(Uri uri);
    // }

    String LOG_TAG = "MovieDetailsFragment";


    private MovieDetails fetchMovieInfo(String movieId)
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String listingsJsonStr = null;

        try {
            // Construct the URL for the Movie DB query
            final String MOVIE_DB_BASE_URL =
                "https://api.themoviedb.org/3/movie/";

            Uri builtUri = Uri.parse(MOVIE_DB_BASE_URL).buildUpon().appendPath(movieId)
                .appendQueryParameter("api_key", getString(R.string.THE_MOVIE_DB_API_TOKEN))
                .build();

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "Built URI " + builtUri.toString());

            // Create the request to The Movie DB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            listingsJsonStr = buffer.toString();

            Log.v(LOG_TAG, "Movie listings string: " + listingsJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the movie data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {

            JSONObject movieInfo = new JSONObject(listingsJsonStr);
            MovieDetails details
                = new MovieDetails(movieInfo.getString("id"),
                                   movieInfo.getString("title"),
                                   movieInfo.getString("release_date"),
                                   String.valueOf(movieInfo.getDouble("vote_average")),
                                   movieInfo.getString("overview"),
                                   movieInfo.getString("poster_path"));
            return details;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }
}
