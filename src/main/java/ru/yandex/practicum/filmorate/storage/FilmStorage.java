package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {

    List<Film> findAll();

    Film create(Film film);

    Film update(Film film);

    Film getFilmById(Long id);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getPopularFilms(Integer count, Long genreId, Integer year);

    List<Film> getRecommendations(Long id);

    List<Film> getDirectorFilmsByYear(Long directorId);

    List<Film> getDirectorFilmsByLikes(Long directorId);

    void remove(Long filmId);

    List<Film> findFilm(String findingSubstring, List<String> params);
}
