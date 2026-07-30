import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { searchProducts } from "../../api/products";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { VinylCover } from "../../components/VinylCover";

export interface PickedProduct {
    productId: number;
    title: string;
    artistName: string;
    coverImageUrl: string | null;
}

interface ProductPickerProps {
    picked: PickedProduct | null;
    onPick: (product: PickedProduct | null) => void;
}

// 경매는 카탈로그에 이미 있는 음반(마스터 상품)에만 걸 수 있다.
// 새 음반을 만드는 화면이 아니라 기존 음반을 찾아 고르는 화면이다
export function ProductPicker({ picked, onPick }: ProductPickerProps) {
    const [keyword, setKeyword] = useState("");
    const debounced = useDebouncedValue(keyword, 300).trim();

    const results = useQuery({
        queryKey: ["productSearch", debounced, 0],
        queryFn: () => searchProducts(debounced, 0, 8),
        enabled: debounced.length > 0 && picked === null,
    });

    if (picked) {
        return (
            <div className="flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3">
                <VinylCover
                    title={picked.title}
                    artist={picked.artistName}
                    imageUrl={picked.coverImageUrl}
                    className="w-14 shrink-0"
                />
                <div className="min-w-0 grow">
                    <p className="truncate font-semibold">{picked.title}</p>
                    <p className="truncate text-[13px] text-muted">{picked.artistName}</p>
                    <p className="font-mono text-[11px] text-faint">상품번호 {picked.productId}</p>
                </div>
                <button
                    type="button"
                    onClick={() => onPick(null)}
                    className="shrink-0 rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-muted hover:border-line-strong hover:text-ink"
                >
                    다시 고르기
                </button>
            </div>
        );
    }

    return (
        <div>
            <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="앨범 제목·아티스트·카탈로그 번호로 검색"
                aria-label="마스터 상품 검색"
                className="w-full rounded-xl border border-line bg-surface px-3 py-2 text-[14px] outline-none focus:border-line-strong"
            />
            {debounced.length === 0 && (
                <p className="mt-2 text-[12px] text-muted">
                    카탈로그에 등록된 음반만 경매로 올릴 수 있습니다. 검색해서 고르세요.
                </p>
            )}
            {debounced.length > 0 && results.isPending && (
                <p className="mt-2 text-[12px] text-muted">검색 중…</p>
            )}
            {results.error && (
                <p className="mt-2 text-[12px] font-semibold text-live">상품 검색에 실패했습니다.</p>
            )}
            {results.data && results.data.content.length === 0 && (
                <p className="mt-2 text-[12px] text-muted">‘{debounced}’ 검색 결과가 없습니다.</p>
            )}
            {results.data && results.data.content.length > 0 && (
                <ul className="mt-2 flex flex-col gap-1.5">
                    {results.data.content.map((card) => (
                        <li key={card.productId}>
                            <button
                                type="button"
                                onClick={() =>
                                    onPick({
                                        productId: card.productId,
                                        title: card.title,
                                        artistName: card.artistName,
                                        coverImageUrl: card.coverImageUrl,
                                    })
                                }
                                className="flex w-full items-center gap-3 rounded-lg border border-line bg-surface px-3 py-2 text-left transition-colors hover:border-line-strong"
                            >
                                <VinylCover
                                    title={card.title}
                                    artist={card.artistName}
                                    imageUrl={card.coverImageUrl}
                                    className="w-10 shrink-0"
                                />
                                <span className="min-w-0 grow">
                                    <span className="block truncate text-[13.5px] font-semibold">{card.title}</span>
                                    <span className="block truncate text-[12px] text-muted">
                                        {card.artistName} · {card.releaseYear}
                                    </span>
                                </span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
