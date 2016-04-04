/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.popularmovies.app;

import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates fetching the movie listings and displaying it as a {@link ListView} layout.
 */
public class MovieListingFragment extends Fragment {

    private MovieArrayAdapter movieAdapter;
    private List<MovieDetails> movies;

    public MovieListingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.listingfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchMovieListingTask movieListingTask = new FetchMovieListingTask();
            Spinner spinner = (Spinner) getActivity().findViewById(R.id.sort_spinner);
            movieListingTask.execute(String.valueOf(spinner.getSelectedItemPosition()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        movies = new ArrayList<MovieDetails>();

        movieAdapter =
                new MovieArrayAdapter(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_listing, // The name of the layout ID.
                        R.id.list_item_listing_textview, // The ID of the textview to populate.
                        movies);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        Spinner spinner = (Spinner) rootView.findViewById(R.id.sort_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter
            = ArrayAdapter.createFromResource(getActivity(), R.array.sort_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        FetchMovieListingTask movieListingTask = new FetchMovieListingTask();
        movieListingTask.execute(String.valueOf(spinner.getSelectedItemPosition()));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                FetchMovieListingTask movieListingTask = new FetchMovieListingTask();
                movieListingTask.execute(String.valueOf(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        // Get a reference to the ListView, and attach this adapter to it.
        GridView gridView = (GridView) rootView.findViewById(R.id.grid_view_listing);
        gridView.setAdapter(movieAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Log.i("MovieListingFragment", "Item clicked: " + view.toString() + " " + i + " " + l);
                getActivity().getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.container, MovieDetailsFragment.newInstance(movies.get(i).id))
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
            }
        });

        return rootView;
    }

    public class FetchMovieListingTask extends AsyncTask<String, Void, MovieDetails[]> {

        private final String LOG_TAG = FetchMovieListingTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Take the String representing the complete listings in JSON Format and
         * pull out the data we need to construct the Strings needed.
         */
        private MovieDetails[] getMovieDataFromJson(String listingsJsonStr)
                throws JSONException {

            JSONObject listingsJson = new JSONObject(listingsJsonStr);
            JSONArray resultObj = listingsJson.getJSONArray("results");

            MovieDetails[] results = new MovieDetails[resultObj.length()];
            for(int i = 0; i < resultObj.length(); i++) {
                JSONObject movie = resultObj.getJSONObject(i);
                results[i] = new MovieDetails();
                results[i].id = movie.getString("id");
                results[i].title = movie.getString("title");
                results[i].date = movie.getString("release_date");
                results[i].plot = movie.getString("overview");
                results[i].avgRating = movie.getString("vote_average");
                results[i].posterUrl = movie.getString("poster_path");
            }

            return results;

        }
        @Override
        protected MovieDetails[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String listingsJsonStr = null;

            // Check to ensure we have a sort option
            if (params.length != 1) {
                return null;
            }

            String sortOption;
            switch (params[0])
            {
                case "0":
                    sortOption = "popular";
                    break;
                case "1":
                    sortOption = "top_rated";
                    break;
                default:
                    Log.e("MovieListingFragment", "Invalid sort option");
                    return null;
            }

            try {
                final String MOVIE_DB_BASE_URL =
                    "https://api.themoviedb.org/3/movie/";

                Uri builtUri = Uri.parse(MOVIE_DB_BASE_URL).buildUpon().appendPath(sortOption)
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
                return getMovieDataFromJson(listingsJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the movie listings.
            return null;
        }

        @Override
        protected void onPostExecute(MovieDetails[] result) {
            if (result != null) {
                movieAdapter.clear();
                for(MovieDetails movieDetails : result) {
                    movieAdapter.add(movieDetails);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}
