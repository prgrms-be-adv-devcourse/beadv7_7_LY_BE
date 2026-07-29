const FALLBACK_PALETTES: [string, string][] = [
    ["#4B6C8A", "#20313F"],
    ["#2C6E7F", "#123138"],
    ["#8A4B22", "#3A1D0C"],
    ["#3A2B52", "#171022"],
    ["#5A5A5A", "#232323"],
    ["#20486E", "#0C1E33"],
];

function paletteFor(title: string): [string, string] {
    let hash = 0;
    for (const ch of title) hash = (hash * 31 + ch.charCodeAt(0)) | 0;
    return FALLBACK_PALETTES[Math.abs(hash) % FALLBACK_PALETTES.length];
}

interface VinylCoverProps {
    title: string;
    artist: string;
    imageUrl?: string | null;
    colors?: [string, string];
    spin?: boolean;
    className?: string;
}

export function VinylCover({ title, artist, imageUrl, colors, spin = false, className = "" }: VinylCoverProps) {
    if (imageUrl) {
        return (
            <div className={`relative aspect-square overflow-hidden rounded-md ${className}`}>
                <img src={imageUrl} alt={`${title} 커버`} className="h-full w-full object-cover" />
            </div>
        );
    }
    const [from, to] = colors ?? paletteFor(title);
    return (
        <div
            className={`relative grid aspect-square place-items-center overflow-hidden rounded-md p-3 text-center ${className}`}
            style={{ background: `linear-gradient(150deg, ${from}, ${to})` }}
        >
            <div
                aria-hidden="true"
                className={`absolute -right-[20%] aspect-square w-[58%] rounded-full ${
                    spin ? "animate-[vinyl-spin_9s_linear_infinite] motion-reduce:animate-none" : ""
                }`}
                style={{
                    background:
                        "radial-gradient(circle, #1c1c1c 0 24%, #d8b46a 25% 27%, #1c1c1c 28% 30%, #2a2a2a 31% 100%)",
                }}
            />
            <div className="relative z-[1] text-white">
                <div className="font-display text-sm font-bold leading-tight [text-shadow:0_1px_4px_rgba(0,0,0,.4)]">
                    {title}
                </div>
                <div className="mt-0.5 text-[11px] opacity-85">{artist}</div>
            </div>
        </div>
    );
}
