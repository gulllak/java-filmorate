package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Set;

public interface GenreStorage {
    List<Genre> findAll();

    Genre getById(Long id);

    List<Genre> getGenreListByFilmId(Long filmId);

    void setFilmsGenres(Long filmId, Set<Genre> genres);
}
