package site.memberservice.auth.domain;

public enum LoginType {

    NORMAL,
    PUBLIC_PC,
    ;

    public static LoginType from(final boolean keepLoggedIn) {
        return keepLoggedIn ? NORMAL : PUBLIC_PC;
    }
}
