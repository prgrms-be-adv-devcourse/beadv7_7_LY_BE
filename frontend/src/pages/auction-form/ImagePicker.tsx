import { useRef, useState } from "react";

// 이미지를 올려둘 서버가 없어서, 브라우저에서 줄인 사진을 문자열로 바꿔 경매 정보에 그대로 담는다.
// 사진 본문이 DB에 들어가는 방식이라 원본을 그대로 담으면 안 된다 — 아래 값으로 줄여서 담는다
const MAX_EDGE_PX = 720;
const JPEG_QUALITY = 0.7;
// 서버는 5장까지 받지만, 사진이 경매 상세 응답에 통째로 실려 나가서 3장으로 묶어둔다
export const MAX_ITEM_IMAGES = 3;
// 줄이기 전에 거르는 크기. 이보다 큰 파일은 브라우저가 여는 동안 화면이 멈춘다
const MAX_SOURCE_BYTES = 20 * 1024 * 1024;

async function shrinkToDataUrl(file: File): Promise<string> {
    // imageOrientation을 지정해야 세로로 찍은 사진이 눕지 않는다
    const bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
    const scale = Math.min(1, MAX_EDGE_PX / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d");
    if (context === null) {
        bitmap.close();
        throw new Error("브라우저가 이미지를 처리하지 못했습니다.");
    }
    context.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();
    return canvas.toDataURL("image/jpeg", JPEG_QUALITY);
}

// 저장될 문자열이 실제로 몇 바이트인지 (사람에게 보여줄 용도)
function approximateBytes(dataUrl: string): number {
    const base64 = dataUrl.slice(dataUrl.indexOf(",") + 1);
    return Math.round((base64.length * 3) / 4);
}

function formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

interface ImagePickerProps {
    images: string[];
    onChange: (images: string[]) => void;
}

export function ImagePicker({ images, onChange }: ImagePickerProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [report, setReport] = useState<string | null>(null);

    const remaining = MAX_ITEM_IMAGES - images.length;

    async function addFiles(files: FileList) {
        setBusy(true);
        setError(null);
        const added: string[] = [];
        let sourceBytes = 0;
        let resultBytes = 0;

        try {
            for (const file of Array.from(files).slice(0, remaining)) {
                if (!file.type.startsWith("image/")) {
                    setError(`${file.name}은(는) 이미지 파일이 아닙니다.`);
                    continue;
                }
                if (file.size > MAX_SOURCE_BYTES) {
                    setError(`${file.name}이(가) 너무 큽니다 (${formatBytes(file.size)}). ${formatBytes(MAX_SOURCE_BYTES)} 이하로 골라주세요.`);
                    continue;
                }
                const dataUrl = await shrinkToDataUrl(file);
                added.push(dataUrl);
                sourceBytes += file.size;
                resultBytes += approximateBytes(dataUrl);
            }
            if (added.length > 0) {
                onChange([...images, ...added]);
                setReport(`${formatBytes(sourceBytes)} → ${formatBytes(resultBytes)}로 줄여 담았습니다.`);
            }
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : "사진을 처리하지 못했습니다.");
        } finally {
            setBusy(false);
            // 같은 파일을 다시 고를 수 있게 입력값을 비운다
            if (inputRef.current) inputRef.current.value = "";
        }
    }

    function removeAt(index: number) {
        onChange(images.filter((_, i) => i !== index));
        setReport(null);
    }

    return (
        <div>
            <div className="flex flex-wrap gap-2.5">
                {images.map((image, index) => (
                    <div key={`${index}-${image.slice(0, 32)}`} className="relative">
                        <img
                            src={image}
                            alt={`매물 사진 ${index + 1}`}
                            className="h-24 w-24 rounded-lg border border-line object-cover"
                        />
                        <button
                            type="button"
                            onClick={() => removeAt(index)}
                            aria-label={`매물 사진 ${index + 1} 빼기`}
                            className="absolute -right-1.5 -top-1.5 grid h-6 w-6 place-items-center rounded-full border border-line bg-surface text-[13px] font-bold text-muted shadow-sm hover:border-live hover:text-live"
                        >
                            ×
                        </button>
                    </div>
                ))}
                {remaining > 0 && (
                    <button
                        type="button"
                        onClick={() => inputRef.current?.click()}
                        disabled={busy}
                        className="grid h-24 w-24 place-items-center rounded-lg border border-dashed border-line-strong bg-paper text-[12px] font-semibold text-muted hover:border-brand hover:text-brand disabled:opacity-50"
                    >
                        {busy ? "줄이는 중…" : `＋ 사진 추가`}
                    </button>
                )}
            </div>

            <input
                ref={inputRef}
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={(e) => {
                    if (e.target.files && e.target.files.length > 0) void addFiles(e.target.files);
                }}
            />

            <p className="mt-2 text-[12px] text-muted">
                최대 {MAX_ITEM_IMAGES}장 · 큰 사진은 자동으로 줄여서 올라갑니다.
                {images.length > 0 && ` (지금 ${images.length}장)`}
            </p>
            {report && <p className="mt-1 text-[12px] text-up">{report}</p>}
            {error && <p className="mt-1 text-[12px] font-semibold text-live">{error}</p>}
        </div>
    );
}
