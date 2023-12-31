package ru.yandex.practicum.filmorate.storage.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.LikeStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Primary
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    private final GenreStorage genreStorage;

    private final DirectorStorage directorStorage;

    private final LikeStorage likeStorage;

    private final UserStorage userStorage;

    @Override
    public List<Film> findAll() {
        String sqlQuery = "SELECT f.id, f.name, f.description, f.releaseDate, f.duration, m.id AS mpa_id, m.name AS mpa_name, g.id AS genre_id, g.name AS genre_name, d.id AS director_id, d.name AS director_name " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.id " +
                "GROUP BY f.id, m.id, g.id, d.id";
        return jdbcTemplate.query(sqlQuery, this::createFilm);
    }

    @Override
    public Film create(Film film) {
        String sqlQuery = "INSERT INTO films (name, description, releasedate, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sqlQuery, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setLong(4, film.getDuration());
            stmt.setLong(5, film.getMpa().getId());
            return stmt;
        }, keyHolder);

        Long filmId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        genreStorage.setFilmsGenres(filmId, film.getGenres());
        directorStorage.setFilmsDirectors(filmId, film.getDirectors());
        return getFilmById(filmId);
    }

    @Override
    public Film update(Film film) {
        String sqlQuery = "UPDATE films SET name = ?, description = ?, releasedate = ?, duration = ?, mpa_id =? WHERE id = ?";
        jdbcTemplate.update(sqlQuery, film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), film.getMpa().getId(), film.getId());
        genreStorage.setFilmsGenres(film.getId(), film.getGenres());
        directorStorage.setFilmsDirectors(film.getId(), film.getDirectors());
        return getFilmById(film.getId());
    }

    @Override
    public Film getFilmById(Long id) {
        String sqlQuery = "SELECT f.id, f.name, f.description, f.releaseDate, f.duration, m.id AS mpa_id, m.name AS mpa_name, g.id AS genre_id, g.name AS genre_name, d.id AS director_id, d.name AS director_name " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.id " +
                "WHERE f.id = ? " +
                "GROUP BY f.id, m.id, g.id, d.id";

        List<Film> films = jdbcTemplate.query(sqlQuery, this::createFilm, id);
        if (films.size() != 1) {
            throw new EntityNotFoundException(String.format("Фильм c id %s отсутствует", id));
        }
        return films.get(0);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        getFilmById(filmId);
        userStorage.getUserById(userId);

        likeStorage.addLike(filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        likeStorage.removeLike(filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(Integer limit, Long genreId, Integer year) {
        List<Film> films = new ArrayList<>();

        for (Long id : likeStorage.getPopularFilms(limit, genreId, year)) {
            films.add(getFilmById(id));
        }
        return films;
    }

    @Override
    public List<Film> getDirectorFilmsByYear(Long directorId) {
        directorStorage.getById(directorId);

        String sqlQuery = "SELECT f.id, f.name, f.description, f.releaseDate, f.duration, m.id AS mpa_id, m.name AS mpa_name, g.id AS genre_id, g.name AS genre_name, d.id AS director_id, d.name AS director_name " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.id " +
                "WHERE d.id = ? " +
                "GROUP BY d.id, f.releaseDate " +
                "ORDER BY EXTRACT(YEAR FROM CAST(f.releaseDate AS DATE))";
        return jdbcTemplate.query(sqlQuery, this::createFilm, directorId);
    }

    @Override
    public List<Film> getDirectorFilmsByLikes(Long directorId) {
        directorStorage.getById(directorId);

        String sqlQuery = "SELECT f.id, f.name, f.description, f.releaseDate, f.duration, m.id AS mpa_id, m.name AS mpa_name, g.id AS genre_id, g.name AS genre_name, d.id AS director_id, d.name AS director_name, COUNT(l.film_id) AS count_likes " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "WHERE d.id = ? " +
                "GROUP BY d.id, g.id " +
                "ORDER BY count_likes";
        return jdbcTemplate.query(sqlQuery, this::createFilm, directorId);
    }

    @Override
    public List<Film> getRecommendations(Long id) {
        String sqlQuery = "SELECT l.film_id " +
                          "FROM likes AS l " +
                          "WHERE l.user_id = (SELECT l2.user_id " + // Поиск пользователя с похожими вкусами
                                              "FROM likes AS l2 " +
                                              "WHERE l2.user_id <> ? " +
                                                "AND l2.film_id IN (SELECT l3.film_id " + // Лайки
                                                                    "FROM likes AS l3 " +
                                                                    "WHERE l3.user_id = ?) " +
                                              "GROUP BY l2.user_id " +
                                              "ORDER BY COUNT(l2.film_id) DESC " + // Сортировка по пересечениям лайков
                                              "LIMIT 1)";

        List<Long> recommendations = jdbcTemplate.queryForList(sqlQuery, Long.class, id, id);

        List<Long> alreadyLiked = likeStorage.getLikedFilmsByUserId(id);
        recommendations.removeAll(alreadyLiked);

        return recommendations.stream()
                .map(this::getFilmById)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(Long filmId) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", filmId);
    }

    @Override
    public List<Film> findFilm(String findingSubstring, List<String> params) {
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery
                .append("SELECT f.id, f.name, f.description, f.releaseDate, f.duration, m.id AS mpa_id, m.name AS mpa_name, ")
                .append("g.id AS genre_id, g.name AS genre_name, d.id AS director_id, d.name AS director_name, COUNT(l.user_id) AS like_count ")
                .append("FROM films f ")
                .append("LEFT JOIN mpa m ON f.mpa_id = m.id ")
                .append("LEFT JOIN film_genres fg ON f.id = fg.film_id ")
                .append("LEFT JOIN genres g ON fg.genre_id = g.id ")
                .append("LEFT JOIN film_directors fd ON f.id = fd.film_id ")
                .append("LEFT JOIN directors d ON fd.director_id = d.id ")
                .append("LEFT JOIN likes l ON f.id = l.film_id ");

        if (params.contains("title") & params.contains("director")) {
            sqlQuery.append("WHERE LOWER(f.name) LIKE LOWER(?) OR LOWER(d.name) LIKE LOWER(?) ")
                    .append("GROUP BY f.id ")
                    .append("ORDER BY like_count DESC");
            return jdbcTemplate.query(sqlQuery.toString(), this::createFilm, String.format("%%%s%%", findingSubstring), String.format("%%%s%%", findingSubstring));
        } else if (params.contains("title")) {
            sqlQuery.append("WHERE LOWER(f.name) LIKE LOWER(?) ");
        } else if (params.contains("director")) {
            sqlQuery.append("WHERE LOWER(d.name) LIKE LOWER(?) ");
        } else {
            throw new RuntimeException("Неверные параметры запроса");
        }
        sqlQuery.append("GROUP BY f.id ")
                .append("ORDER BY like_count DESC");

        return jdbcTemplate.query(sqlQuery.toString(), this::createFilm, String.format("%%%s%%", findingSubstring));
    }

    private List<Film> createFilm(ResultSet rs) throws SQLException {
        ResultSetExtractor<List<Film>> resultSetExtractor = rs1 -> {
            Map<Long, Film> list = new LinkedHashMap<>();
            while (rs1.next()) {
                if (list.containsKey(rs1.getLong("id"))) {
                    if (rs1.getLong("genre_id") != 0) {
                        list.get(rs1.getLong("id")).getGenres().add(Genre.builder()
                                .id(rs1.getLong("genre_id"))
                                .name(rs1.getString("genre_name"))
                                .build());
                    }

                    if (rs1.getLong("director_id") != 0) {
                        list.get(rs1.getLong("id")).getDirectors().add(Director.builder()
                                .id(rs1.getLong("director_id"))
                                .name(rs1.getString("director_name"))
                                .build());
                    }
                } else {
                    Film film = Film.builder()
                            .id(rs1.getLong("id"))
                            .name(rs1.getString("name"))
                            .description(rs1.getString("description"))
                            .releaseDate(rs1.getDate("releasedate").toLocalDate())
                            .duration(rs1.getInt("duration"))
                            .mpa(Mpa.builder()
                                    .id(rs1.getLong("mpa_id"))
                                    .name(rs1.getString("mpa_name"))
                                    .build())
                            .genres(new HashSet<>())
                            .directors(new HashSet<>())
                            .build();

                    if (rs1.getLong("genre_id") != 0) {
                        film.getGenres().add(Genre.builder()
                                .id(rs1.getLong("genre_id"))
                                .name(rs1.getString("genre_name"))
                                .build());
                    }

                    if (rs1.getLong("director_id") != 0) {
                        film.getDirectors().add(Director.builder()
                                .id(rs1.getLong("director_id"))
                                .name(rs1.getString("director_name"))
                                .build());
                    }
                    list.put(film.getId(), film);
                }
            }
            return new ArrayList<>(list.values());
        };
        return resultSetExtractor.extractData(rs);
    }
}
