"""Builds the application figures for the paper from screenshots captured in the browser pane.
Company names and e-mail addresses were replaced on the page before capture."""
from pathlib import Path
from PIL import Image

T = Path(r"C:\Users\adity\.claude\projects\D--esg-genius-pro\6582cfc9-bd72-468e-9d83-0b5c45f013c6\tool-results")
OUT = Path(__file__).parent / "shots"; OUT.mkdir(exist_ok=True)
g = lambda stamp: Image.open(next(T.glob(f"mcp-Claude_Browser-blob-{stamp}-*.jpg"))).convert("RGB")

def stack(parts, name, gap=8):
    crops = [g(s).crop((0, y0, 784, y1)) for s, y0, y1 in parts]
    h = sum(c.height for c in crops) + gap * (len(crops) - 1)
    img = Image.new("RGB", (784, h), (255, 255, 255)); y = 0
    for c in crops:
        img.paste(c, (0, y)); y += c.height + gap
    img.save(OUT / name, optimize=True); print(name, img.size)

stack([("1792859629689", 85, 778)], "documents.png")
stack([("1792859686147", 85, 690), ("1792859739264", 100, 560)], "results.png")
stack([("1792859752976", 85, 765), ("1792859759651", 255, 495), ("1792859756042", 405, 672)], "detail.png")
stack([("1792859706401", 80, 640), ("1792859710917", 85, 620)], "score.png")
stack([("1792859787546", 85, 540), ("1792859794748", 85, 470)], "report.png")
stack([("1792859977293", 85, 770)], "assistant.png")
