package dev.nikunjgupta;

public final class Util {
    public static  <T> T nonNullOr(T obj, T substitute) {
        return obj != null ? obj : substitute;
    }
}
