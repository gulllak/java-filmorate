package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    private final EventStorage eventStorage;

    public UserService(UserStorage userStorage,
                       FilmStorage filmStorage,
                       EventStorage eventStorage) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.eventStorage = eventStorage;
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User user) {
        userStorage.getUserById(user.getId());

        return userStorage.update(user);
    }

    public List<User> getFriends(Long id) {
        userStorage.getUserById(id);

        return userStorage.getFriends(id);
    }

    public void addFriend(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);

        userStorage.addFriend(id, friendId);
    }

    public void removeFriend(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);

        userStorage.removeFriend(id, friendId);
    }

    public List<User> getCommonFriends(Long id, Long otherId) {
        userStorage.getUserById(id);
        userStorage.getUserById(otherId);

        return userStorage.getCommonFriends(id, otherId);
    }

    public List<Film> getRecommendations(Long id) {
        return filmStorage.getRecommendations(id);
    }

    public void remove(Long userId) {
        userStorage.remove(userId);
    }

    public List<Event> getFeedByUserId(Long id) {
        userStorage.getUserById(id);

        return eventStorage.getFeedByUserId(id);
    }
}
