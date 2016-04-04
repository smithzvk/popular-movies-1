
package com.example.android.popularmovies.app;

public class MovieDetails {
    public MovieDetails() { }

    public MovieDetails(String movieId,
                        String movieTitle,
                        String movieDate,
                        String movieAvgRating,
                        String moviePlot,
                        String moviePosterUrl)
    {
        id = movieId;
        title = movieTitle;
        date = movieDate;
        plot = moviePlot;
        avgRating = movieAvgRating;
        posterUrl = moviePosterUrl;
    }

    public String
        id,
        title,
        date,
        plot,
        avgRating,
        posterUrl;
}
