package site.explorationservice.productindex.presentation;

import java.util.List;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * 추천 결과를 눈으로 볼 수 있게 넣어두는 예시 상품 15건.
 * <p>
 * <b>아티스트·장르가 일부러 겹치게 짜여 있다.</b> 전부 다른 상품만 넣으면 kNN이 무엇을 가까이 놓든 그게 맞는지 알 수 없다. 같은 아티스트의 다른 앨범(Miles
 * Davis 3장, 김광석 2장 등), 같은 장르의 다른 아티스트(Coltrane과 Evans), 같은 연대의 다른 지역이 섞여 있어야 "무엇을 기준으로 가깝다고 판단했는가"가
 * 결과 순서에 드러난다.
 * <p>
 * id를 9001부터 쓰는 건 실제 상품 id와 겹치지 않게 하려는 것이다 — 같은 인덱스를 쓰므로 나중에 이 범위만 지우면 정리된다.
 * <p>
 * 한국어를 자바 소스에 두는 이유는 <b>curl로 한글 본문을 보내면 Git Bash에서 CP949로 깨지기 때문</b>이다. 소스에 두면 인코딩 문제 없이 그대로 색인된다.
 */
final class SampleProducts {

    private static final List<ProductIndexCommand> COMMANDS = List.of(
        // 재즈 — 같은 아티스트 여러 장, 같은 장르 다른 아티스트가 겹친다.
        product(9001L, "Kind of Blue", "Miles Davis", "Jazz", "Columbia", 1959, "미국", "ORIGINAL"),
        product(9002L, "Bitches Brew", "Miles Davis", "Jazz Fusion", "Columbia", 1970, "미국",
            "ORIGINAL"),
        product(9003L, "'Round About Midnight", "Miles Davis", "Jazz", "Columbia", 1957, "미국",
            "REISSUE"),
        product(9004L, "A Love Supreme", "John Coltrane", "Jazz", "Impulse!", 1965, "미국",
            "ORIGINAL"),
        product(9005L, "Blue Train", "John Coltrane", "Hard Bop", "Blue Note", 1958, "미국",
            "REISSUE"),
        product(9006L, "Waltz for Debby", "Bill Evans Trio", "Jazz", "Riverside", 1962, "미국",
            "ORIGINAL"),
        product(9007L, "Sunday at the Village Vanguard", "Bill Evans Trio", "Jazz", "Riverside",
            1961, "미국", "REISSUE"),

        // 한국 — 언어가 다른 문서끼리도 아티스트·장르로 묶이는지 본다.
        product(9008L, "김광석 네 번째", "김광석", "포크", "킹레코드", 1994, "한국", "ORIGINAL"),
        product(9009L, "다시 부르기 2", "김광석", "포크", "킹레코드", 1995, "한국", "REISSUE"),
        product(9010L, "산울림 새노래 모음", "산울림", "록", "서라벌레코드", 1977, "한국", "ORIGINAL"),
        product(9011L, "산울림 3집", "산울림", "사이키델릭 록", "서라벌레코드", 1978, "한국", "REISSUE"),

        // 록 — 재즈 쪽과 확실히 떨어져야 정상이다.
        product(9012L, "The Dark Side of the Moon", "Pink Floyd", "프로그레시브 록", "Harvest", 1973,
            "영국", "ORIGINAL"),
        product(9013L, "Wish You Were Here", "Pink Floyd", "프로그레시브 록", "Harvest", 1975, "영국",
            "REISSUE"),
        product(9014L, "Nevermind", "Nirvana", "그런지", "DGC", 1991, "미국", "ORIGINAL"),
        product(9015L, "In Utero", "Nirvana", "그런지", "DGC", 1993, "미국", "ORIGINAL")
    );

    private SampleProducts() {
    }

    static List<ProductIndexCommand> all() {
        return COMMANDS;
    }

    private static ProductIndexCommand product(final Long productId, final String title,
        final String artistName, final String genre, final String label, final Integer releaseYear,
        final String releaseCountry, final String pressType) {
        return new ProductIndexCommand(productId, title, artistName, genre, label,
            releaseYear, releaseCountry, pressType, true);
    }
}
