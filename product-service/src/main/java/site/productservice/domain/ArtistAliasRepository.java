package site.productservice.domain;

import java.util.List;
import java.util.Map;

/**
 * 아티스트 별칭 저장소. 시드 적재(쓰기)와 존재 확인에만 쓴다 — 검색 조회는
 * ProductSearchRepository 담당.
 */
public interface ArtistAliasRepository {

    ArtistAlias save(ArtistAlias alias);

    /**
     * 같은 아티스트에 같은 정규화 별칭이 이미 있는지 — 시드를 여러 번 실행해도 중복이 안 쌓이게
     * 하는 확인.
     */
    boolean hasAlias(Long artistId, String normalizedName);

    /**
     * 아티스트 여러 명의 별칭 이름을 한 번에 가져온다. 별칭이 아티스트에 붙어 있어 묶는 단위도
     * 아티스트다 — 한 아티스트의 상품이 여럿이면 그 상품들이 같은 별칭을 나눠 갖는다.
     * <p>
     * 별칭이 하나도 없는 아티스트는 <b>키 자체가 없다.</b> 부르는 쪽에서 빈 목록으로 받는다.
     */
    Map<Long, List<String>> findNamesByArtistIds(List<Long> artistIds);
}
